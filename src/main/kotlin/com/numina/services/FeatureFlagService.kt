package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.FeatureFlagRepository
import com.numina.domain.CreateFeatureFlagRequest
import com.numina.domain.FeatureFlag
import com.numina.domain.UpdateFeatureFlagRequest
import org.slf4j.LoggerFactory
import java.util.UUID

interface FeatureFlagService {
    suspend fun createFlag(request: CreateFeatureFlagRequest): FeatureFlag
    suspend fun getFlag(name: String): FeatureFlag
    suspend fun getAllFlags(): List<FeatureFlag>
    suspend fun updateFlag(id: String, request: UpdateFeatureFlagRequest): FeatureFlag
    suspend fun deleteFlag(id: String): Boolean
    suspend fun isFlagEnabled(name: String): Boolean
}

class FeatureFlagServiceImpl(
    private val featureFlagRepository: FeatureFlagRepository
) : FeatureFlagService {
    private val logger = LoggerFactory.getLogger(FeatureFlagServiceImpl::class.java)

    override suspend fun createFlag(request: CreateFeatureFlagRequest): FeatureFlag {
        logger.info("Creating feature flag: ${request.name}")

        // Validate rollout percentage
        if (request.rolloutPercentage !in 0..100) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("rolloutPercentage" to "Must be between 0 and 100")
            )
        }

        // Check if flag already exists
        val existing = featureFlagRepository.getFlagByName(request.name)
        if (existing != null) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("name" to "Feature flag with this name already exists")
            )
        }

        return featureFlagRepository.createFlag(
            name = request.name,
            enabled = request.enabled,
            description = request.description,
            rolloutPercentage = request.rolloutPercentage
        )
    }

    override suspend fun getFlag(name: String): FeatureFlag {
        logger.debug("Fetching feature flag: $name")
        return featureFlagRepository.getFlagByName(name)
            ?: throw NotFoundException(
                message = "Feature flag not found",
                errorCode = "FEATURE_FLAG_NOT_FOUND",
                details = mapOf("name" to name)
            )
    }

    override suspend fun getAllFlags(): List<FeatureFlag> {
        logger.debug("Fetching all feature flags")
        return featureFlagRepository.getAllFlags()
    }

    override suspend fun updateFlag(id: String, request: UpdateFeatureFlagRequest): FeatureFlag {
        logger.info("Updating feature flag: $id")

        // Validate rollout percentage if provided
        request.rolloutPercentage?.let { percentage ->
            if (percentage !in 0..100) {
                throw ValidationException(
                    message = "Validation failed",
                    details = mapOf("rolloutPercentage" to "Must be between 0 and 100")
                )
            }
        }

        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("id" to "Invalid UUID format")
            )
        }

        return featureFlagRepository.updateFlag(uuid, request.enabled, request.description, request.rolloutPercentage)
            ?: throw NotFoundException(
                message = "Feature flag not found",
                errorCode = "FEATURE_FLAG_NOT_FOUND",
                details = mapOf("id" to id)
            )
    }

    override suspend fun deleteFlag(id: String): Boolean {
        logger.info("Deleting feature flag: $id")

        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("id" to "Invalid UUID format")
            )
        }

        return featureFlagRepository.deleteFlag(uuid)
    }

    override suspend fun isFlagEnabled(name: String): Boolean {
        val flag = featureFlagRepository.getFlagByName(name) ?: return false
        return flag.enabled
    }
}
