package com.numina.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

object AdminJwtConfig {
    private val secret = System.getenv("ADMIN_JWT_SECRET") ?: "admin-secret-change-in-production"
    private val issuer = "numina-backend-admin"
    private val audience = "numina-admin"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateToken(userId: Int, email: String, role: String): String {
        val now = Clock.System.now()
        val expiration = now.plus(8.hours) // Shorter expiration for admin tokens

        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("isAdmin", true)
            .withExpiresAt(java.util.Date(expiration.toEpochMilliseconds()))
            .sign(algorithm)
    }
}
