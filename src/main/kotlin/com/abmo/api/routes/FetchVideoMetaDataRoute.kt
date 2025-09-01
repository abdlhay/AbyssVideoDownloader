package com.abmo.api.routes

import com.abmo.api.model.Endpoint
import com.abmo.services.VideoDownloader
import com.abmo.util.toJson
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.fetchVideoMetadataRoute(videoDownloader: VideoDownloader) {
    post(Endpoint.FetchVideoMetadata.path) {
        val params = call.receiveParameters()
        val url = params["url"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing URL")
        val headers = params["headers"]?.split("\n")?.associate {
            val parts = it.split(":")
            val key = parts.first()
            val value = parts[1]
            key to value
        } ?: emptyMap()

        val result = videoDownloader.getVideoMetaData(url, headers, "")
        if (result != null) {
            call.respondText(result.toJson(), ContentType.Application.Json)
        } else {
            call.respond(HttpStatusCode.InternalServerError, "Failed to fetch metadata")
        }
    }
}