package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

// ============================
// User-to-User Matching
// ============================

@Serializable
data class UserMatch(
    val userId: Int,
    val profile: PublicProfile,
    val matchScore: Int, // 0-100
    val matchReasons: List<String>,
    val sharedInterests: List<String>,
    val distanceKm: Float? = null
)

@Serializable
data class MutualMatch(
    val userId: Int,
    val profile: PublicProfile,
    val matchScore: Int,
    val matchedAt: Instant
)

// ============================
// User-to-Class Matching
// ============================

@Serializable
data class ClassMatch(
    val classId: Int,
    val classDetails: FitnessClass,
    val matchScore: Int, // 0-100
    val matchReasons: List<String>,
    val estimatedFit: String // "perfect" | "good" | "okay"
)

// ============================
// Match Actions
// ============================

@Serializable
data class MatchAction(
    val id: Int,
    val userId: Int,
    val targetUserId: Int,
    val action: MatchActionType,
    val createdAt: Instant
)

@Serializable
enum class MatchActionType {
    LIKE,
    PASS,
    SUPER_LIKE
}

@Serializable
data class MatchActionRequest(
    val targetUserId: Int,
    val action: MatchActionType
)

@Serializable
data class MatchActionResponse(
    val mutual: Boolean,
    val match: UserMatch? = null
)

// ============================
// Match Preferences
// ============================

@Serializable
data class MatchPreferences(
    val userId: Int,
    val maxDistanceKm: Float = 10.0f,
    val minFitnessLevel: Int? = null,
    val maxFitnessLevel: Int? = null,
    val preferredAgeMin: Int? = null,
    val preferredAgeMax: Int? = null
)

@Serializable
data class UpdateMatchPreferencesRequest(
    val maxDistanceKm: Float? = null,
    val minFitnessLevel: Int? = null,
    val maxFitnessLevel: Int? = null,
    val preferredAgeMin: Int? = null,
    val preferredAgeMax: Int? = null
)
