package com.numina.services

import com.numina.data.repositories.*
import com.numina.domain.*
import kotlinx.datetime.Instant

/**
 * Main service orchestrating all matching operations.
 */
interface MatchingService {
    /**
     * Get potential workout partners for a user
     */
    suspend fun getPartners(
        userId: Int,
        limit: Int = 20,
        minScore: Int = 60,
        radiusKm: Float = 10.0f
    ): List<UserMatch>

    /**
     * Get recommended classes for a user
     */
    suspend fun getRecommendedClasses(
        userId: Int,
        limit: Int = 20,
        minScore: Int = 50,
        startDate: Instant? = null,
        endDate: Instant? = null
    ): List<ClassMatch>

    /**
     * Get mutual matches (users who both liked each other)
     */
    suspend fun getMutualMatches(userId: Int): List<MutualMatch>

    /**
     * Record a match action (like/pass/super_like)
     * Returns true and the match if it creates a mutual match
     */
    suspend fun recordMatchAction(
        userId: Int,
        targetUserId: Int,
        action: MatchActionType
    ): MatchActionResponse
}

class MatchingServiceImpl(
    private val userMatcher: UserMatcher,
    private val classMatcher: ClassMatcher,
    private val matchActionRepository: MatchActionRepository,
    private val mutualMatchRepository: MutualMatchRepository,
    private val userProfileRepository: UserProfileRepository,
    private val scoreCalculator: ScoreCalculator
) : MatchingService {

    override suspend fun getPartners(
        userId: Int,
        limit: Int,
        minScore: Int,
        radiusKm: Float
    ): List<UserMatch> {
        return userMatcher.findPartners(userId, minScore, radiusKm, limit)
    }

    override suspend fun getRecommendedClasses(
        userId: Int,
        limit: Int,
        minScore: Int,
        startDate: Instant?,
        endDate: Instant?
    ): List<ClassMatch> {
        return classMatcher.findClasses(userId, minScore, startDate, endDate, limit)
    }

    override suspend fun getMutualMatches(userId: Int): List<MutualMatch> {
        // Get mutual matches from repository
        val mutualMatchesData = mutualMatchRepository.getMutualMatchesForUser(userId)

        // Convert to MutualMatch domain objects with profiles
        return mutualMatchesData.mapNotNull { matchData ->
            val profile = userProfileRepository.getPublicProfile(matchData.userId, userId)
                ?: return@mapNotNull null

            MutualMatch(
                userId = matchData.userId,
                profile = profile,
                matchScore = matchData.matchScore,
                matchedAt = matchData.matchedAt
            )
        }
    }

    override suspend fun recordMatchAction(
        userId: Int,
        targetUserId: Int,
        action: MatchActionType
    ): MatchActionResponse {
        // Don't allow users to match with themselves
        if (userId == targetUserId) {
            return MatchActionResponse(mutual = false)
        }

        // Record the action
        matchActionRepository.createOrUpdateAction(userId, targetUserId, action)

        // Check if this is a like/super_like action
        if (action != MatchActionType.LIKE && action != MatchActionType.SUPER_LIKE) {
            return MatchActionResponse(mutual = false)
        }

        // Check if the other user has also liked this user
        val otherUserLiked = matchActionRepository.hasLiked(targetUserId, userId)

        if (!otherUserLiked) {
            return MatchActionResponse(mutual = false)
        }

        // It's a mutual match! Calculate score and create the match
        val currentUserProfile = userProfileRepository.getProfile(userId)
        val targetUserProfile = userProfileRepository.getProfile(targetUserId)

        if (currentUserProfile == null || targetUserProfile == null) {
            return MatchActionResponse(mutual = false)
        }

        // Calculate distance
        val distanceKm = if (currentUserProfile.locationLat != null &&
            currentUserProfile.locationLong != null &&
            targetUserProfile.locationLat != null &&
            targetUserProfile.locationLong != null
        ) {
            scoreCalculator.calculateDistance(
                currentUserProfile.locationLat,
                currentUserProfile.locationLong,
                targetUserProfile.locationLat,
                targetUserProfile.locationLong
            )
        } else {
            null
        }

        // Calculate match score
        val scoreResult = scoreCalculator.calculateUserMatchScore(
            currentUserProfile,
            targetUserProfile,
            distanceKm,
            hasPreviousInteraction = true
        )

        // Create mutual match record
        mutualMatchRepository.createMutualMatch(userId, targetUserId, scoreResult.score)

        // Get shared interests
        val sharedInterests = currentUserProfile.fitnessInterests
            .intersect(targetUserProfile.fitnessInterests.toSet())
            .toList()

        // Get public profile
        val publicProfile = userProfileRepository.getPublicProfile(targetUserId, userId)
            ?: return MatchActionResponse(mutual = false)

        val userMatch = UserMatch(
            userId = targetUserId,
            profile = publicProfile,
            matchScore = scoreResult.score,
            matchReasons = scoreResult.reasons,
            sharedInterests = sharedInterests,
            distanceKm = distanceKm
        )

        return MatchActionResponse(
            mutual = true,
            match = userMatch
        )
    }
}
