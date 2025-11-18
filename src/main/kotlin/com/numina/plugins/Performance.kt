package com.numina.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configure performance optimizations including compression, caching headers, and rate limiting
 */
fun Application.configurePerformance() {
    // Response Compression
    install(Compression) {
        gzip {
            priority = 1.0
            minimumSize(1024) // Only compress responses larger than 1KB
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }

    // Caching Headers
    install(CachingHeaders) {
        options { call, content ->
            val path = call.request.local.uri
            when {
                // Cache class listings for 15 minutes
                path.contains("/api/v1/classes") && !path.contains("/book") ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 900))

                // Cache user profiles for 30 minutes
                path.contains("/api/v1/users/profile") && call.request.local.method == HttpMethod.Get ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 1800))

                // Cache reviews for 1 hour
                path.contains("/api/v1/reviews") && call.request.local.method == HttpMethod.Get ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))

                // No caching for mutations
                call.request.local.method != HttpMethod.Get ->
                    CachingOptions(CacheControl.NoCache(null))

                else -> null
            }
        }
    }

    // Rate Limiting
    install(RateLimit) {
        // Auth endpoints - 5 requests/min per IP
        register(RateLimitName("auth")) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.headers["X-Forwarded-For"]
                    ?: call.request.local.remoteHost
            }
        }

        // Messaging - 20 messages/min per user
        register(RateLimitName("messaging")) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.headers["X-User-Id"]
                    ?: call.request.local.remoteHost
            }
        }

        // Social activity - 10 posts/min per user
        register(RateLimitName("social")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.headers["X-User-Id"]
                    ?: call.request.local.remoteHost
            }
        }

        // Reviews - 5 reviews/hour per user
        register(RateLimitName("reviews")) {
            rateLimiter(limit = 5, refillPeriod = 1.hours)
            requestKey { call ->
                call.request.headers["X-User-Id"]
                    ?: call.request.local.remoteHost
            }
        }

        // Default - 100 requests/min per user
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
            requestKey { call ->
                call.request.headers["X-User-Id"]
                    ?: call.request.local.remoteHost
            }
        }
    }
}
