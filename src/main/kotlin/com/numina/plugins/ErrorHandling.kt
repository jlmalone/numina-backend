package com.numina.plugins

import com.numina.common.exceptions.ApiException
import com.numina.common.models.ApiResponse
import com.numina.common.utils.CorrelationIdUtils.getCorrelationId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandling")

    install(StatusPages) {
        // Handle custom API exceptions
        exception<ApiException> { call, cause ->
            val correlationId = call.getCorrelationId()
            logger.error(
                "API Exception [correlationId=$correlationId]: ${cause.message}",
                cause
            )

            val response = ApiResponse.error(
                message = cause.message,
                errorCode = cause.errorCode,
                details = cause.details?.mapValues { it.value.toString() }
            )

            call.respond(cause.statusCode, response)
        }

        // Handle serialization errors
        exception<SerializationException> { call, cause ->
            val correlationId = call.getCorrelationId()
            logger.error(
                "Serialization error [correlationId=$correlationId]: ${cause.message}",
                cause
            )

            val response = ApiResponse.error(
                message = "Invalid request format",
                errorCode = "INVALID_FORMAT",
                details = mapOf("detail" to (cause.message ?: "Unknown serialization error"))
            )

            call.respond(HttpStatusCode.BadRequest, response)
        }

        // Handle illegal argument exceptions
        exception<IllegalArgumentException> { call, cause ->
            val correlationId = call.getCorrelationId()
            logger.error(
                "Illegal argument [correlationId=$correlationId]: ${cause.message}",
                cause
            )

            val response = ApiResponse.error(
                message = cause.message ?: "Invalid argument",
                errorCode = "INVALID_ARGUMENT"
            )

            call.respond(HttpStatusCode.BadRequest, response)
        }

        // Handle all other exceptions
        exception<Throwable> { call, cause ->
            val correlationId = call.getCorrelationId()
            logger.error(
                "Unhandled exception [correlationId=$correlationId]: ${cause.message}",
                cause
            )

            // Don't expose internal error details in production
            val isDevelopment = environment.developmentMode
            val errorMessage = if (isDevelopment) {
                cause.message ?: "Unknown error"
            } else {
                "An unexpected error occurred"
            }

            val details = if (isDevelopment) {
                mapOf(
                    "type" to cause::class.simpleName.orEmpty(),
                    "stackTrace" to cause.stackTraceToString().take(500)
                )
            } else {
                mapOf("correlationId" to (correlationId ?: "unknown"))
            }

            val response = ApiResponse.error(
                message = errorMessage,
                errorCode = "INTERNAL_ERROR",
                details = details
            )

            call.respond(HttpStatusCode.InternalServerError, response)
        }

        // Handle 404 Not Found
        status(HttpStatusCode.NotFound) { call, status ->
            val correlationId = call.getCorrelationId()
            logger.warn("404 Not Found [correlationId=$correlationId]: ${call.request.local.uri}")

            val response = ApiResponse.error(
                message = "The requested resource was not found",
                errorCode = "RESOURCE_NOT_FOUND",
                details = mapOf("path" to call.request.local.uri)
            )

            call.respond(status, response)
        }

        // Handle 405 Method Not Allowed
        status(HttpStatusCode.MethodNotAllowed) { call, status ->
            val correlationId = call.getCorrelationId()
            logger.warn(
                "405 Method Not Allowed [correlationId=$correlationId]: ${call.request.httpMethod.value} ${call.request.local.uri}"
            )

            val response = ApiResponse.error(
                message = "HTTP method not allowed for this endpoint",
                errorCode = "METHOD_NOT_ALLOWED",
                details = mapOf(
                    "method" to call.request.httpMethod.value,
                    "path" to call.request.local.uri
                )
            )

            call.respond(status, response)
        }
    }
}
