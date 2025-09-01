package com.abmo.api

import com.abmo.api.plugins.configureRouting
import com.abmo.api.plugins.configureStaticFileServing
import com.abmo.api.plugins.configureWebsockets
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun startWebGuiServer() {
    embeddedServer(Netty, port = 8080) {
        configureStaticFileServing()
        configureWebsockets()
        configureRouting()
    }.start(wait = true)
}
