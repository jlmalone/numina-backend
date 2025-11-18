package com.numina.routes

import com.numina.auth.JwtConfig
import com.numina.data.repositories.RefreshTokenRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.data.repositories.UserRepository
import com.numina.domain.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.authRoutes() {
    val userRepository by inject<UserRepository>()
    val userProfileRepository by inject<UserProfileRepository>()
    val refreshTokenRepository by inject<RefreshTokenRepository>()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            // Validate input
            if (request.email.isBlank() || request.password.isBlank() || request.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email, password, and name are required"))
                return@post
            }

            if (request.password.length < 8) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password must be at least 8 characters"))
                return@post
            }

            // Create user
            val user = userRepository.createUser(request.email, request.password)
            if (user == null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "User with this email already exists"))
                return@post
            }

            // Create user profile
            userProfileRepository.createProfile(user.id, request.name)

            // Generate tokens
            val token = JwtConfig.generateToken(user.id, user.email)
            val refreshToken = refreshTokenRepository.createRefreshToken(user.id)

            call.respond(HttpStatusCode.Created, LoginResponse(token, refreshToken, user))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = userRepository.verifyPassword(request.email, request.password)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }

            val token = JwtConfig.generateToken(user.id, user.email)
            val refreshToken = refreshTokenRepository.createRefreshToken(user.id)

            call.respond(LoginResponse(token, refreshToken, user))
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()

            val userId = refreshTokenRepository.validateRefreshToken(request.refreshToken)
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired refresh token"))
                return@post
            }

            val user = userRepository.getUserById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                return@post
            }

            // Revoke old token and create new one
            refreshTokenRepository.revokeRefreshToken(request.refreshToken)
            val newToken = JwtConfig.generateToken(user.id, user.email)
            val newRefreshToken = refreshTokenRepository.createRefreshToken(user.id)

            call.respond(TokenResponse(newToken, newRefreshToken))
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                refreshTokenRepository.revokeAllUserTokens(userId)

                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            }
        }
    }
}
