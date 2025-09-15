package com.abmo.services

import com.abmo.api.model.DownloadProgress
import com.abmo.common.Constants.abyssDefaultHeaders
import com.abmo.common.Logger
import com.abmo.crypto.CryptoHelper
import com.abmo.executor.JavaScriptExecutor
import com.abmo.model.*
import com.abmo.model.dto.VideoDto
import com.abmo.model.dto.toVideo
import com.abmo.util.*
import com.mashape.unirest.http.Unirest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class VideoDownloader: KoinComponent {

    private val cryptoHelper: CryptoHelper by inject()
    private val javaScriptExecutor: JavaScriptExecutor by inject()
    private val abyssCodeExtractor: AbyssJsCodeExtractor by inject()
    private val httpClientManager: HttpClientManager by inject()

    /**
     * Downloads video segments in parallel, decrypts the header of each segment, and merges them into a single MP4 file.
     * This function uses coroutines for concurrent downloading with a limit on the number of concurrent downloads.
     * The header of each segment is decrypted only once (on the first chunk) using `isHeader` to distinguish it.
     *
     * @param config The configuration containing settings like output file path and connection limits, resolution.
     * @param videoMetadata The metadata of the video, used to generate segment data and the decryption key.
     * @throws Exception If there are errors during the download or file operations.
     */
    suspend fun downloadSegmentsInParallel(config: Config, videoMetadata: Video?) {
        val simpleVideo = videoMetadata?.toSimpleVideo(config.resolution)
        val segmentBodies = generateSegmentsBody(simpleVideo)
        val segmentUrl = getSegmentUrl(videoMetadata, config.resolution)
        val decryptionKey = cryptoHelper.getKey(simpleVideo?.size)


        val tempDir = initializeDownloadTempDir(config, simpleVideo, segmentBodies.size)

        val segmentsToDownload = segmentBodies.filter { (index, _) -> index in tempDir.second }.ifEmpty {
            segmentBodies
        }

        // reference: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-semaphore/
        // used to limit the number of concurrent coroutines executing the download tasks.
        val semaphore = Semaphore(config.connections)
        val totalSegments = segmentsToDownload.size
        val mediaSize = segmentsToDownload.size * 2097152L
        val downloadedSegments = AtomicInteger(0)
        val totalBytesDownloaded = AtomicLong(0L)

        val startTime = System.currentTimeMillis()

        coroutineScope {
            val downloadJobs = segmentsToDownload.entries.mapIndexed { _, segmentBody ->
                async(Dispatchers.IO) {
                    val index = segmentBody.key
                    semaphore.withPermit {
                        var isHeader = true
                        requestSegment(segmentUrl, segmentBody.value.toJson(), index).collect { chunk ->
                            val array = if (isHeader) {
                                isHeader = false
                                cryptoHelper.decryptAESCTR(chunk, decryptionKey)
                            } else {
                                chunk
                            }
                            File(tempDir.first, "segment_$index").appendBytes(array)
                            totalBytesDownloaded.addAndGet(array.size.toLong())
                        }
                    }
                    downloadedSegments.incrementAndGet()

                }
            }

            val progressJob = launch {
                var lastUpdateTime = System.currentTimeMillis()
                while (isActive) {
                    lastUpdateTime = displayProgressBar(
                        mediaSize,
                        totalSegments,
                        totalBytesDownloaded.toLong(),
                        downloadedSegments.get(),
                        startTime,
                        lastUpdateTime
                    )

                    ProgressManager.emitProgress(DownloadProgress(
                        downloadedSegments = downloadedSegments.get(),
                        totalSegments = totalSegments,
                        downloadedBytes = totalBytesDownloaded.get(),
                        mediaSize = mediaSize,
                        percent = (downloadedSegments.get().toDouble() / totalSegments * 100.0),
                        startTime = startTime
                    ))

                    delay(1000)
                }
            }
            downloadJobs.awaitAll()
            progressJob.cancel()
        }
        println("\n")
        Logger.debug("All segments have been downloaded successfully!")
        Logger.info("merging segments into mp4 file...")
        config.outputFile?.let { mergeSegmentsIntoMp4File(tempDir.first, it) }

    }


    @Deprecated("Use downloadSegmentsInParallel instead.",
        ReplaceWith("downloadSegmentsInParallel(config, videoMetadata)"))
    suspend fun downloadVideo(config: Config, videoMetadata: Video?) {
        var totalBytesDownloaded = 0L
        val startTime = System.currentTimeMillis()
        val simpleVideo = videoMetadata?.toSimpleVideo(config.resolution)
        val segmentBodies = generateSegmentsBody(simpleVideo)
        val segmentUrl = getSegmentUrl(videoMetadata, config.resolution)
        val decryptionKey = cryptoHelper.getKey(simpleVideo?.size)

        for (body in segmentBodies) {
            var isFirst = true
            requestSegment(segmentUrl, body.toJson()).collect { chunk ->
                val array = if (isFirst) {
                    isFirst = false
                    cryptoHelper.decryptAESCTR(chunk, decryptionKey)
                } else {
                    chunk
                }
                config.outputFile?.appendBytes(array)
                totalBytesDownloaded += array.size
                displayProgressBar(totalBytesDownloaded, simpleVideo?.size!!.toLong(), startTime)
            }
        }
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime).toReadableTime()
        println("\nDownload took: $duration")
        println("\nDownload complete")
    }



    private suspend fun mergeSegmentsIntoMp4File(segmentFolderPath: File, output: File) {
        val segmentFiles  = segmentFolderPath.listFiles { file -> file.name.startsWith("segment_") }
            ?.toList()
            ?.sortedBy { it.name.removePrefix("segment_").toIntOrNull() }
            ?: emptyList()
        segmentFiles.forEach {
            output.appendBytes(it.readBytes())
        }

        Logger.success("Segments merged successfully.")

        ProgressManager.emitProgress(DownloadProgress(
            percent = 100.0,
            status = "completed",
            message = "Download completed successfully!"
        ))

        if (segmentFolderPath.exists() && segmentFolderPath.isDirectory) {
            val files = segmentFolderPath.listFiles()

            if (files != null) {
                for (file in files) {
                    file.delete()
                }
            }

            if (!segmentFolderPath.delete()) {
                Logger.error("Failed to delete folder: ${segmentFolderPath.absolutePath}")
//                Logger.info("Deleted temporary folder at: ${segmentFolderPath.absolutePath}")
            }
        } else {
            Logger.error("Folder does not exist or is not a directory: ${segmentFolderPath.absolutePath}")
        }
    }


    /**
     * Sends an HTTP GET request to retrieve and decode video metadata.
     *
     * @param url The URL to send the GET request to.
     * @param headers A map of headers to include in the request.
     * @return The decoded video metadata, or null if the extraction or decoding fails.
     */
    fun getVideoMetaData(url: String, headers: Map<String, String?>?, curlPath: String): Video? {
        val response = httpClientManager.makeHttpRequest(url, headers, curlPath)
        val encryptedData = response?.body ?: return null
        val videoData = extractEncryptedVideoMetaData(encryptedData)?.toObject<VideoDto>()
        val decodedSources = cryptoHelper.decodeEncryptedString(videoData?.sourcesEncrypted)?.sources?.distinct()
            ?.sortedBy { it?.size }
        return videoData?.toVideo(decodedSources)

    }


    private fun extractEncryptedVideoMetaData(html: String): String? {
       val jsCode = Jsoup.parse(html)
           .select("script")
           .find { it.html().contains("atob") }
           ?.html()

        if (jsCode == null) {
            Logger.debug("No encrypted video metadata found in the provided HTML.", true)
            return null
        }

        val javascriptCodeToExecute = abyssCodeExtractor.getCompleteJsCode(jsCode)


        val result =  javaScriptExecutor.runJavaScriptCode(javascriptCode = javascriptCodeToExecute)
        return result
    }

    private fun getSegmentUrl(video: Video?, res: String): String {
        val simpleVideo = video?.toSimpleVideo(res)
        // don't know what is this 'isBal' boolean parameter for
        val url = "https://${video?.domain}/tunnel/list?slug=${simpleVideo?.slug}" +
                "&size=${simpleVideo?.size}&label=${simpleVideo?.label}&md5_id=${simpleVideo?.md5_id}&isBal=1"

        val response = Unirest.get(url)
            .headers(abyssDefaultHeaders)
            .asString().body

        val urlList =  response.toObject<List<String>>().map { it + "/${video?.id}" }

        return urlList.getOrNull(2) ?: urlList[0]
//        return "https://${video?.domain}/${video?.id}"
    }


    private fun initializeDownloadTempDir(
        config: Config,
        simpleVideo: SimpleVideo?,
        totalSegments: Int
    ): Pair<File, List<Int>> {
        val tempFolderName = "temp_${simpleVideo?.slug}_${simpleVideo?.label}"
        // no need to check if path exists before creating temp folder we already did that in Main.kt
        val tempFolder = File(config.outputFile?.parentFile, tempFolderName)

        if (tempFolder.exists() && tempFolder.isDirectory) {
            Logger.info("Resuming download from temporary folder: $tempFolderName. Continuing from previously downloaded segments.")
            println("\n")
            val existingSegments = tempFolder.listFiles { file ->
                if (
                    file.isFile &&
                    file.name.matches(Regex("segment_\\d+")) &&
                    file.length() < 2097152) {
                    file.delete()
                }
                file.isFile && file.name.matches(Regex("segment_\\d+"))
            }?.mapNotNull { file ->
                file.name.removePrefix("segment_").toIntOrNull()
            }?.toSet() ?: emptySet()

            val allSegmentNames = (0 until totalSegments).toList()

            val missingSegmentNames = allSegmentNames.filterNot { it in existingSegments }

            return tempFolder to missingSegmentNames
        } else {
            Logger.info("Creating temporary folder $tempFolderName")
            println("\n")
            tempFolder.mkdirs()
        }
        return tempFolder to emptyList()
    }

    private fun generateRanges(size: Long, step: Long = 2097152): List<LongRange> {
        val ranges = mutableListOf<LongRange>()

        // if the size is less than or equal to step size return a single range
        if (size <= step) {
            ranges.add(0 until size)
            return ranges
        }

        var start = 0L
        while (start < size) {
            val end = minOf(start + step, size) // ensure the end doesn't exceed the size
            ranges.add(start until end)
            start = end
        }

        return ranges
    }


    private fun generateSegmentsBody(simpleVideo: SimpleVideo?): Map<Int, String> {
        Logger.debug("Generating segment POST request body and encrypting the data.")
        val fragmentList = mutableMapOf<Int, String>()
        val encryptionKey = cryptoHelper.getKey(simpleVideo?.slug)
        if (simpleVideo?.size != null) {
            val ranges = generateRanges(simpleVideo.size)
            ranges.forEachIndexed { index, range ->
                val body = simpleVideo.copy(
                    range = Range(range.first, range.last)
                )
                val encryptedBody = cryptoHelper.encryptAESCTR(body.toJson(), encryptionKey)
                fragmentList[index] = encryptedBody
            }
            Logger.debug("${fragmentList.size} request body generated")
            return fragmentList
        }
        return emptyMap()
    }


    private suspend fun requestSegment(url: String, body: String, index: Int? = null): Flow<ByteArray> = flow {
//        println("\n")
        Logger.debug("[$index] Starting HTTP POST request to $url with body length: ${body.length}. Body (truncated): \"...${body.takeLast(30)}")
        val response = Unirest.post(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            .header("Content-Type", "application/json")
            .headers(abyssDefaultHeaders)
            .body("""{"hash":$body}""")
            .asBinary()

        val rawBody = response.rawBody
        val responseCode = response.status

//        println("\n")
        Logger.debug("[$index] Received response with status $responseCode\n", responseCode !in 200..299)

        val buffer = ByteArray(65536)
        var bytesRead: Int

        rawBody.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                emit(buffer.copyOf(bytesRead))
            }
        }
    }.flowOn(Dispatchers.IO)


}