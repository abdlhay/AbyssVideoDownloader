package com.abmo.api.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*


fun Application.configureStaticFileServing() {
    routing { staticResources("/static", "static") }
}