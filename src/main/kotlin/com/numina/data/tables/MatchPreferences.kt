package com.numina.data.tables

import org.jetbrains.exposed.sql.Table

object MatchPreferences : Table("match_preferences") {
    val userId = integer("user_id").references(Users.id)
    val maxDistanceKm = float("max_distance_km").default(10.0f)
    val minFitnessLevel = integer("min_fitness_level").nullable()
    val maxFitnessLevel = integer("max_fitness_level").nullable()
    val preferredAgeMin = integer("preferred_age_min").nullable()
    val preferredAgeMax = integer("preferred_age_max").nullable()

    override val primaryKey = PrimaryKey(userId)

    init {
        // Index for lookups by user
        index(false, userId)
    }
}
