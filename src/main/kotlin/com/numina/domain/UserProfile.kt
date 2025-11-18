package com.numina.domain

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: Int,
    val name: String,
    val bio: String? = null,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val fitnessInterests: List<String> = emptyList(),
    val fitnessLevel: Int? = null, // 1-10 scale
    val availability: Map<String, List<String>> = emptyMap(), // e.g., {"Monday": ["morning", "evening"]}
    val photoUrl: String? = null,
    val privacySettings: PrivacySettings = PrivacySettings()
)

@Serializable
data class PrivacySettings(
    val bioPublic: Boolean = true,
    val locationPublic: Boolean = true,
    val fitnessInterestsPublic: Boolean = true,
    val fitnessLevelPublic: Boolean = true,
    val availabilityPublic: Boolean = false
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val bio: String? = null,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val fitnessInterests: List<String>? = null,
    val fitnessLevel: Int? = null,
    val availability: Map<String, List<String>>? = null,
    val photoUrl: String? = null,
    val privacySettings: PrivacySettings? = null
)

@Serializable
data class PublicProfile(
    val userId: Int,
    val name: String,
    val bio: String? = null,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val fitnessInterests: List<String>? = null,
    val fitnessLevel: Int? = null,
    val availability: Map<String, List<String>>? = null,
    val photoUrl: String? = null
)
