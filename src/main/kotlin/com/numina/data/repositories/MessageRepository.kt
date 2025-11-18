package com.numina.data.repositories

import com.numina.data.tables.Messages
import com.numina.messaging.Message
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface MessageRepository {
    suspend fun createMessage(conversationId: String, senderId: Int, content: String): Message
    suspend fun getMessageById(id: String): Message?
    suspend fun getMessagesByConversation(
        conversationId: String,
        page: Int = 1,
        pageSize: Int = 50
    ): Pair<List<Message>, Int>
    suspend fun markAsDelivered(messageId: String, deliveredAt: Instant): Boolean
    suspend fun markAsRead(messageId: String, readAt: Instant): Boolean
    suspend fun markConversationAsRead(conversationId: String, userId: Int, readAt: Instant): Int
    suspend fun deleteMessage(messageId: String): Boolean
    suspend fun getUnreadCount(userId: Int): Int
}

class MessageRepositoryImpl : MessageRepository {

    private fun resultRowToMessage(row: ResultRow): Message {
        return Message(
            id = row[Messages.id],
            conversationId = row[Messages.conversationId],
            senderId = row[Messages.senderId],
            content = row[Messages.content],
            sentAt = row[Messages.sentAt],
            deliveredAt = row[Messages.deliveredAt],
            readAt = row[Messages.readAt],
            deleted = row[Messages.deleted]
        )
    }

    override suspend fun createMessage(
        conversationId: String,
        senderId: Int,
        content: String
    ): Message = transaction {
        val now = Clock.System.now()
        val messageId = UUID.randomUUID().toString()

        Messages.insert {
            it[id] = messageId
            it[Messages.conversationId] = conversationId
            it[Messages.senderId] = senderId
            it[Messages.content] = content
            it[sentAt] = now
            it[createdAt] = now
            it[deleted] = false
        }

        Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            sentAt = now,
            deliveredAt = null,
            readAt = null,
            deleted = false
        )
    }

    override suspend fun getMessageById(id: String): Message? = transaction {
        Messages.select { Messages.id eq id }
            .map { resultRowToMessage(it) }
            .singleOrNull()
    }

    override suspend fun getMessagesByConversation(
        conversationId: String,
        page: Int,
        pageSize: Int
    ): Pair<List<Message>, Int> = transaction {
        val offset = (page - 1) * pageSize

        val total = Messages.select {
            (Messages.conversationId eq conversationId) and
            (Messages.deleted eq false)
        }.count().toInt()

        val messages = Messages.select {
            (Messages.conversationId eq conversationId) and
            (Messages.deleted eq false)
        }
            .orderBy(Messages.sentAt to SortOrder.DESC)
            .limit(pageSize, offset.toLong())
            .map { resultRowToMessage(it) }

        Pair(messages, total)
    }

    override suspend fun markAsDelivered(messageId: String, deliveredAt: Instant): Boolean = transaction {
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[Messages.deliveredAt] = deliveredAt
        }
        updated > 0
    }

    override suspend fun markAsRead(messageId: String, readAt: Instant): Boolean = transaction {
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[Messages.readAt] = readAt
        }
        updated > 0
    }

    override suspend fun markConversationAsRead(
        conversationId: String,
        userId: Int,
        readAt: Instant
    ): Int = transaction {
        Messages.update({
            (Messages.conversationId eq conversationId) and
            (Messages.senderId neq userId) and
            (Messages.readAt.isNull())
        }) {
            it[Messages.readAt] = readAt
        }
    }

    override suspend fun deleteMessage(messageId: String): Boolean = transaction {
        val updated = Messages.update({ Messages.id eq messageId }) {
            it[deleted] = true
        }
        updated > 0
    }

    override suspend fun getUnreadCount(userId: Int): Int = transaction {
        // Get all conversations where user is a participant
        val userConversations = com.numina.data.tables.Conversations
            .slice(com.numina.data.tables.Conversations.id)
            .select {
                (com.numina.data.tables.Conversations.participant1Id eq userId) or
                (com.numina.data.tables.Conversations.participant2Id eq userId)
            }
            .map { it[com.numina.data.tables.Conversations.id] }

        // Count unread messages in those conversations where user is NOT the sender
        Messages.select {
            (Messages.conversationId inList userConversations) and
            (Messages.senderId neq userId) and
            (Messages.readAt.isNull()) and
            (Messages.deleted eq false)
        }.count().toInt()
    }
}
