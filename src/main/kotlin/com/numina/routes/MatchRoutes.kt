package com.numina.routes

import com.numina.domain.MatchActionRequest
import com.numina.services.MatchingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.days

fun Route.matchRoutes() {
    val matchingService by inject<MatchingService>()

    authenticate("auth-jwt") {
        route("/matches") {
            /**
             * GET /api/v1/matches/partners
             * Get potential workout partners for current user
             */
            get("/partners") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                // Parse query parameters
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val minScore = call.request.queryParameters["minScore"]?.toIntOrNull() ?: 60
                val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0

                val matches = matchingService.getPartnerMatches(
                    userId = userId,
                    limit = limit.coerceIn(1, 100),
                    minScore = minScore.coerceIn(0, 100),
                    radiusKm = radius.coerceIn(0.1, 100.0)
                )

                call.respond(HttpStatusCode.OK, matches)
            }

            /**
             * GET /api/v1/matches/classes
             * Get recommended classes for current user
             */
            get("/classes") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                // Parse query parameters
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val minScore = call.request.queryParameters["minScore"]?.toIntOrNull() ?: 50
                val startDate = call.request.queryParameters["startDate"]?.let { Instant.parse(it) }
                    ?: Clock.System.now()
                val endDate = call.request.queryParameters["endDate"]?.let { Instant.parse(it) }
                    ?: Clock.System.now().plus(7.days)

                val matches = matchingService.getClassMatches(
                    userId = userId,
                    limit = limit.coerceIn(1, 100),
                    minScore = minScore.coerceIn(0, 100),
                    startDate = startDate,
                    endDate = endDate
                )

                call.respond(HttpStatusCode.OK, matches)
            }

            /**
             * GET /api/v1/matches/mutual
             * Get mutual matches (both users matched each other)
             */
            get("/mutual") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val mutualMatches = matchingService.getMutualMatches(userId)

                call.respond(HttpStatusCode.OK, mutualMatches)
            }

            /**
             * POST /api/v1/matches/action
             * Record a match action (like/pass/super_like)
             */
            post("/action") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<MatchActionRequest>()

                val response = matchingService.recordMatchAction(userId, request)

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
