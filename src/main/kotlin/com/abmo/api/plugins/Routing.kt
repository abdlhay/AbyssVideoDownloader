package com.abmo.api.plugins

import com.abmo.api.routes.downloadVideo
import com.abmo.api.routes.fetchVideoMetadataRoute
import com.abmo.api.routes.getCurrentDownloadProgress
import com.abmo.api.routes.root
import com.abmo.services.VideoDownloader
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


fun Application.configureRouting() {

    val videoDownloader: VideoDownloader by inject()

    routing {
        root()
        fetchVideoMetadataRoute(videoDownloader)
        getCurrentDownloadProgress()
        downloadVideo(videoDownloader)
    }

}