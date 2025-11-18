package com.numina.data.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb

object UserProfiles : Table("user_profiles") {
    val userId = integer("user_id").references(Users.id).uniqueIndex()
    val name = varchar("name", 255)
    val bio = text("bio").nullable()
    val locationLat = double("location_lat").nullable()
    val locationLong = double("location_long").nullable()
    val fitnessInterests = jsonb<List<String>>("fitness_interests", Json.Default)
    val fitnessLevel = integer("fitness_level").nullable()
    val availability = jsonb<Map<String, List<String>>>("availability", Json.Default)
    val photoUrl = varchar("photo_url", 500).nullable()
    val privacySettings = jsonb<Map<String, Boolean>>("privacy_settings", Json.Default)

    override val primaryKey = PrimaryKey(userId)
}
