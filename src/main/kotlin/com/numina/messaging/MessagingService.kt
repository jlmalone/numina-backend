package com.numina.messaging

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.*
import kotlinx.datetime.Clock

interface MessagingService {
    suspend fun sendMessage(senderId: Int, recipientId: Int, content: String): SendMessageResponse
    suspend fun getConversations(userId: Int, page: Int, pageSize: Int): ConversationListResponse
    suspend fun getMessages(conversationId: String, userId: Int, page: Int, pageSize: Int): MessageListResponse
    suspend fun markAsRead(conversationId: String, userId: Int): Boolean
    suspend fun blockUser(blockerId: Int, blockedId: Int): BlockUserResponse
    suspend fun unblockUser(blockerId: Int, blockedId: Int): Boolean
    suspend fun reportMessage(messageId: String, reporterId: Int, reason: String): ReportMessageResponse
    suspend fun getUnreadCount(userId: Int): UnreadCountResponse
    suspend fun deleteMessage(messageId: String, userId: Int): Boolean
}

class MessagingServiceImpl(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val blockedUserRepository: BlockedUserRepository,
    private val messageReportRepository: MessageReportRepository,
    private val userRepository: UserRepository
) : MessagingService {

    override suspend fun sendMessage(senderId: Int, recipientId: Int, content: String): SendMessageResponse {
        // Validation
        if (content.isBlank()) {
            throw ValidationException(
                message = "Message content cannot be empty",
                errorCode = "EMPTY_MESSAGE"
            )
        }

        if (content.length > 5000) {
            throw ValidationException(
                message = "Message content cannot exceed 5000 characters",
                errorCode = "MESSAGE_TOO_LONG"
            )
        }

        if (senderId == recipientId) {
            throw ValidationException(
                message = "Cannot send message to yourself",
                errorCode = "SELF_MESSAGE"
            )
        }

        // Check if users exist
        val sender = userRepository.findById(senderId)
            ?: throw NotFoundException("Sender not found", "USER_NOT_FOUND")

        val recipient = userRepository.findById(recipientId)
            ?: throw NotFoundException("Recipient not found", "USER_NOT_FOUND")

        // Check if blocked
        val isBlocked = blockedUserRepository.isBlocked(senderId, recipientId)
        if (isBlocked) {
            throw ForbiddenException(
                message = "Cannot send message to this user",
                errorCode = "USER_BLOCKED"
            )
        }

        // Find or create conversation
        var conversation = conversationRepository.findConversationByParticipants(senderId, recipientId)
        if (conversation == null) {
            conversation = conversationRepository.createConversation(senderId, recipientId)
        }

        // Create message
        val message = messageRepository.createMessage(conversation.id, senderId, content)

        // Update conversation last message timestamp
        conversationRepository.updateLastMessage(conversation.id, message.sentAt)

        return SendMessageResponse(
            message = message,
            conversationId = conversation.id
        )
    }

    override suspend fun getConversations(
        userId: Int,
        page: Int,
        pageSize: Int
    ): ConversationListResponse {
        if (page < 1) {
            throw ValidationException("Page must be >= 1", "INVALID_PAGE")
        }
        if (pageSize < 1 || pageSize > 100) {
            throw ValidationException("Page size must be between 1 and 100", "INVALID_PAGE_SIZE")
        }

        val (conversations, total) = conversationRepository.getConversationsForUser(
            userId = userId,
            page = page,
            pageSize = pageSize,
            includeArchived = false
        )

        return ConversationListResponse(
            conversations = conversations,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    override suspend fun getMessages(
        conversationId: String,
        userId: Int,
        page: Int,
        pageSize: Int
    ): MessageListResponse {
        if (page < 1) {
            throw ValidationException("Page must be >= 1", "INVALID_PAGE")
        }
        if (pageSize < 1 || pageSize > 100) {
            throw ValidationException("Page size must be between 1 and 100", "INVALID_PAGE_SIZE")
        }

        // Verify user is a participant in the conversation
        val conversation = conversationRepository.getConversationById(conversationId)
            ?: throw NotFoundException("Conversation not found", "CONVERSATION_NOT_FOUND")

        if (conversation.participant1Id != userId && conversation.participant2Id != userId) {
            throw ForbiddenException(
                message = "You do not have access to this conversation",
                errorCode = "CONVERSATION_FORBIDDEN"
            )
        }

        val (messages, total) = messageRepository.getMessagesByConversation(
            conversationId = conversationId,
            page = page,
            pageSize = pageSize
        )

        return MessageListResponse(
            messages = messages,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    override suspend fun markAsRead(conversationId: String, userId: Int): Boolean {
        // Verify user is a participant
        val conversation = conversationRepository.getConversationById(conversationId)
            ?: throw NotFoundException("Conversation not found", "CONVERSATION_NOT_FOUND")

        if (conversation.participant1Id != userId && conversation.participant2Id != userId) {
            throw ForbiddenException(
                message = "You do not have access to this conversation",
                errorCode = "CONVERSATION_FORBIDDEN"
            )
        }

        val now = Clock.System.now()
        val updatedCount = messageRepository.markConversationAsRead(conversationId, userId, now)

        return updatedCount > 0
    }

    override suspend fun blockUser(blockerId: Int, blockedId: Int): BlockUserResponse {
        if (blockerId == blockedId) {
            throw ValidationException("Cannot block yourself", "SELF_BLOCK")
        }

        // Check if blocked user exists
        val blockedUser = userRepository.findById(blockedId)
            ?: throw NotFoundException("User not found", "USER_NOT_FOUND")

        val block = blockedUserRepository.blockUser(blockerId, blockedId)

        return BlockUserResponse(
            blockedUserId = blockedId,
            success = true
        )
    }

    override suspend fun unblockUser(blockerId: Int, blockedId: Int): Boolean {
        return blockedUserRepository.unblockUser(blockerId, blockedId)
    }

    override suspend fun reportMessage(messageId: String, reporterId: Int, reason: String): ReportMessageResponse {
        if (reason.isBlank()) {
            throw ValidationException("Report reason cannot be empty", "EMPTY_REASON")
        }

        if (reason.length > 500) {
            throw ValidationException("Reason cannot exceed 500 characters", "REASON_TOO_LONG")
        }

        // Check if message exists
        val message = messageRepository.getMessageById(messageId)
            ?: throw NotFoundException("Message not found", "MESSAGE_NOT_FOUND")

        // Cannot report your own message
        if (message.senderId == reporterId) {
            throw ValidationException("Cannot report your own message", "SELF_REPORT")
        }

        val report = messageReportRepository.createReport(messageId, reporterId, reason)

        return ReportMessageResponse(
            reportId = report.id,
            status = report.status
        )
    }

    override suspend fun getUnreadCount(userId: Int): UnreadCountResponse {
        val count = messageRepository.getUnreadCount(userId)
        return UnreadCountResponse(count = count)
    }

    override suspend fun deleteMessage(messageId: String, userId: Int): Boolean {
        // Check if message exists
        val message = messageRepository.getMessageById(messageId)
            ?: throw NotFoundException("Message not found", "MESSAGE_NOT_FOUND")

        // Only the sender can delete their message
        if (message.senderId != userId) {
            throw ForbiddenException(
                message = "You can only delete your own messages",
                errorCode = "MESSAGE_DELETE_FORBIDDEN"
            )
        }

        return messageRepository.deleteMessage(messageId)
    }
}
