package com.numina.data.repositories

import com.numina.data.tables.BlockedUsers
import com.numina.messaging.BlockedUser
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface BlockedUserRepository {
    suspend fun blockUser(blockerId: Int, blockedId: Int): BlockedUser
    suspend fun unblockUser(blockerId: Int, blockedId: Int): Boolean
    suspend fun isBlocked(userId1: Int, userId2: Int): Boolean
    suspend fun getBlockedUsers(userId: Int): List<BlockedUser>
}

class BlockedUserRepositoryImpl : BlockedUserRepository {

    private fun resultRowToBlockedUser(row: ResultRow): BlockedUser {
        return BlockedUser(
            id = row[BlockedUsers.id],
            blockerId = row[BlockedUsers.blockerId],
            blockedId = row[BlockedUsers.blockedId],
            createdAt = row[BlockedUsers.createdAt]
        )
    }

    override suspend fun blockUser(blockerId: Int, blockedId: Int): BlockedUser = transaction {
        val now = Clock.System.now()
        val blockId = UUID.randomUUID().toString()

        // Check if already blocked
        val existing = BlockedUsers.select {
            (BlockedUsers.blockerId eq blockerId) and (BlockedUsers.blockedId eq blockedId)
        }.singleOrNull()

        if (existing != null) {
            return@transaction resultRowToBlockedUser(existing)
        }

        BlockedUsers.insert {
            it[id] = blockId
            it[BlockedUsers.blockerId] = blockerId
            it[BlockedUsers.blockedId] = blockedId
            it[createdAt] = now
        }

        BlockedUser(
            id = blockId,
            blockerId = blockerId,
            blockedId = blockedId,
            createdAt = now
        )
    }

    override suspend fun unblockUser(blockerId: Int, blockedId: Int): Boolean = transaction {
        val deleted = BlockedUsers.deleteWhere {
            (BlockedUsers.blockerId eq blockerId) and (BlockedUsers.blockedId eq blockedId)
        }
        deleted > 0
    }

    override suspend fun isBlocked(userId1: Int, userId2: Int): Boolean = transaction {
        val blocked = BlockedUsers.select {
            ((BlockedUsers.blockerId eq userId1) and (BlockedUsers.blockedId eq userId2)) or
            ((BlockedUsers.blockerId eq userId2) and (BlockedUsers.blockedId eq userId1))
        }.count() > 0

        blocked
    }

    override suspend fun getBlockedUsers(userId: Int): List<BlockedUser> = transaction {
        BlockedUsers.select { BlockedUsers.blockerId eq userId }
            .map { resultRowToBlockedUser(it) }
    }
}
