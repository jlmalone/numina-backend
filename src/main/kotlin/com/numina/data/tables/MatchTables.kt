package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object MatchActions : UUIDTable("match_actions") {
    val userId = integer("user_id").references(Users.id)
    val targetUserId = integer("target_user_id").references(Users.id)
    val action = varchar("action", 20) // 'like', 'pass', 'super_like'
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(userId, targetUserId)
    }
}

object MutualMatches : UUIDTable("mutual_matches") {
    val user1Id = integer("user1_id").references(Users.id)
    val user2Id = integer("user2_id").references(Users.id)
    val matchScore = integer("match_score")
    val matchedAt = timestamp("matched_at")

    init {
        uniqueIndex(user1Id, user2Id)
        // Ensure canonical ordering: user1_id < user2_id enforced at application level
    }
}

object MatchPreferences : org.jetbrains.exposed.sql.Table("match_preferences") {
    val userId = integer("user_id").references(Users.id).uniqueIndex()
    val maxDistanceKm = double("max_distance_km").default(10.0)
    val minFitnessLevel = integer("min_fitness_level").nullable()
    val maxFitnessLevel = integer("max_fitness_level").nullable()
    val preferredAgeMin = integer("preferred_age_min").nullable()
    val preferredAgeMax = integer("preferred_age_max").nullable()

    override val primaryKey = PrimaryKey(userId)
}
