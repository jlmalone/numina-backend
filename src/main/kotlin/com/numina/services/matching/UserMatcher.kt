package com.numina.services.matching

import com.numina.common.utils.ScoreCalculator
import com.numina.domain.PublicProfile
import com.numina.domain.UserMatch
import com.numina.domain.UserProfile
import org.slf4j.LoggerFactory

/**
 * User-to-user matching algorithm
 *
 * Weighted scoring:
 * - Fitness level similarity: 20%
 * - Shared interests: 30%
 * - Geographic proximity: 25%
 * - Schedule compatibility: 20%
 * - Past interactions: 5%
 */
class UserMatcher {
    private val logger = LoggerFactory.getLogger(UserMatcher::class.java)

    companion object {
        // Scoring weights (must sum to 1.0)
        private val WEIGHTS = mapOf(
            "fitnessLevel" to 0.20,
            "interests" to 0.30,
            "proximity" to 0.25,
            "schedule" to 0.20,
            "pastInteractions" to 0.05
        )
    }

    /**
     * Calculate match score between current user and a potential partner
     */
    fun calculateMatch(
        currentUserProfile: UserProfile,
        candidateProfile: PublicProfile,
        maxDistanceKm: Double = 10.0,
        pastInteractionScore: Double = 0.0 // TODO: Implement from match history
    ): UserMatch {
        logger.debug("Calculating match: user=${currentUserProfile.userId} vs candidate=${candidateProfile.userId}")

        // Calculate individual component scores
        val scores = mutableMapOf<String, Double>()

        // 1. Fitness level similarity (20%)
        scores["fitnessLevel"] = ScoreCalculator.calculateFitnessLevelScore(
            currentUserProfile.fitnessLevel,
            candidateProfile.fitnessLevel
        )

        // 2. Shared interests (30%)
        val sharedInterests = currentUserProfile.fitnessInterests.intersect(
            (candidateProfile.fitnessInterests ?: emptyList()).toSet()
        ).toList()

        scores["interests"] = ScoreCalculator.calculateSharedInterestsScore(
            currentUserProfile.fitnessInterests,
            candidateProfile.fitnessInterests ?: emptyList()
        )

        // 3. Geographic proximity (25%)
        val distance = if (currentUserProfile.locationLat != null &&
            currentUserProfile.locationLong != null &&
            candidateProfile.locationLat != null &&
            candidateProfile.locationLong != null
        ) {
            ScoreCalculator.calculateDistance(
                currentUserProfile.locationLat,
                currentUserProfile.locationLong,
                candidateProfile.locationLat,
                candidateProfile.locationLong
            )
        } else {
            null
        }

        scores["proximity"] = if (distance != null) {
            ScoreCalculator.calculateProximityScore(distance, maxDistanceKm)
        } else {
            50.0 // Neutral score if location unknown
        }

        // 4. Schedule compatibility (20%)
        scores["schedule"] = ScoreCalculator.calculateScheduleCompatibilityScore(
            currentUserProfile.availability,
            candidateProfile.availability ?: emptyMap()
        )

        // 5. Past interactions (5%)
        scores["pastInteractions"] = pastInteractionScore

        // Calculate weighted total score
        val matchScore = ScoreCalculator.calculateWeightedScore(scores, WEIGHTS)

        // Generate match reasons
        val matchReasons = ScoreCalculator.generateMatchReasons(scores)

        logger.debug("Match calculated: score=$matchScore, reasons=$matchReasons")

        return UserMatch(
            userId = candidateProfile.userId,
            profile = candidateProfile,
            matchScore = matchScore,
            matchReasons = matchReasons,
            sharedInterests = sharedInterests,
            distanceKm = distance
        )
    }

    /**
     * Filter and rank candidates by match score
     */
    fun rankCandidates(
        currentUserProfile: UserProfile,
        candidates: List<PublicProfile>,
        minScore: Int = 60,
        maxDistanceKm: Double = 10.0,
        limit: Int = 20
    ): List<UserMatch> {
        logger.info("Ranking ${candidates.size} candidates for user ${currentUserProfile.userId}")

        return candidates
            .map { calculateMatch(currentUserProfile, it, maxDistanceKm) }
            .filter { it.matchScore >= minScore }
            .sortedByDescending { it.matchScore }
            .take(limit)
            .also {
                logger.info("Ranked results: ${it.size} matches above score $minScore")
            }
    }

    /**
     * Check if two users are a mutual match (both meet minimum threshold)
     */
    fun isMutualMatch(
        user1Profile: UserProfile,
        user2Profile: PublicProfile,
        minScore: Int = 60
    ): Boolean {
        val matchScore = calculateMatch(user1Profile, user2Profile).matchScore
        return matchScore >= minScore
    }
}
