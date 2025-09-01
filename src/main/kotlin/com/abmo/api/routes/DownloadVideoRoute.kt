package com.abmo.api.routes

import com.abmo.api.model.DownloadRequest
import com.abmo.api.model.Endpoint
import com.abmo.model.Config
import com.abmo.services.ProgressManager
import com.abmo.services.VideoDownloader
import com.abmo.util.getDownloadsFolder
import com.abmo.util.sanitizeFileName
import com.abmo.util.toJson
import com.abmo.util.toObject
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


fun Route.downloadVideo(videoDownloader: VideoDownloader) {
    post(Endpoint.DownloadVideo.path) {
        val request = call.receive<String>().toObject<DownloadRequest>()


        val currentTimeStamp = System.currentTimeMillis()
        val defaultFileName = "${request.videoMetadata.slug}_${request.config.resolution}_$currentTimeStamp.mp4"
        val fileName = request.config.outputFile?.sanitizeFileName() ?: defaultFileName
        val config = request.config.let {
            Config(
                url = it.url,
                resolution = it.resolution,
                outputFile = File(getDownloadsFolder(), fileName),
                header = it.header,
                connections = it.connections
            )
        }


        // Emit initial progress immediately
        ProgressManager.emitProgress(mapOf(
            "status" to "starting",
            "message" to "Initializing download...",
            "downloadedSegments" to 0,
            "totalSegments" to 0,
            "downloadedBytes" to 0,
            "mediaSize" to 0,
            "percent" to 0.0
        ))

        // Launch download asynchronously DON'T WAIT FOR IT
        CoroutineScope(Dispatchers.IO).launch {
            try {
                videoDownloader.downloadSegmentsInParallel(
                    config = config,
                    videoMetadata = request.videoMetadata
                )
            } catch (e: Exception) {
                // Handle download errors through ProgressManager
                ProgressManager.emitProgress(mapOf(
                    "status" to "error",
                    "error" to e.message!!,
                    "message" to "Download failed: ${e.message}"
                ))
            }
        }

        call.respond(mapOf("message" to "Download started").toJson())
    }
}