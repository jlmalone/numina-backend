package com.numina.data.repositories

import com.numina.data.tables.MatchActions
import com.numina.data.tables.MatchPreferences
import com.numina.data.tables.MutualMatches
import com.numina.domain.MatchAction
import com.numina.domain.MatchPreferencesModel
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface MatchRepository {
    suspend fun recordMatchAction(userId: Int, targetUserId: Int, action: MatchAction)
    suspend fun getMatchAction(userId: Int, targetUserId: Int): MatchAction?
    suspend fun hasUserLiked(userId: Int, targetUserId: Int): Boolean
    suspend fun createMutualMatch(user1Id: Int, user2Id: Int, matchScore: Int)
    suspend fun getMutualMatches(userId: Int): List<Pair<Int, Int>> // (partnerId, matchScore)
    suspend fun isMutualMatch(user1Id: Int, user2Id: Int): Boolean
    suspend fun getMatchPreferences(userId: Int): MatchPreferencesModel?
    suspend fun setMatchPreferences(preferences: MatchPreferencesModel)
    suspend fun getUsersWhoLiked(userId: Int): List<Int>
}

class MatchRepositoryImpl : MatchRepository {

    override suspend fun recordMatchAction(userId: Int, targetUserId: Int, action: MatchAction) = transaction {
        // Insert or update match action
        val existing = MatchActions.select {
            (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
        }.singleOrNull()

        if (existing != null) {
            // Update existing action
            MatchActions.update({
                (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
            }) {
                it[MatchActions.action] = action.toDbValue()
                it[createdAt] = Clock.System.now()
            }
        } else {
            // Insert new action
            MatchActions.insert {
                it[id] = UUID.randomUUID()
                it[MatchActions.userId] = userId
                it[MatchActions.targetUserId] = targetUserId
                it[MatchActions.action] = action.toDbValue()
                it[createdAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getMatchAction(userId: Int, targetUserId: Int): MatchAction? = transaction {
        MatchActions.select {
            (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
        }.singleOrNull()?.let {
            MatchAction.fromString(it[MatchActions.action])
        }
    }

    override suspend fun hasUserLiked(userId: Int, targetUserId: Int): Boolean = transaction {
        val action = getMatchAction(userId, targetUserId)
        action == MatchAction.LIKE || action == MatchAction.SUPER_LIKE
    }

    override suspend fun createMutualMatch(user1Id: Int, user2Id: Int, matchScore: Int) = transaction {
        // Ensure canonical ordering (user1Id < user2Id)
        val (canonicalUser1, canonicalUser2) = if (user1Id < user2Id) {
            Pair(user1Id, user2Id)
        } else {
            Pair(user2Id, user1Id)
        }

        // Check if already exists
        val existing = MutualMatches.select {
            (MutualMatches.user1Id eq canonicalUser1) and (MutualMatches.user2Id eq canonicalUser2)
        }.singleOrNull()

        if (existing == null) {
            MutualMatches.insert {
                it[id] = UUID.randomUUID()
                it[user1Id] = canonicalUser1
                it[user2Id] = canonicalUser2
                it[MutualMatches.matchScore] = matchScore
                it[matchedAt] = Clock.System.now()
            }
        }
    }

    override suspend fun getMutualMatches(userId: Int): List<Pair<Int, Int>> = transaction {
        MutualMatches.select {
            (MutualMatches.user1Id eq userId) or (MutualMatches.user2Id eq userId)
        }.map { row ->
            val user1 = row[MutualMatches.user1Id]
            val user2 = row[MutualMatches.user2Id]
            val score = row[MutualMatches.matchScore]

            // Return the other user's ID and the score
            if (user1 == userId) {
                Pair(user2, score)
            } else {
                Pair(user1, score)
            }
        }
    }

    override suspend fun isMutualMatch(user1Id: Int, user2Id: Int): Boolean = transaction {
        val (canonicalUser1, canonicalUser2) = if (user1Id < user2Id) {
            Pair(user1Id, user2Id)
        } else {
            Pair(user2Id, user1Id)
        }

        MutualMatches.select {
            (MutualMatches.user1Id eq canonicalUser1) and (MutualMatches.user2Id eq canonicalUser2)
        }.count() > 0
    }

    override suspend fun getMatchPreferences(userId: Int): MatchPreferencesModel? = transaction {
        MatchPreferences.select { MatchPreferences.userId eq userId }
            .singleOrNull()?.let { row ->
                MatchPreferencesModel(
                    userId = row[MatchPreferences.userId],
                    maxDistanceKm = row[MatchPreferences.maxDistanceKm],
                    minFitnessLevel = row[MatchPreferences.minFitnessLevel],
                    maxFitnessLevel = row[MatchPreferences.maxFitnessLevel],
                    preferredAgeMin = row[MatchPreferences.preferredAgeMin],
                    preferredAgeMax = row[MatchPreferences.preferredAgeMax]
                )
            }
    }

    override suspend fun setMatchPreferences(preferences: MatchPreferencesModel) = transaction {
        val existing = MatchPreferences.select {
            MatchPreferences.userId eq preferences.userId
        }.singleOrNull()

        if (existing != null) {
            MatchPreferences.update({ MatchPreferences.userId eq preferences.userId }) {
                it[maxDistanceKm] = preferences.maxDistanceKm
                it[minFitnessLevel] = preferences.minFitnessLevel
                it[maxFitnessLevel] = preferences.maxFitnessLevel
                it[preferredAgeMin] = preferences.preferredAgeMin
                it[preferredAgeMax] = preferences.preferredAgeMax
            }
        } else {
            MatchPreferences.insert {
                it[userId] = preferences.userId
                it[maxDistanceKm] = preferences.maxDistanceKm
                it[minFitnessLevel] = preferences.minFitnessLevel
                it[maxFitnessLevel] = preferences.maxFitnessLevel
                it[preferredAgeMin] = preferences.preferredAgeMin
                it[preferredAgeMax] = preferences.preferredAgeMax
            }
        }
    }

    override suspend fun getUsersWhoLiked(userId: Int): List<Int> = transaction {
        MatchActions.select {
            (MatchActions.targetUserId eq userId) and
            (MatchActions.action inList listOf("like", "super_like"))
        }.map { it[MatchActions.userId] }
    }
}
