package com.numina.plugins

import com.numina.common.utils.CorrelationIdUtils
import com.numina.common.utils.CorrelationIdUtils.getOrGenerateCorrelationId
import com.numina.common.utils.CorrelationIdUtils.getOrGenerateRequestId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import org.slf4j.event.Level
import org.slf4j.LoggerFactory

val appMicrometerRegistry = PrometheusMeterRegistry(
    PrometheusConfig.DEFAULT,
    CollectorRegistry.defaultRegistry,
    Clock.SYSTEM
)

fun Application.configureMonitoring() {
    val logger = LoggerFactory.getLogger("Monitoring")

    // Install Call ID for request tracking
    install(CallId) {
        header(CorrelationIdUtils.CORRELATION_ID_HEADER)
        header(CorrelationIdUtils.REQUEST_ID_HEADER)
        generate { java.util.UUID.randomUUID().toString() }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }

    // Install Call Logging with correlation IDs and request duration
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
            val duration = call.attributes.getOrNull(requestDurationKey)

            buildString {
                append("[$httpMethod] $uri")
                append(" | Status: $status")
                duration?.let { append(" | Duration: ${it}ms") }
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

    // Request duration tracking and slow query logging
    intercept(ApplicationCallPipeline.Setup) {
        val startTime = System.currentTimeMillis()

        try {
            proceed()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            call.attributes.put(requestDurationKey, duration)

            // Log slow requests (> 1 second)
            if (duration > 1000) {
                logger.warn("SLOW REQUEST: ${call.request.httpMethod.value} ${call.request.uri} took ${duration}ms")
            }

            // Record metrics
            appMicrometerRegistry.timer(
                "http_requests_duration",
                "method", call.request.httpMethod.value,
                "uri", call.request.path(),
                "status", call.response.status()?.value?.toString() ?: "unknown"
            ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
    }

    // Add correlation ID to response headers
    intercept(ApplicationCallPipeline.Monitoring) {
        val correlationId = call.getOrGenerateCorrelationId()
        val requestId = call.getOrGenerateRequestId()

        call.response.headers.append(CorrelationIdUtils.CORRELATION_ID_HEADER, correlationId)
        call.response.headers.append(CorrelationIdUtils.REQUEST_ID_HEADER, requestId)
    }

    // Configure Prometheus metrics with JVM metrics
    ClassLoaderMetrics().bindTo(appMicrometerRegistry)
    JvmMemoryMetrics().bindTo(appMicrometerRegistry)
    JvmGcMetrics().bindTo(appMicrometerRegistry)
    JvmThreadMetrics().bindTo(appMicrometerRegistry)
    ProcessorMetrics().bindTo(appMicrometerRegistry)

    // Expose Prometheus metrics endpoint
    routing {
        get("/metrics") {
            call.respond(appMicrometerRegistry.scrape())
        }
    }

    logger.info("Monitoring and metrics configured")
}

private val requestDurationKey = AttributeKey<Long>("RequestDuration")
