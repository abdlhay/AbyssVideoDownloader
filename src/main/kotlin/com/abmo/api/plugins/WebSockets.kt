package com.abmo.api.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*


fun Application.configureWebsockets() {
    install(WebSockets)
}