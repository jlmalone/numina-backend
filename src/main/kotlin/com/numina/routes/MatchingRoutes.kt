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
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.matchingRoutes() {
    val matchingService by inject<MatchingService>()

    authenticate("auth-jwt") {
        route("/matches") {
            // GET /api/v1/matches/partners - Get potential workout partners
            get("/partners") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                // Parse query parameters
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val minScore = call.request.queryParameters["minScore"]?.toIntOrNull() ?: 60
                val radius = call.request.queryParameters["radius"]?.toFloatOrNull() ?: 10.0f

                // Validate parameters
                if (limit < 1 || limit > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Limit must be between 1 and 100")
                    )
                    return@get
                }
                if (minScore < 0 || minScore > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "minScore must be between 0 and 100")
                    )
                    return@get
                }
                if (radius <= 0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "radius must be positive")
                    )
                    return@get
                }

                val partners = matchingService.getPartners(userId, limit, minScore, radius)
                call.respond(HttpStatusCode.OK, partners)
            }

            // GET /api/v1/matches/classes - Get recommended classes
            get("/classes") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                // Parse query parameters
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val minScore = call.request.queryParameters["minScore"]?.toIntOrNull() ?: 50
                val startDateStr = call.request.queryParameters["startDate"]
                val endDateStr = call.request.queryParameters["endDate"]

                // Validate limit
                if (limit < 1 || limit > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Limit must be between 1 and 100")
                    )
                    return@get
                }
                if (minScore < 0 || minScore > 100) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "minScore must be between 0 and 100")
                    )
                    return@get
                }

                // Parse dates
                val startDate = startDateStr?.let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid startDate format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00Z)")
                        )
                        return@get
                    }
                }
                val endDate = endDateStr?.let {
                    try {
                        Instant.parse(it)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid endDate format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00Z)")
                        )
                        return@get
                    }
                }

                val classes = matchingService.getRecommendedClasses(userId, limit, minScore, startDate, endDate)
                call.respond(HttpStatusCode.OK, classes)
            }

            // GET /api/v1/matches/mutual - Get mutual matches
            get("/mutual") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val mutualMatches = matchingService.getMutualMatches(userId)
                call.respond(HttpStatusCode.OK, mutualMatches)
            }

            // POST /api/v1/matches/action - Record a match action
            post("/action") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<MatchActionRequest>()

                // Validate that user is not matching with themselves
                if (request.targetUserId == userId) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Cannot perform match action on yourself")
                    )
                    return@post
                }

                val response = matchingService.recordMatchAction(
                    userId,
                    request.targetUserId,
                    request.action
                )

                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
