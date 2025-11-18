package com.numina.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.time.Duration

/**
 * Configure WebSocket support for real-time messaging
 */
fun Application.configureWebSockets() {
    install(WebSockets) {
        // Ping period - send ping frame every 15 seconds to keep connection alive
        pingPeriod = Duration.ofSeconds(15)

        // Timeout for ping/pong frames - close connection if no pong received within 20 seconds
        timeout = Duration.ofSeconds(20)

        // Maximum frame size in bytes (1MB)
        maxFrameSize = Long.MAX_VALUE

        // Enable masking of outgoing frames (recommended for security)
        masking = false

        // Configure content converter for JSON serialization (optional)
        // contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}
