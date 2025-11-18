package com.numina

import com.numina.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Core infrastructure
    configureKoin()
    configureDatabase()

    // Monitoring and logging (early in pipeline)
    configureMonitoring()

    // Serialization
    configureSerialization()

    // Error handling (before routes)
    configureErrorHandling()

    // Security
    configureSecurity()

    // Health checks
    configureHealth()

    // Routes (last)
    configureRouting()
}
