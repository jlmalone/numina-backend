package com.numina.common.utils

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import java.util.UUID

/**
 * Correlation ID for request tracking across services
 */
object CorrelationIdUtils {
    const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    const val REQUEST_ID_HEADER = "X-Request-ID"

    private val correlationIdKey = AttributeKey<String>("CorrelationId")
    private val requestIdKey = AttributeKey<String>("RequestId")

    fun ApplicationCall.getOrGenerateCorrelationId(): String {
        val existingId = request.headers[CORRELATION_ID_HEADER]
        return existingId ?: UUID.randomUUID().toString().also {
            attributes.put(correlationIdKey, it)
        }
    }

    fun ApplicationCall.getOrGenerateRequestId(): String {
        val existingId = request.headers[REQUEST_ID_HEADER]
        return existingId ?: UUID.randomUUID().toString().also {
            attributes.put(requestIdKey, it)
        }
    }

    fun ApplicationCall.getCorrelationId(): String? {
        return attributes.getOrNull(correlationIdKey)
    }

    fun ApplicationCall.getRequestId(): String? {
        return attributes.getOrNull(requestIdKey)
    }
}
