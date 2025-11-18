package com.numina.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long,
    val version: String,
    val checks: Map<String, HealthCheck>
)

@Serializable
data class HealthCheck(
    val status: String,
    val message: String? = null,
    val responseTime: Long? = null
)

fun Application.configureHealth() {
    val logger = LoggerFactory.getLogger("Health")

    routing {
        get("/health") {
            val checks = mutableMapOf<String, HealthCheck>()
            var overallStatus = "healthy"

            // Database health check
            val dbCheck = checkDatabase()
            checks["database"] = dbCheck
            if (dbCheck.status != "healthy") {
                overallStatus = "unhealthy"
            }

            // Memory health check
            val memoryCheck = checkMemory()
            checks["memory"] = memoryCheck
            if (memoryCheck.status != "warning" && memoryCheck.status != "healthy") {
                overallStatus = "unhealthy"
            }

            val response = HealthResponse(
                status = overallStatus,
                timestamp = System.currentTimeMillis(),
                version = environment.config.property("ktor.deployment.version").getString(),
                checks = checks
            )

            val statusCode = if (overallStatus == "healthy") {
                HttpStatusCode.OK
            } else {
                HttpStatusCode.ServiceUnavailable
            }

            call.respond(statusCode, response)
        }

        get("/health/ready") {
            // Readiness probe - is the app ready to serve traffic?
            val dbHealthy = checkDatabase().status == "healthy"

            if (dbHealthy) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
            } else {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("status" to "not ready", "reason" to "database unavailable")
                )
            }
        }

        get("/health/live") {
            // Liveness probe - is the app running?
            call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
        }
    }
}

private fun checkDatabase(): HealthCheck {
    return try {
        val startTime = System.currentTimeMillis()

        transaction {
            exec("SELECT 1") { }
        }

        val responseTime = System.currentTimeMillis() - startTime

        HealthCheck(
            status = "healthy",
            message = "Database connection successful",
            responseTime = responseTime
        )
    } catch (e: Exception) {
        HealthCheck(
            status = "unhealthy",
            message = "Database connection failed: ${e.message}"
        )
    }
}

private fun checkMemory(): HealthCheck {
    val runtime = Runtime.getRuntime()
    val maxMemory = runtime.maxMemory()
    val allocatedMemory = runtime.totalMemory()
    val freeMemory = runtime.freeMemory()
    val usedMemory = allocatedMemory - freeMemory
    val percentageUsed = (usedMemory.toDouble() / maxMemory.toDouble()) * 100

    return when {
        percentageUsed > 90 -> HealthCheck(
            status = "unhealthy",
            message = "Memory usage critical: ${percentageUsed.toInt()}%"
        )
        percentageUsed > 75 -> HealthCheck(
            status = "warning",
            message = "Memory usage high: ${percentageUsed.toInt()}%"
        )
        else -> HealthCheck(
            status = "healthy",
            message = "Memory usage normal: ${percentageUsed.toInt()}%"
        )
    }
}
