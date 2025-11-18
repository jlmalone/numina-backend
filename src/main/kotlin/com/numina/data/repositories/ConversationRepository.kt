package com.numina.data.repositories

import com.numina.data.tables.Conversations
import com.numina.data.tables.Messages
import com.numina.data.tables.Users
import com.numina.messaging.Conversation
import com.numina.messaging.ConversationParticipant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface ConversationRepository {
    suspend fun createConversation(participant1Id: Int, participant2Id: Int): Conversation
    suspend fun getConversationById(id: String): Conversation?
    suspend fun findConversationByParticipants(userId1: Int, userId2: Int): Conversation?
    suspend fun getConversationsForUser(
        userId: Int,
        page: Int = 1,
        pageSize: Int = 20,
        includeArchived: Boolean = false
    ): Pair<List<Conversation>, Int>
    suspend fun updateLastMessage(conversationId: String, timestamp: Instant): Boolean
    suspend fun archiveConversation(conversationId: String, userId: Int): Boolean
    suspend fun deleteConversation(conversationId: String): Boolean
}

class ConversationRepositoryImpl(
    private val userRepository: UserRepository
) : ConversationRepository {

    private suspend fun resultRowToConversation(row: ResultRow, currentUserId: Int): Conversation {
        val conversationId = row[Conversations.id]
        val participant1Id = row[Conversations.participant1Id]
        val participant2Id = row[Conversations.participant2Id]

        // Determine the other participant
        val otherParticipantId = if (participant1Id == currentUserId) participant2Id else participant1Id

        // Get the other participant's info
        val otherUser = userRepository.findById(otherParticipantId)
        val otherParticipant = otherUser?.let {
            ConversationParticipant(
                id = it.id,
                name = it.email.substringBefore('@'), // Using email as name fallback
                email = it.email
            )
        }

        // Get last message
        val lastMessage = transaction {
            Messages.select {
                (Messages.conversationId eq conversationId) and
                (Messages.deleted eq false)
            }
                .orderBy(Messages.sentAt to SortOrder.DESC)
                .limit(1)
                .map { it[Messages.content] }
                .firstOrNull()
        }

        // Get unread count for current user
        val unreadCount = transaction {
            Messages.select {
                (Messages.conversationId eq conversationId) and
                (Messages.senderId neq currentUserId) and
                (Messages.readAt.isNull()) and
                (Messages.deleted eq false)
            }.count().toInt()
        }

        return Conversation(
            id = conversationId,
            participant1Id = participant1Id,
            participant2Id = participant2Id,
            lastMessageAt = row[Conversations.lastMessageAt],
            lastMessage = lastMessage,
            unreadCount = unreadCount,
            otherParticipant = otherParticipant
        )
    }

    override suspend fun createConversation(participant1Id: Int, participant2Id: Int): Conversation = transaction {
        val now = Clock.System.now()
        val conversationId = UUID.randomUUID().toString()

        // Ensure participant1Id is always the smaller ID for consistency
        val (p1, p2) = if (participant1Id < participant2Id) {
            participant1Id to participant2Id
        } else {
            participant2Id to participant1Id
        }

        Conversations.insert {
            it[id] = conversationId
            it[Conversations.participant1Id] = p1
            it[Conversations.participant2Id] = p2
            it[lastMessageAt] = now
            it[createdAt] = now
            it[archivedByUser1] = false
            it[archivedByUser2] = false
        }

        Conversation(
            id = conversationId,
            participant1Id = p1,
            participant2Id = p2,
            lastMessageAt = now,
            lastMessage = null,
            unreadCount = 0
        )
    }

    override suspend fun getConversationById(id: String): Conversation? = transaction {
        Conversations.select { Conversations.id eq id }
            .map {
                // We don't have currentUserId here, so we'll use participant1
                val participant1Id = it[Conversations.participant1Id]
                resultRowToConversation(it, participant1Id)
            }
            .singleOrNull()
    }

    override suspend fun findConversationByParticipants(userId1: Int, userId2: Int): Conversation? = transaction {
        val (p1, p2) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1

        Conversations.select {
            (Conversations.participant1Id eq p1) and (Conversations.participant2Id eq p2)
        }
            .map { resultRowToConversation(it, userId1) }
            .singleOrNull()
    }

    override suspend fun getConversationsForUser(
        userId: Int,
        page: Int,
        pageSize: Int,
        includeArchived: Boolean
    ): Pair<List<Conversation>, Int> = transaction {
        val offset = (page - 1) * pageSize

        var query = Conversations.select {
            (Conversations.participant1Id eq userId) or (Conversations.participant2Id eq userId)
        }

        if (!includeArchived) {
            query = query.andWhere {
                ((Conversations.participant1Id eq userId) and (Conversations.archivedByUser1 eq false)) or
                ((Conversations.participant2Id eq userId) and (Conversations.archivedByUser2 eq false))
            }
        }

        val total = query.count().toInt()

        val conversations = query
            .orderBy(Conversations.lastMessageAt to SortOrder.DESC)
            .limit(pageSize, offset.toLong())
            .map { resultRowToConversation(it, userId) }

        Pair(conversations, total)
    }

    override suspend fun updateLastMessage(conversationId: String, timestamp: Instant): Boolean = transaction {
        val updated = Conversations.update({ Conversations.id eq conversationId }) {
            it[lastMessageAt] = timestamp
        }
        updated > 0
    }

    override suspend fun archiveConversation(conversationId: String, userId: Int): Boolean = transaction {
        val conversation = Conversations.select { Conversations.id eq conversationId }
            .singleOrNull() ?: return@transaction false

        val participant1Id = conversation[Conversations.participant1Id]
        val participant2Id = conversation[Conversations.participant2Id]

        val updated = when (userId) {
            participant1Id -> Conversations.update({ Conversations.id eq conversationId }) {
                it[archivedByUser1] = true
            }
            participant2Id -> Conversations.update({ Conversations.id eq conversationId }) {
                it[archivedByUser2] = true
            }
            else -> 0
        }

        updated > 0
    }

    override suspend fun deleteConversation(conversationId: String): Boolean = transaction {
        // Soft delete by archiving for both users
        val updated = Conversations.update({ Conversations.id eq conversationId }) {
            it[archivedByUser1] = true
            it[archivedByUser2] = true
        }
        updated > 0
    }
}
