package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * User-to-user match result
 */
@Serializable
data class UserMatch(
    val userId: Int,
    val profile: PublicProfile,
    val matchScore: Int, // 0-100
    val matchReasons: List<String>,
    val sharedInterests: List<String>,
    val distanceKm: Double?
)

/**
 * User-to-class match result
 */
@Serializable
data class ClassMatch(
    val classId: Int,
    val classDetails: FitnessClass,
    val matchScore: Int, // 0-100
    val matchReasons: List<String>,
    val estimatedFit: String // "perfect", "good", "okay"
)

/**
 * Mutual match between two users
 */
@Serializable
data class MutualMatch(
    val userId: Int,
    val profile: PublicProfile,
    val matchScore: Int,
    val matchedAt: Instant
)

/**
 * Match action request
 */
@Serializable
data class MatchActionRequest(
    val targetUserId: Int,
    val action: MatchAction
)

/**
 * Match action types
 */
@Serializable
enum class MatchAction {
    LIKE,
    PASS,
    SUPER_LIKE;

    companion object {
        fun fromString(value: String): MatchAction {
            return when (value.lowercase()) {
                "like" -> LIKE
                "pass" -> PASS
                "super_like", "superlike" -> SUPER_LIKE
                else -> throw IllegalArgumentException("Invalid match action: $value")
            }
        }
    }

    fun toDbValue(): String {
        return when (this) {
            LIKE -> "like"
            PASS -> "pass"
            SUPER_LIKE -> "super_like"
        }
    }
}

/**
 * Match action response
 */
@Serializable
data class MatchActionResponse(
    val mutual: Boolean,
    val match: UserMatch?
)

/**
 * Match preferences
 */
@Serializable
data class MatchPreferencesModel(
    val userId: Int,
    val maxDistanceKm: Double = 10.0,
    val minFitnessLevel: Int? = null,
    val maxFitnessLevel: Int? = null,
    val preferredAgeMin: Int? = null,
    val preferredAgeMax: Int? = null
)

/**
 * Match scoring breakdown for debugging
 */
@Serializable
data class MatchScoreBreakdown(
    val fitnessLevelScore: Double,
    val interestsScore: Double,
    val proximityScore: Double,
    val scheduleScore: Double,
    val pastInteractionScore: Double,
    val totalScore: Int
)

/**
 * Class match scoring breakdown
 */
@Serializable
data class ClassMatchScoreBreakdown(
    val interestsScore: Double,
    val intensityScore: Double,
    val scheduleScore: Double,
    val locationScore: Double,
    val priceScore: Double,
    val totalScore: Int
)
