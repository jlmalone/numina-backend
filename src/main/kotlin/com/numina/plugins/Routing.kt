package com.numina.plugins

import com.numina.routes.authRoutes
import com.numina.routes.classRoutes
import com.numina.routes.groupRoutes
import com.numina.routes.userRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Numina Backend API v1.0")
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }

        route("/api/v1") {
            authRoutes()
            userRoutes()
            classRoutes()
            groupRoutes()
        }
    }
}
