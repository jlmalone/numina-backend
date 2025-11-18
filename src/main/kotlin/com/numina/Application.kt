package com.numina

import com.numina.plugins.*
import com.numina.services.FCMService
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.ktor.ext.inject

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Core infrastructure
    configureKoin()
    configureDatabase()

    // Initialize Firebase Cloud Messaging
    val fcmService by inject<FCMService>()
    val firebaseServiceAccountPath = environment.config.propertyOrNull("firebase.serviceAccountPath")?.getString()
    fcmService.initialize(firebaseServiceAccountPath)

    // Monitoring and logging (early in pipeline)
    configureMonitoring()

    // Serialization
    configureSerialization()

    // Error handling (before routes)
    configureErrorHandling()

    // Security
    configureSecurity()

    // WebSockets for real-time messaging
    configureWebSockets()

    // Health checks
    configureHealth()

    // Routes (last)
    configureRouting()
}
