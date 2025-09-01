package com.abmo.api.routes

import com.abmo.api.model.Endpoint
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.root() {
    get(Endpoint.Root.path) {
        call.respondRedirect("/static/index.html")
    }
}