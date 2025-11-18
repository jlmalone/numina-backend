package com.numina.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Database Tables for Messaging System
 */

object Messages : Table("messages") {
    val id = varchar("id", 36)  // UUID
    val conversationId = varchar("conversation_id", 36).index()  // Foreign key to Conversations
    val senderId = integer("sender_id").references(Users.id).index()  // Foreign key to Users
    val content = text("content")
    val sentAt = timestamp("sent_at").index()
    val deliveredAt = timestamp("delivered_at").nullable()
    val readAt = timestamp("read_at").nullable()
    val deleted = bool("deleted").default(false)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Conversations : Table("conversations") {
    val id = varchar("id", 36)  // UUID
    val participant1Id = integer("participant_1_id").references(Users.id).index()
    val participant2Id = integer("participant_2_id").references(Users.id).index()
    val lastMessageAt = timestamp("last_message_at").index()
    val createdAt = timestamp("created_at")
    val archivedByUser1 = bool("archived_by_user_1").default(false)
    val archivedByUser2 = bool("archived_by_user_2").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        // Composite index for finding conversations by participants
        index(isUnique = true, participant1Id, participant2Id)
    }
}

object BlockedUsers : Table("blocked_users") {
    val id = varchar("id", 36)  // UUID
    val blockerId = integer("blocker_id").references(Users.id).index()
    val blockedId = integer("blocked_id").references(Users.id).index()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        // Prevent duplicate blocks
        uniqueIndex(blockerId, blockedId)
    }
}

object MessageReports : Table("message_reports") {
    val id = varchar("id", 36)  // UUID
    val messageId = varchar("message_id", 36).references(Messages.id).index()
    val reporterId = integer("reporter_id").references(Users.id).index()
    val reason = varchar("reason", 500)
    val status = varchar("status", 20).default("PENDING")  // PENDING, REVIEWED, RESOLVED
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
