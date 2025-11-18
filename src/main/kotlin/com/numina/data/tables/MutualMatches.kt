package com.numina.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object MutualMatches : IntIdTable("mutual_matches") {
    val user1Id = integer("user1_id").references(Users.id)
    val user2Id = integer("user2_id").references(Users.id)
    val matchScore = integer("match_score")
    val matchedAt = timestamp("matched_at")

    init {
        // Unique constraint ensuring canonical ordering (user1_id < user2_id)
        uniqueIndex(user1Id, user2Id)
        // Index for finding matches by user
        index(false, user1Id)
        index(false, user2Id)
        // Check constraint would be: CHECK (user1_id < user2_id)
        // Note: Exposed doesn't support CHECK constraints directly in DSL
        // This must be enforced in the repository layer
    }
}
