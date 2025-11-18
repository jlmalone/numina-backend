package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object FeatureFlags : UUIDTable("feature_flags") {
    val name = varchar("name", 100).uniqueIndex()
    val enabled = bool("enabled").default(false)
    val description = text("description").nullable()
    val rolloutPercentage = integer("rollout_percentage").default(100)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
