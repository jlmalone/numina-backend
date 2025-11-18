package com.numina.services

import com.numina.auth.JwtConfig
import com.numina.common.exceptions.ConflictException
import com.numina.common.exceptions.UnauthorizedException
import com.numina.common.exceptions.ValidationException
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.RefreshTokenRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.data.repositories.UserRepository
import com.numina.domain.*
import org.slf4j.LoggerFactory

interface AuthService {
    suspend fun register(request: RegisterRequest): LoginResponse
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun refreshToken(refreshToken: String): TokenResponse
    suspend fun logout(userId: Int)
}

class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) : AuthService {
    private val logger = LoggerFactory.getLogger(AuthServiceImpl::class.java)

    override suspend fun register(request: RegisterRequest): LoginResponse {
        logger.info("Attempting to register user with email: ${request.email}")

        // Validate input
        ValidationUtils.validateEmail(request.email)
        ValidationUtils.validatePassword(request.password)
        ValidationUtils.validateRequired(request.name, "name")

        // Create user
        val user = userRepository.createUser(request.email, request.password)
            ?: throw ConflictException(
                message = "User with email ${request.email} already exists",
                errorCode = "USER_ALREADY_EXISTS"
            )

        logger.info("User registered successfully: userId=${user.id}")

        // Create user profile
        userProfileRepository.createProfile(user.id, request.name)
        logger.debug("User profile created for userId=${user.id}")

        // Generate tokens
        val token = JwtConfig.generateToken(user.id, user.email)
        val refreshToken = refreshTokenRepository.createRefreshToken(user.id)

        logger.info("Tokens generated for userId=${user.id}")

        return LoginResponse(token, refreshToken, user)
    }

    override suspend fun login(request: LoginRequest): LoginResponse {
        logger.info("Login attempt for email: ${request.email}")

        // Validate input
        ValidationUtils.validateEmail(request.email)
        if (request.password.isBlank()) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("password" to "Password is required")
            )
        }

        // Verify credentials
        val user = userRepository.verifyPassword(request.email, request.password)
            ?: throw UnauthorizedException(
                message = "Invalid email or password",
                errorCode = "INVALID_CREDENTIALS"
            )

        logger.info("User authenticated successfully: userId=${user.id}")

        // Generate tokens
        val token = JwtConfig.generateToken(user.id, user.email)
        val refreshToken = refreshTokenRepository.createRefreshToken(user.id)

        logger.info("Tokens generated for userId=${user.id}")

        return LoginResponse(token, refreshToken, user)
    }

    override suspend fun refreshToken(refreshToken: String): TokenResponse {
        logger.debug("Token refresh attempt")

        if (refreshToken.isBlank()) {
            throw ValidationException(
                message = "Refresh token is required",
                errorCode = "MISSING_REFRESH_TOKEN"
            )
        }

        // Validate refresh token
        val userId = refreshTokenRepository.validateRefreshToken(refreshToken)
            ?: throw UnauthorizedException(
                message = "Invalid or expired refresh token",
                errorCode = "INVALID_REFRESH_TOKEN"
            )

        val user = userRepository.getUserById(userId)
            ?: throw UnauthorizedException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND"
            )

        // Revoke old token and create new one
        refreshTokenRepository.revokeRefreshToken(refreshToken)
        val newToken = JwtConfig.generateToken(user.id, user.email)
        val newRefreshToken = refreshTokenRepository.createRefreshToken(user.id)

        logger.info("Token refreshed for userId=${user.id}")

        return TokenResponse(newToken, newRefreshToken)
    }

    override suspend fun logout(userId: Int) {
        logger.info("Logout request for userId=$userId")
        refreshTokenRepository.revokeAllUserTokens(userId)
        logger.info("All tokens revoked for userId=$userId")
    }
}
