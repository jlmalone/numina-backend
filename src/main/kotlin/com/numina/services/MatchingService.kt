package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.data.repositories.ClassRepository
import com.numina.data.repositories.MatchRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.*
import com.numina.services.matching.ClassMatcher
import com.numina.services.matching.UserMatcher
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory

interface MatchingService {
    suspend fun getPartnerMatches(
        userId: Int,
        limit: Int = 20,
        minScore: Int = 60,
        radiusKm: Double = 10.0
    ): List<UserMatch>

    suspend fun getClassMatches(
        userId: Int,
        limit: Int = 20,
        minScore: Int = 50,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): List<ClassMatch>

    suspend fun getMutualMatches(userId: Int): List<MutualMatch>

    suspend fun recordMatchAction(
        userId: Int,
        request: MatchActionRequest
    ): MatchActionResponse
}

class MatchingServiceImpl(
    private val userProfileRepository: UserProfileRepository,
    private val classRepository: ClassRepository,
    private val matchRepository: MatchRepository,
    private val userMatcher: UserMatcher,
    private val classMatcher: ClassMatcher
) : MatchingService {
    private val logger = LoggerFactory.getLogger(MatchingServiceImpl::class.java)

    override suspend fun getPartnerMatches(
        userId: Int,
        limit: Int,
        minScore: Int,
        radiusKm: Double
    ): List<UserMatch> {
        logger.info("Finding partner matches for user $userId")

        // Get current user's profile
        val currentUserProfile = userProfileRepository.getProfile(userId)
            ?: throw NotFoundException(
                message = "User profile not found",
                errorCode = "PROFILE_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        // Get match preferences if set
        val preferences = matchRepository.getMatchPreferences(userId)
        val maxDistance = preferences?.maxDistanceKm ?: radiusKm

        // Get all user profiles (excluding current user)
        // TODO: Optimize with database filtering (location prefiltering, fitness level range, etc.)
        val allProfiles = getAllOtherUserProfiles(userId)

        // Filter by location radius if both users have location set
        val nearbyProfiles = if (currentUserProfile.locationLat != null && currentUserProfile.locationLong != null) {
            allProfiles.filter { profile ->
                profile.locationLat != null && profile.locationLong != null
            }
        } else {
            allProfiles
        }

        // Calculate and rank matches
        val matches = userMatcher.rankCandidates(
            currentUserProfile = currentUserProfile,
            candidates = nearbyProfiles,
            minScore = minScore,
            maxDistanceKm = maxDistance,
            limit = limit
        )

        logger.info("Found ${matches.size} partner matches for user $userId")
        return matches
    }

    override suspend fun getClassMatches(
        userId: Int,
        limit: Int,
        minScore: Int,
        startDate: Instant?,
        endDate: Instant?
    ): List<ClassMatch> {
        logger.info("Finding class matches for user $userId")

        // Get current user's profile
        val currentUserProfile = userProfileRepository.getProfile(userId)
            ?: throw NotFoundException(
                message = "User profile not found",
                errorCode = "PROFILE_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        // Get preferences
        val preferences = matchRepository.getMatchPreferences(userId)
        val maxDistance = preferences?.maxDistanceKm ?: 10.0

        // Get classes within date range
        val filters = ClassFilters(
            startDate = startDate,
            endDate = endDate
        )
        val classes = classRepository.getClasses(filters)

        // Calculate and rank class matches
        val matches = classMatcher.rankClasses(
            userProfile = currentUserProfile,
            classes = classes,
            minScore = minScore,
            maxDistanceKm = maxDistance,
            maxPrice = null, // TODO: Add budget preference
            limit = limit
        )

        logger.info("Found ${matches.size} class matches for user $userId")
        return matches
    }

    override suspend fun getMutualMatches(userId: Int): List<MutualMatch> {
        logger.info("Getting mutual matches for user $userId")

        // Get all mutual match records
        val mutualMatchRecords = matchRepository.getMutualMatches(userId)

        // Fetch profiles and create response
        val mutualMatches = mutualMatchRecords.mapNotNull { (partnerId, matchScore) ->
            val profile = userProfileRepository.getPublicProfile(partnerId, userId)
            if (profile != null) {
                MutualMatch(
                    userId = partnerId,
                    profile = profile,
                    matchScore = matchScore,
                    matchedAt = kotlinx.datetime.Clock.System.now() // TODO: Get from DB
                )
            } else {
                null
            }
        }

        logger.info("Found ${mutualMatches.size} mutual matches for user $userId")
        return mutualMatches
    }

    override suspend fun recordMatchAction(
        userId: Int,
        request: MatchActionRequest
    ): MatchActionResponse {
        logger.info("Recording match action: user $userId ${request.action} user ${request.targetUserId}")

        // Validate that target user exists
        val targetProfile = userProfileRepository.getProfile(request.targetUserId)
            ?: throw NotFoundException(
                message = "Target user not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("targetUserId" to request.targetUserId.toString())
            )

        // Record the action
        matchRepository.recordMatchAction(userId, request.targetUserId, request.action)

        // Check if it's a mutual match
        val isMutual = when (request.action) {
            MatchAction.LIKE, MatchAction.SUPER_LIKE -> {
                // Check if the other user also liked this user
                matchRepository.hasUserLiked(request.targetUserId, userId)
            }
            MatchAction.PASS -> false
        }

        // If mutual, create mutual match record
        val match = if (isMutual) {
            // Get current user profile to calculate match score
            val currentUserProfile = userProfileRepository.getProfile(userId)
                ?: throw NotFoundException(
                    message = "User profile not found",
                    errorCode = "PROFILE_NOT_FOUND"
                )

            val targetPublicProfile = userProfileRepository.getPublicProfile(request.targetUserId, userId)
                ?: throw NotFoundException(
                    message = "Target profile not found",
                    errorCode = "PROFILE_NOT_FOUND"
                )

            // Calculate match score
            val userMatch = userMatcher.calculateMatch(currentUserProfile, targetPublicProfile)

            // Create mutual match record
            matchRepository.createMutualMatch(userId, request.targetUserId, userMatch.matchScore)

            logger.info("Mutual match created: users $userId and ${request.targetUserId}")
            userMatch
        } else {
            null
        }

        return MatchActionResponse(
            mutual = isMutual,
            match = match
        )
    }

    /**
     * Helper to get all user profiles except the current user
     */
    private suspend fun getAllOtherUserProfiles(userId: Int): List<PublicProfile> {
        return userProfileRepository.getAllPublicProfiles(userId, userId)
    }
}
