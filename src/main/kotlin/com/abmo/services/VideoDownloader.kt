package com.abmo.services

import com.abmo.common.Logger
import com.abmo.crypto.CryptoHelper
import com.abmo.model.Config
import com.abmo.model.SimpleVideo
import com.abmo.model.Datas
import com.abmo.model.video.Mp4
import com.abmo.model.video.Video
import com.abmo.model.video.toSimpleVideo
import com.abmo.util.displayProgressBar
import com.abmo.util.toObject
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
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class VideoDownloader: KoinComponent {

    private val cryptoHelper: CryptoHelper by inject()
    private val httpClientManager: HttpClientManager by inject()

    companion object {
        private const val FRAGMENT_SIZE_IN_BYTES = 2097152L
    }

    /**
     * Downloads video segments in parallel and merges them into a single MP4 file.
     * This function uses coroutines for concurrent downloading with a limit on the number of concurrent downloads.
     *
     * @param config The configuration containing settings like output file path and connection limits, resolution.
     * @param videoMetadata The metadata of the video, used to generate segment data and the decryption key.
     * @throws Exception If there are errors during the download or file operations.
     */
    suspend fun downloadSegmentsInParallel(config: Config, videoMetadata: Mp4?) {
        val simpleVideo = videoMetadata?.toSimpleVideo(config.resolution)
        val segmentTokens = generateSegmentTokens(simpleVideo)

        val tempDir = initializeDownloadTempDir(config, simpleVideo, segmentTokens.size)

        val segmentsToDownload = segmentTokens.filter { (index, _) -> index in tempDir.second }.ifEmpty {
            segmentTokens
        }

        // reference: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-semaphore/
        // used to limit the number of concurrent coroutines executing the download tasks.
        val semaphore = Semaphore(config.connections)
        val totalSegments = segmentsToDownload.size
        val mediaSize = segmentsToDownload.size * FRAGMENT_SIZE_IN_BYTES
        val downloadedSegments = AtomicInteger(0)
        val totalBytesDownloaded = AtomicLong(0L)

        val startTime = System.currentTimeMillis()

        coroutineScope {
            val downloadJobs = segmentsToDownload.entries.mapIndexed { _, segmentToken ->
                val segmentUrl = "${simpleVideo?.url}/sora/${simpleVideo?.size}/${segmentToken.value}"
                async(Dispatchers.IO) {
                    val index = segmentToken.key
                    semaphore.withPermit {
                        requestSegment(segmentUrl, segmentToken.value, index).collect { chunk ->
                            File(tempDir.first, "segment_$index").appendBytes(chunk)
                            totalBytesDownloaded.addAndGet(chunk.size.toLong())
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

    private fun mergeSegmentsIntoMp4File(segmentFolderPath: File, output: File) {
        val segmentFiles  = segmentFolderPath.listFiles { file -> file.name.startsWith("segment_") }
            ?.toList()
            ?.sortedBy { it.name.removePrefix("segment_").toIntOrNull() }
            ?: emptyList()
        segmentFiles.forEach {
            output.appendBytes(it.readBytes())
        }

        Logger.success("Segments merged successfully.")

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
    fun getVideoMetaData(url: String, headers: Map<String, String?>?): Mp4? {
        val response = httpClientManager.makeHttpRequest(url, headers)
        val encryptedData = response?.body ?: return null
        val videoData = parseEncryptedMp4MetadataFromHtml(encryptedData)
        return videoData

    }


    private fun parseEncryptedMp4MetadataFromHtml(html: String): Mp4? {
       val jsCode = Jsoup.parse(html)
           .select("script")
           .find { it.html().contains("datas") }
           ?.html()


        if (jsCode == null) {
            Logger.error("No encoded media metadata found in the provided HTML.")
            return null
        }
        val datasRegex = Regex("""const\s+datas\s*=\s*"([^"]*)"""")
        val datas = datasRegex.find(jsCode)?.groups?.get(1)?.value
        val decodedDatas = String(Base64.getDecoder().decode(datas), Charsets.ISO_8859_1)
        val mediaMetadata = decodedDatas.toObject<Datas>()
        val encryptedMediaMetadata = mediaMetadata.media

        if (encryptedMediaMetadata == null) {
            Logger.error("failed to get encrypted media")
            return null
        }

        val mediaKey = "${mediaMetadata.user_id}:${mediaMetadata.slug}:${mediaMetadata.md5_id}"
        val decryptionKey = cryptoHelper.getKey(mediaKey).toByteArray()
        val mediaSources = cryptoHelper.decryptString(encryptedMediaMetadata, decryptionKey)
            .toObject<Video>()
            .mp4?.copy(
                slug = mediaMetadata.slug,
                md5_id = mediaMetadata.md5_id
            )

        return mediaSources
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
                    file.length() < FRAGMENT_SIZE_IN_BYTES) {
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

    private fun generateRanges(size: Long, step: Long = FRAGMENT_SIZE_IN_BYTES): List<LongRange> {
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


    private fun generateSegmentTokens(simpleVideo: SimpleVideo?): Map<Int, String> {
        Logger.debug("Generating segment request tokens.")
        val fragmentList = mutableMapOf<Int, String>()
        // --- TAMBAHKAN LOG DI SINI ---
        Logger.debug("=== DEBUG ABYSS ===")
        Logger.debug("Raw Size Value: '${simpleVideo.size}'")
        Logger.debug("Data Type: ${simpleVideo.size?.javaClass?.name}")
        
        val encryptionKey = cryptoHelper.getKey(simpleVideo?.size)
        
        Logger.debug("encryptionKey: ${encryptionKey}")
        
        if (simpleVideo?.size != null) {
            val ranges = generateRanges(simpleVideo.size)
            ranges.forEachIndexed { index, _ ->
                val path = "/mp4/${simpleVideo.md5_id}/${simpleVideo.resId}/${simpleVideo.size}/$FRAGMENT_SIZE_IN_BYTES/$index"
                val encryptedBody = cryptoHelper.encryptAESCTR(path, encryptionKey)
                
                Logger.debug("encryptedBody: ${encryptedBody}")
                
                fragmentList[index] = doubleEncodeToBase64(encryptedBody)
                Logger.debug("fragmentList ${index}: ${fragmentList[index]}")
            }
            Logger.debug("${fragmentList.size} request token generated")
            return fragmentList
        }
        return emptyMap()
    }

    private fun doubleEncodeToBase64(input: String): String {
        val first = Base64.getEncoder()
            .encodeToString(input.toByteArray(Charsets.ISO_8859_1))
            .replace("=", "")

        return Base64.getEncoder()
            .encodeToString(first.toByteArray())
            .replace("=", "")
    }


    private suspend fun requestSegment(url: String, token: String, index: Int? = null): Flow<ByteArray> = flow {
        Logger.debug("[$index] Starting request to $url with token token: $token")
        val response = Unirest.get(url)
            .header("Referer", "https://abysscdn.com/")
            .asBinary()

        val rawBody = response.rawBody
        val responseCode = response.status

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
