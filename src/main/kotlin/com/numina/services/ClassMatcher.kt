package com.numina.services

import com.numina.data.repositories.ClassRepository
import com.numina.data.repositories.MatchPreferencesRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.ClassFilters
import com.numina.domain.ClassMatch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

/**
 * Service for matching users to recommended fitness classes.
 */
interface ClassMatcher {
    /**
     * Find recommended classes for the given user.
     * @param userId The user to find classes for
     * @param minScore Minimum match score (0-100)
     * @param startDate Start of date range (default: now)
     * @param endDate End of date range (default: +7 days)
     * @param limit Maximum number of results
     * @return List of matched classes sorted by score (highest first)
     */
    suspend fun findClasses(
        userId: Int,
        minScore: Int = 50,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 20
    ): List<ClassMatch>
}

class ClassMatcherImpl(
    private val userProfileRepository: UserProfileRepository,
    private val classRepository: ClassRepository,
    private val matchPreferencesRepository: MatchPreferencesRepository,
    private val scoreCalculator: ScoreCalculator
) : ClassMatcher {

    override suspend fun findClasses(
        userId: Int,
        minScore: Int,
        startDate: Instant?,
        endDate: Instant?,
        limit: Int
    ): List<ClassMatch> {
        // Get the user's profile
        val userProfile = userProfileRepository.getProfile(userId)
            ?: return emptyList()

        // Get the user's match preferences
        val preferences = matchPreferencesRepository.getPreferences(userId)

        // Set date range defaults
        val now = Clock.System.now()
        val searchStartDate = startDate ?: now
        val searchEndDate = endDate ?: (now + 7.days)

        // Query classes within date range
        val classes = classRepository.getClasses(
            ClassFilters(
                startDate = searchStartDate,
                endDate = searchEndDate,
                // Could add more filters based on user preferences
                locationLat = userProfile.locationLat,
                locationLong = userProfile.locationLong,
                radiusKm = (preferences.maxDistanceKm * 2).toDouble() // Query broader area, filter later
            )
        )

        // Score and filter classes
        val matches = classes.mapNotNull { fitnessClass ->
            // Calculate distance if user has location
            val distanceKm = if (userProfile.locationLat != null && userProfile.locationLong != null) {
                scoreCalculator.calculateDistance(
                    userProfile.locationLat,
                    userProfile.locationLong,
                    fitnessClass.locationLat,
                    fitnessClass.locationLong
                )
            } else {
                null
            }

            // Calculate match score
            val scoreResult = scoreCalculator.calculateClassMatchScore(
                userProfile,
                fitnessClass,
                distanceKm,
                preferences.maxDistanceKm,
                userMaxPrice = null // Could be added to preferences
            )

            // Filter by minimum score
            if (scoreResult.score < minScore) {
                return@mapNotNull null
            }

            // Determine fit level
            val estimatedFit = when {
                scoreResult.score >= 80 -> "perfect"
                scoreResult.score >= 65 -> "good"
                else -> "okay"
            }

            ClassMatch(
                classId = fitnessClass.id,
                classDetails = fitnessClass,
                matchScore = scoreResult.score,
                matchReasons = scoreResult.reasons,
                estimatedFit = estimatedFit
            )
        }

        // Sort by score (highest first) and limit
        return matches
            .sortedByDescending { it.matchScore }
            .take(limit)
    }
}
