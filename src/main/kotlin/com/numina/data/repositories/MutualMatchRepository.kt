package com.numina.data.repositories

import com.numina.data.tables.MutualMatches
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface MutualMatchRepository {
    suspend fun createMutualMatch(user1Id: Int, user2Id: Int, matchScore: Int): Boolean
    suspend fun getMutualMatchesForUser(userId: Int): List<MutualMatchData>
    suspend fun isMutualMatch(user1Id: Int, user2Id: Int): Boolean
    suspend fun getMutualMatchScore(user1Id: Int, user2Id: Int): Int?
}

data class MutualMatchData(
    val userId: Int,
    val matchScore: Int,
    val matchedAt: Instant
)

class MutualMatchRepositoryImpl : MutualMatchRepository {
    private fun normalizeUserIds(user1Id: Int, user2Id: Int): Pair<Int, Int> {
        return if (user1Id < user2Id) Pair(user1Id, user2Id) else Pair(user2Id, user1Id)
    }

    override suspend fun createMutualMatch(user1Id: Int, user2Id: Int, matchScore: Int): Boolean = transaction {
        val (normalizedUser1, normalizedUser2) = normalizeUserIds(user1Id, user2Id)

        try {
            MutualMatches.insert {
                it[MutualMatches.user1Id] = normalizedUser1
                it[MutualMatches.user2Id] = normalizedUser2
                it[MutualMatches.matchScore] = matchScore
                it[matchedAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
            }
            true
        } catch (e: Exception) {
            // Duplicate key error - match already exists
            false
        }
    }

    override suspend fun getMutualMatchesForUser(userId: Int): List<MutualMatchData> = transaction {
        MutualMatches.select {
            (MutualMatches.user1Id eq userId) or (MutualMatches.user2Id eq userId)
        }.map { row ->
            val otherUserId = if (row[MutualMatches.user1Id] == userId) {
                row[MutualMatches.user2Id]
            } else {
                row[MutualMatches.user1Id]
            }

            MutualMatchData(
                userId = otherUserId,
                matchScore = row[MutualMatches.matchScore],
                matchedAt = row[MutualMatches.matchedAt].toKotlinInstant()
            )
        }
    }

    override suspend fun isMutualMatch(user1Id: Int, user2Id: Int): Boolean = transaction {
        val (normalizedUser1, normalizedUser2) = normalizeUserIds(user1Id, user2Id)

        MutualMatches.select {
            (MutualMatches.user1Id eq normalizedUser1) and (MutualMatches.user2Id eq normalizedUser2)
        }.count() > 0
    }

    override suspend fun getMutualMatchScore(user1Id: Int, user2Id: Int): Int? = transaction {
        val (normalizedUser1, normalizedUser2) = normalizeUserIds(user1Id, user2Id)

        MutualMatches.select {
            (MutualMatches.user1Id eq normalizedUser1) and (MutualMatches.user2Id eq normalizedUser2)
        }.map { row -> row[MutualMatches.matchScore] }.singleOrNull()
    }
}
