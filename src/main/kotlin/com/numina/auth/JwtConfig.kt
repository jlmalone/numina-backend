package com.numina.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

object JwtConfig {
    private val secret = System.getenv("JWT_SECRET") ?: "default-secret-change-in-production"
    private val issuer = "numina-backend"
    private val audience = "numina-users"
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateToken(userId: Int, email: String): String {
        val now = Clock.System.now()
        val expiration = now.plus(24.hours)

        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(java.util.Date(expiration.toEpochMilliseconds()))
            .sign(algorithm)
    }
}
