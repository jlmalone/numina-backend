package com.numina.messaging

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Domain Models for Messaging System
 */

@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: Int,
    val content: String,
    val sentAt: Instant,
    val deliveredAt: Instant? = null,
    val readAt: Instant? = null,
    val deleted: Boolean = false
)

@Serializable
data class Conversation(
    val id: String,
    val participant1Id: Int,
    val participant2Id: Int,
    val lastMessageAt: Instant,
    val lastMessage: String? = null,
    val unreadCount: Int = 0,
    val otherParticipant: ConversationParticipant? = null
)

@Serializable
data class ConversationParticipant(
    val id: Int,
    val name: String,
    val email: String
)

@Serializable
data class BlockedUser(
    val id: String,
    val blockerId: Int,
    val blockedId: Int,
    val createdAt: Instant
)

@Serializable
data class MessageReport(
    val id: String,
    val messageId: String,
    val reporterId: Int,
    val reason: String,
    val status: ReportStatus,
    val createdAt: Instant
)

@Serializable
enum class ReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED
}

/**
 * Request DTOs
 */

@Serializable
data class SendMessageRequest(
    val recipientId: Int,
    val content: String
)

@Serializable
data class MarkAsReadRequest(
    val messageIds: List<String>? = null  // If null, marks all messages in conversation as read
)

@Serializable
data class ReportMessageRequest(
    val reason: String
)

/**
 * Response DTOs
 */

@Serializable
data class SendMessageResponse(
    val message: Message,
    val conversationId: String
)

@Serializable
data class ConversationListResponse(
    val conversations: List<Conversation>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class MessageListResponse(
    val messages: List<Message>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UnreadCountResponse(
    val count: Int
)

@Serializable
data class BlockUserResponse(
    val blockedUserId: Int,
    val success: Boolean
)

@Serializable
data class ReportMessageResponse(
    val reportId: String,
    val status: ReportStatus
)

/**
 * WebSocket Messages
 */

@Serializable
sealed class WebSocketMessage {
    @Serializable
    data class NewMessage(val message: Message) : WebSocketMessage()

    @Serializable
    data class MessageDelivered(val messageId: String, val deliveredAt: Instant) : WebSocketMessage()

    @Serializable
    data class MessageRead(val messageId: String, val readAt: Instant) : WebSocketMessage()

    @Serializable
    data class TypingIndicator(val conversationId: String, val userId: Int, val typing: Boolean) : WebSocketMessage()

    @Serializable
    data class UserOnlineStatus(val userId: Int, val online: Boolean, val lastSeen: Instant?) : WebSocketMessage()

    @Serializable
    data class Error(val message: String, val code: String) : WebSocketMessage()
}
