package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.PublicProfile
import com.numina.domain.UpdateProfileRequest
import com.numina.domain.UserProfile
import org.slf4j.LoggerFactory

interface UserService {
    suspend fun getProfile(userId: Int): UserProfile
    suspend fun getPublicProfile(userId: Int, requesterId: Int): PublicProfile
    suspend fun updateProfile(userId: Int, request: UpdateProfileRequest): UserProfile
}

class UserServiceImpl(
    private val userProfileRepository: UserProfileRepository
) : UserService {
    private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

    override suspend fun getProfile(userId: Int): UserProfile {
        logger.debug("Fetching profile for userId=$userId")

        return userProfileRepository.getProfile(userId)
            ?: throw NotFoundException(
                message = "Profile not found",
                errorCode = "PROFILE_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )
    }

    override suspend fun getPublicProfile(userId: Int, requesterId: Int): PublicProfile {
        logger.debug("Fetching public profile for userId=$userId (requesterId=$requesterId)")

        return userProfileRepository.getPublicProfile(userId, requesterId)
            ?: throw NotFoundException(
                message = "Profile not found",
                errorCode = "PROFILE_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )
    }

    override suspend fun updateProfile(userId: Int, request: UpdateProfileRequest): UserProfile {
        logger.info("Updating profile for userId=$userId")

        // Validate fitness level if provided
        request.fitnessLevel?.let { level ->
            ValidationUtils.validateRange(level, 1, 10, "fitnessLevel")
        }

        // Validate location if provided
        request.locationLat?.let { ValidationUtils.validateLatitude(it) }
        request.locationLong?.let { ValidationUtils.validateLongitude(it) }

        // Validate name if provided
        request.name?.let { name ->
            if (name.isBlank()) {
                throw ValidationException(
                    message = "Validation failed",
                    details = mapOf("name" to "Name cannot be empty")
                )
            }
        }

        val updatedProfile = userProfileRepository.updateProfile(userId, request)
            ?: throw NotFoundException(
                message = "Profile not found",
                errorCode = "PROFILE_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        logger.info("Profile updated successfully for userId=$userId")
        return updatedProfile
    }
}
