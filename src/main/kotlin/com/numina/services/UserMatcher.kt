package com.numina.services

import com.numina.data.repositories.MatchActionRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.PublicProfile
import com.numina.domain.UserMatch
import com.numina.domain.UserProfile

/**
 * Service for matching users to potential workout partners.
 */
interface UserMatcher {
    /**
     * Find potential workout partners for the given user.
     * @param userId The user to find partners for
     * @param minScore Minimum match score (0-100)
     * @param maxDistance Maximum distance in km (default: 10)
     * @param limit Maximum number of results
     * @return List of matched users sorted by score (highest first)
     */
    suspend fun findPartners(
        userId: Int,
        minScore: Int = 60,
        maxDistance: Float = 10.0f,
        limit: Int = 20
    ): List<UserMatch>
}

class UserMatcherImpl(
    private val userProfileRepository: UserProfileRepository,
    private val matchActionRepository: MatchActionRepository,
    private val scoreCalculator: ScoreCalculator
) : UserMatcher {

    override suspend fun findPartners(
        userId: Int,
        minScore: Int,
        maxDistance: Float,
        limit: Int
    ): List<UserMatch> {
        // Get the current user's profile
        val currentUserProfile = userProfileRepository.getProfile(userId)
            ?: return emptyList()

        // Get all other user profiles (excluding current user)
        val candidateProfiles = userProfileRepository.getAllProfilesExcluding(userId, limit = 200)

        // Score and filter candidates
        val matches = candidateProfiles.mapNotNull { candidateProfile ->
            // Calculate distance if both users have location
            val distanceKm = if (currentUserProfile.locationLat != null &&
                currentUserProfile.locationLong != null &&
                candidateProfile.locationLat != null &&
                candidateProfile.locationLong != null
            ) {
                scoreCalculator.calculateDistance(
                    currentUserProfile.locationLat,
                    currentUserProfile.locationLong,
                    candidateProfile.locationLat,
                    candidateProfile.locationLong
                )
            } else {
                null
            }

            // Filter by distance if both users have location
            if (distanceKm != null && distanceKm > maxDistance) {
                return@mapNotNull null
            }

            // Check if there's previous interaction
            val hasPreviousInteraction = matchActionRepository.hasLiked(userId, candidateProfile.userId)

            // Calculate match score
            val scoreResult = scoreCalculator.calculateUserMatchScore(
                currentUserProfile,
                candidateProfile,
                distanceKm,
                hasPreviousInteraction
            )

            // Filter by minimum score
            if (scoreResult.score < minScore) {
                return@mapNotNull null
            }

            // Get shared interests
            val sharedInterests = currentUserProfile.fitnessInterests
                .intersect(candidateProfile.fitnessInterests.toSet())
                .toList()

            // Convert to public profile
            val publicProfile = convertToPublicProfile(candidateProfile)

            UserMatch(
                userId = candidateProfile.userId,
                profile = publicProfile,
                matchScore = scoreResult.score,
                matchReasons = scoreResult.reasons,
                sharedInterests = sharedInterests,
                distanceKm = distanceKm
            )
        }

        // Sort by score (highest first) and limit
        return matches
            .sortedByDescending { it.matchScore }
            .take(limit)
    }

    private fun convertToPublicProfile(profile: UserProfile): PublicProfile {
        val settings = profile.privacySettings
        return PublicProfile(
            userId = profile.userId,
            name = profile.name,
            bio = if (settings.bioPublic) profile.bio else null,
            locationLat = if (settings.locationPublic) profile.locationLat else null,
            locationLong = if (settings.locationPublic) profile.locationLong else null,
            fitnessInterests = if (settings.fitnessInterestsPublic) profile.fitnessInterests else null,
            fitnessLevel = if (settings.fitnessLevelPublic) profile.fitnessLevel else null,
            availability = if (settings.availabilityPublic) profile.availability else null,
            photoUrl = profile.photoUrl
        )
    }
}
