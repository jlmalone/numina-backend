package com.numina.routes

import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.UpdateProfileRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val userProfileRepository by inject<UserProfileRepository>()

    authenticate("auth-jwt") {
        route("/users") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val profile = userProfileRepository.getProfile(userId)
                if (profile == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profile not found"))
                    return@get
                }

                call.respond(profile)
            }

            put("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<UpdateProfileRequest>()

                // Validate fitness level if provided
                if (request.fitnessLevel != null && (request.fitnessLevel < 1 || request.fitnessLevel > 10)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fitness level must be between 1 and 10"))
                    return@put
                }

                val updatedProfile = userProfileRepository.updateProfile(userId, request)
                if (updatedProfile == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profile not found"))
                    return@put
                }

                call.respond(updatedProfile)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val requesterId = principal!!.payload.getClaim("userId").asInt()
                val userId = call.parameters["id"]?.toIntOrNull()

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                    return@get
                }

                val publicProfile = userProfileRepository.getPublicProfile(userId, requesterId)
                if (publicProfile == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Profile not found"))
                    return@get
                }

                call.respond(publicProfile)
            }
        }
    }
}
