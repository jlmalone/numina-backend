package com.numina.data.repositories

import com.numina.data.tables.UserProfiles
import com.numina.domain.PrivacySettings
import com.numina.domain.PublicProfile
import com.numina.domain.UpdateProfileRequest
import com.numina.domain.UserProfile
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface UserProfileRepository {
    suspend fun createProfile(userId: Int, name: String): UserProfile?
    suspend fun getProfile(userId: Int): UserProfile?
    suspend fun getPublicProfile(userId: Int, requesterId: Int): PublicProfile?
    suspend fun updateProfile(userId: Int, request: UpdateProfileRequest): UserProfile?
}

class UserProfileRepositoryImpl : UserProfileRepository {
    private fun resultRowToProfile(row: ResultRow): UserProfile {
        val privacyMap = row[UserProfiles.privacySettings]
        val privacySettings = PrivacySettings(
            bioPublic = privacyMap["bioPublic"] as? Boolean ?: true,
            locationPublic = privacyMap["locationPublic"] as? Boolean ?: true,
            fitnessInterestsPublic = privacyMap["fitnessInterestsPublic"] as? Boolean ?: true,
            fitnessLevelPublic = privacyMap["fitnessLevelPublic"] as? Boolean ?: true,
            availabilityPublic = privacyMap["availabilityPublic"] as? Boolean ?: false
        )

        return UserProfile(
            userId = row[UserProfiles.userId],
            name = row[UserProfiles.name],
            bio = row[UserProfiles.bio],
            locationLat = row[UserProfiles.locationLat],
            locationLong = row[UserProfiles.locationLong],
            fitnessInterests = row[UserProfiles.fitnessInterests],
            fitnessLevel = row[UserProfiles.fitnessLevel],
            availability = row[UserProfiles.availability],
            photoUrl = row[UserProfiles.photoUrl],
            privacySettings = privacySettings
        )
    }

    override suspend fun createProfile(userId: Int, name: String): UserProfile? = transaction {
        UserProfiles.insert {
            it[UserProfiles.userId] = userId
            it[UserProfiles.name] = name
            it[fitnessInterests] = emptyList()
            it[availability] = emptyMap()
            it[privacySettings] = mapOf(
                "bioPublic" to true,
                "locationPublic" to true,
                "fitnessInterestsPublic" to true,
                "fitnessLevelPublic" to true,
                "availabilityPublic" to false
            )
        }

        UserProfiles.select { UserProfiles.userId eq userId }
            .map { resultRowToProfile(it) }
            .singleOrNull()
    }

    override suspend fun getProfile(userId: Int): UserProfile? = transaction {
        UserProfiles.select { UserProfiles.userId eq userId }
            .map { resultRowToProfile(it) }
            .singleOrNull()
    }

    override suspend fun getPublicProfile(userId: Int, requesterId: Int): PublicProfile? = transaction {
        val profile = UserProfiles.select { UserProfiles.userId eq userId }
            .map { resultRowToProfile(it) }
            .singleOrNull()

        profile?.let {
            val settings = it.privacySettings
            PublicProfile(
                userId = it.userId,
                name = it.name,
                bio = if (settings.bioPublic) it.bio else null,
                locationLat = if (settings.locationPublic) it.locationLat else null,
                locationLong = if (settings.locationPublic) it.locationLong else null,
                fitnessInterests = if (settings.fitnessInterestsPublic) it.fitnessInterests else null,
                fitnessLevel = if (settings.fitnessLevelPublic) it.fitnessLevel else null,
                availability = if (settings.availabilityPublic) it.availability else null,
                photoUrl = it.photoUrl
            )
        }
    }

    override suspend fun updateProfile(userId: Int, request: UpdateProfileRequest): UserProfile? = transaction {
        UserProfiles.update({ UserProfiles.userId eq userId }) {
            request.name?.let { name -> it[UserProfiles.name] = name }
            request.bio?.let { bio -> it[UserProfiles.bio] = bio }
            request.locationLat?.let { lat -> it[locationLat] = lat }
            request.locationLong?.let { long -> it[locationLong] = long }
            request.fitnessInterests?.let { interests -> it[fitnessInterests] = interests }
            request.fitnessLevel?.let { level -> it[fitnessLevel] = level }
            request.availability?.let { avail -> it[availability] = avail }
            request.photoUrl?.let { url -> it[photoUrl] = url }
            request.privacySettings?.let { settings ->
                it[privacySettings] = mapOf(
                    "bioPublic" to settings.bioPublic,
                    "locationPublic" to settings.locationPublic,
                    "fitnessInterestsPublic" to settings.fitnessInterestsPublic,
                    "fitnessLevelPublic" to settings.fitnessLevelPublic,
                    "availabilityPublic" to settings.availabilityPublic
                )
            }
        }

        UserProfiles.select { UserProfiles.userId eq userId }
            .map { resultRowToProfile(it) }
            .singleOrNull()
    }
}
