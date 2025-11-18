package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class User(
    val id: Int,
    val email: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val user: User
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class TokenResponse(
    val token: String,
    val refreshToken: String
)
