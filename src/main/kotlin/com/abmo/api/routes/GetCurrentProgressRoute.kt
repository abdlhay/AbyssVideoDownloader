package com.abmo.api.routes

import com.abmo.api.model.Endpoint
import com.abmo.services.ProgressManager
import com.abmo.util.toJson
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Route.getCurrentDownloadProgress() {
    webSocket(Endpoint.GetCurrentProgress.path) {
        ProgressManager.getFlow().collect { progress ->
            send(Frame.Text(progress.toJson()))
        }
    }
}