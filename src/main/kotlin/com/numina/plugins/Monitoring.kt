package com.numina.plugins

import com.numina.common.utils.CorrelationIdUtils
import com.numina.common.utils.CorrelationIdUtils.getOrGenerateCorrelationId
import com.numina.common.utils.CorrelationIdUtils.getOrGenerateRequestId
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    // Install Call ID for request tracking
    install(CallId) {
        header(CorrelationIdUtils.CORRELATION_ID_HEADER)
        header(CorrelationIdUtils.REQUEST_ID_HEADER)
        generate { java.util.UUID.randomUUID().toString() }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }

    // Install Call Logging with correlation IDs
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }

        format { call ->
            val correlationId = call.getOrGenerateCorrelationId()
            val requestId = call.getOrGenerateRequestId()
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val uri = call.request.uri

            buildString {
                append("[$httpMethod] $uri")
                append(" | Status: $status")
                append(" | CorrelationId: $correlationId")
                append(" | RequestId: $requestId")
                userAgent?.let { append(" | UserAgent: $it") }
            }
        }

        // Add correlation ID to MDC for structured logging
        mdc(CorrelationIdUtils.CORRELATION_ID_HEADER) { call ->
            call.getOrGenerateCorrelationId()
        }
        mdc(CorrelationIdUtils.REQUEST_ID_HEADER) { call ->
            call.getOrGenerateRequestId()
        }
    }

    // Add correlation ID to response headers
    intercept(ApplicationCallPipeline.Monitoring) {
        val correlationId = call.getOrGenerateCorrelationId()
        val requestId = call.getOrGenerateRequestId()

        call.response.headers.append(CorrelationIdUtils.CORRELATION_ID_HEADER, correlationId)
        call.response.headers.append(CorrelationIdUtils.REQUEST_ID_HEADER, requestId)
    }
}
