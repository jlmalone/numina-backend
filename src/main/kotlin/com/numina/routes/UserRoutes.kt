package com.numina.routes

import com.numina.domain.UpdateProfileRequest
import com.numina.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val userService by inject<UserService>()

    authenticate("auth-jwt") {
        route("/users") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val profile = userService.getProfile(userId)
                call.respond(HttpStatusCode.OK, profile)
            }

            put("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<UpdateProfileRequest>()

                val updatedProfile = userService.updateProfile(userId, request)
                call.respond(HttpStatusCode.OK, updatedProfile)
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val requesterId = principal!!.payload.getClaim("userId").asInt()
                val userId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                val publicProfile = userService.getPublicProfile(userId, requesterId)
                call.respond(HttpStatusCode.OK, publicProfile)
            }
        }
    }
}
