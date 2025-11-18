package com.numina.data.repositories

import com.numina.data.tables.MatchActions
import com.numina.domain.MatchAction
import com.numina.domain.MatchActionType
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface MatchActionRepository {
    suspend fun createOrUpdateAction(userId: Int, targetUserId: Int, action: MatchActionType): MatchAction?
    suspend fun getAction(userId: Int, targetUserId: Int): MatchAction?
    suspend fun getUserActions(userId: Int): List<MatchAction>
    suspend fun getActionsOnUser(targetUserId: Int): List<MatchAction>
    suspend fun hasLiked(userId: Int, targetUserId: Int): Boolean
}

class MatchActionRepositoryImpl : MatchActionRepository {
    private fun resultRowToMatchAction(row: ResultRow): MatchAction {
        return MatchAction(
            id = row[MatchActions.id].value,
            userId = row[MatchActions.userId],
            targetUserId = row[MatchActions.targetUserId],
            action = MatchActionType.valueOf(row[MatchActions.action]),
            createdAt = row[MatchActions.createdAt].toKotlinInstant()
        )
    }

    override suspend fun createOrUpdateAction(userId: Int, targetUserId: Int, action: MatchActionType): MatchAction? = transaction {
        // Try to update existing action first
        val updated = MatchActions.update({
            (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
        }) {
            it[MatchActions.action] = action.name
            it[createdAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
        }

        if (updated > 0) {
            // Return the updated action
            MatchActions.select {
                (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
            }.map { resultRowToMatchAction(it) }.singleOrNull()
        } else {
            // Insert new action
            MatchActions.insert {
                it[MatchActions.userId] = userId
                it[MatchActions.targetUserId] = targetUserId
                it[MatchActions.action] = action.name
                it[createdAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
            }

            MatchActions.select {
                (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
            }.map { resultRowToMatchAction(it) }.singleOrNull()
        }
    }

    override suspend fun getAction(userId: Int, targetUserId: Int): MatchAction? = transaction {
        MatchActions.select {
            (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
        }.map { resultRowToMatchAction(it) }.singleOrNull()
    }

    override suspend fun getUserActions(userId: Int): List<MatchAction> = transaction {
        MatchActions.select { MatchActions.userId eq userId }
            .map { resultRowToMatchAction(it) }
    }

    override suspend fun getActionsOnUser(targetUserId: Int): List<MatchAction> = transaction {
        MatchActions.select { MatchActions.targetUserId eq targetUserId }
            .map { resultRowToMatchAction(it) }
    }

    override suspend fun hasLiked(userId: Int, targetUserId: Int): Boolean = transaction {
        val action = MatchActions.select {
            (MatchActions.userId eq userId) and (MatchActions.targetUserId eq targetUserId)
        }.map { row -> MatchActionType.valueOf(row[MatchActions.action]) }.singleOrNull()

        action == MatchActionType.LIKE || action == MatchActionType.SUPER_LIKE
    }
}
