package com.numina.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object MatchActions : IntIdTable("match_actions") {
    val userId = integer("user_id").references(Users.id)
    val targetUserId = integer("target_user_id").references(Users.id)
    val action = varchar("action", 20) // 'LIKE', 'PASS', 'SUPER_LIKE'
    val createdAt = timestamp("created_at")

    init {
        // Unique constraint: one action per user-target pair
        uniqueIndex(userId, targetUserId)
        // Index for finding actions by user
        index(false, userId)
        // Index for finding actions by target user
        index(false, targetUserId)
    }
}
