package com.numina.messaging

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.*
import com.numina.domain.User
import kotlinx.datetime.Clock
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessagingServiceTest {

    private fun createMockUserRepository(): UserRepository {
        return object : UserRepository {
            override suspend fun create(email: String, passwordHash: String): User {
                return User(1, email, Clock.System.now(), Clock.System.now())
            }

            override suspend fun findByEmail(email: String): User? {
                return when (email) {
                    "user1@test.com" -> User(1, email, Clock.System.now(), Clock.System.now())
                    "user2@test.com" -> User(2, email, Clock.System.now(), Clock.System.now())
                    else -> null
                }
            }

            override suspend fun findById(id: Int): User? {
                return when (id) {
                    1 -> User(1, "user1@test.com", Clock.System.now(), Clock.System.now())
                    2 -> User(2, "user2@test.com", Clock.System.now(), Clock.System.now())
                    else -> null
                }
            }

            override suspend fun updatePassword(userId: Int, newPasswordHash: String): Boolean = true
            override suspend fun deleteUser(userId: Int): Boolean = true
        }
    }

    private fun createMockMessageRepository(): MessageRepository {
        return object : MessageRepository {
            private val messages = mutableListOf<Message>()

            override suspend fun createMessage(conversationId: String, senderId: Int, content: String): Message {
                val message = Message(
                    id = "msg-${messages.size + 1}",
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content,
                    sentAt = Clock.System.now(),
                    deliveredAt = null,
                    readAt = null,
                    deleted = false
                )
                messages.add(message)
                return message
            }

            override suspend fun getMessageById(id: String): Message? {
                return messages.find { it.id == id && !it.deleted }
            }

            override suspend fun getMessagesByConversation(
                conversationId: String,
                page: Int,
                pageSize: Int
            ): Pair<List<Message>, Int> {
                val filtered = messages.filter { it.conversationId == conversationId && !it.deleted }
                return Pair(filtered, filtered.size)
            }

            override suspend fun markAsDelivered(messageId: String, deliveredAt: kotlinx.datetime.Instant): Boolean {
                messages.find { it.id == messageId }?.let {
                    messages[messages.indexOf(it)] = it.copy(deliveredAt = deliveredAt)
                    return true
                }
                return false
            }

            override suspend fun markAsRead(messageId: String, readAt: kotlinx.datetime.Instant): Boolean {
                messages.find { it.id == messageId }?.let {
                    messages[messages.indexOf(it)] = it.copy(readAt = readAt)
                    return true
                }
                return false
            }

            override suspend fun markConversationAsRead(
                conversationId: String,
                userId: Int,
                readAt: kotlinx.datetime.Instant
            ): Int = 0

            override suspend fun deleteMessage(messageId: String): Boolean {
                messages.find { it.id == messageId }?.let {
                    messages[messages.indexOf(it)] = it.copy(deleted = true)
                    return true
                }
                return false
            }

            override suspend fun getUnreadCount(userId: Int): Int = 0
        }
    }

    private fun createMockConversationRepository(): ConversationRepository {
        return object : ConversationRepository {
            private val conversations = mutableListOf<Conversation>()

            override suspend fun createConversation(participant1Id: Int, participant2Id: Int): Conversation {
                val (p1, p2) = if (participant1Id < participant2Id) {
                    participant1Id to participant2Id
                } else {
                    participant2Id to participant1Id
                }
                val conversation = Conversation(
                    id = "conv-${conversations.size + 1}",
                    participant1Id = p1,
                    participant2Id = p2,
                    lastMessageAt = Clock.System.now()
                )
                conversations.add(conversation)
                return conversation
            }

            override suspend fun getConversationById(id: String): Conversation? {
                return conversations.find { it.id == id }
            }

            override suspend fun findConversationByParticipants(userId1: Int, userId2: Int): Conversation? {
                val (p1, p2) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1
                return conversations.find { it.participant1Id == p1 && it.participant2Id == p2 }
            }

            override suspend fun getConversationsForUser(
                userId: Int,
                page: Int,
                pageSize: Int,
                includeArchived: Boolean
            ): Pair<List<Conversation>, Int> {
                val filtered = conversations.filter {
                    it.participant1Id == userId || it.participant2Id == userId
                }
                return Pair(filtered, filtered.size)
            }

            override suspend fun updateLastMessage(conversationId: String, timestamp: kotlinx.datetime.Instant): Boolean = true
            override suspend fun archiveConversation(conversationId: String, userId: Int): Boolean = true
            override suspend fun deleteConversation(conversationId: String): Boolean = true
        }
    }

    private fun createMockBlockedUserRepository(): BlockedUserRepository {
        return object : BlockedUserRepository {
            private val blocked = mutableSetOf<Pair<Int, Int>>()

            override suspend fun blockUser(blockerId: Int, blockedId: Int): BlockedUser {
                blocked.add(blockerId to blockedId)
                return BlockedUser(
                    id = "block-1",
                    blockerId = blockerId,
                    blockedId = blockedId,
                    createdAt = Clock.System.now()
                )
            }

            override suspend fun unblockUser(blockerId: Int, blockedId: Int): Boolean {
                return blocked.remove(blockerId to blockedId)
            }

            override suspend fun isBlocked(userId1: Int, userId2: Int): Boolean {
                return blocked.contains(userId1 to userId2) || blocked.contains(userId2 to userId1)
            }

            override suspend fun getBlockedUsers(userId: Int): List<BlockedUser> = emptyList()
        }
    }

    private fun createMockMessageReportRepository(): MessageReportRepository {
        return object : MessageReportRepository {
            override suspend fun createReport(messageId: String, reporterId: Int, reason: String): MessageReport {
                return MessageReport(
                    id = "report-1",
                    messageId = messageId,
                    reporterId = reporterId,
                    reason = reason,
                    status = ReportStatus.PENDING,
                    createdAt = Clock.System.now()
                )
            }

            override suspend fun getReportById(id: String): MessageReport? = null
            override suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean = true
            override suspend fun getReportsByStatus(status: ReportStatus): List<MessageReport> = emptyList()
            override suspend fun getReportsByMessage(messageId: String): List<MessageReport> = emptyList()
        }
    }

    @Test
    fun testSendMessage() = kotlinx.coroutines.runBlocking {
        val messagingService = MessagingServiceImpl(
            messageRepository = createMockMessageRepository(),
            conversationRepository = createMockConversationRepository(),
            blockedUserRepository = createMockBlockedUserRepository(),
            messageReportRepository = createMockMessageReportRepository(),
            userRepository = createMockUserRepository()
        )

        val response = messagingService.sendMessage(1, 2, "Hello, User 2!")

        assertNotNull(response)
        assertEquals("Hello, User 2!", response.message.content)
        assertEquals(1, response.message.senderId)
    }

    @Test
    fun testSendMessageValidationEmpty() = kotlinx.coroutines.runBlocking {
        val messagingService = MessagingServiceImpl(
            messageRepository = createMockMessageRepository(),
            conversationRepository = createMockConversationRepository(),
            blockedUserRepository = createMockBlockedUserRepository(),
            messageReportRepository = createMockMessageReportRepository(),
            userRepository = createMockUserRepository()
        )

        assertFailsWith<ValidationException> {
            messagingService.sendMessage(1, 2, "")
        }
    }

    @Test
    fun testSendMessageToSelf() = kotlinx.coroutines.runBlocking {
        val messagingService = MessagingServiceImpl(
            messageRepository = createMockMessageRepository(),
            conversationRepository = createMockConversationRepository(),
            blockedUserRepository = createMockBlockedUserRepository(),
            messageReportRepository = createMockMessageReportRepository(),
            userRepository = createMockUserRepository()
        )

        assertFailsWith<ValidationException> {
            messagingService.sendMessage(1, 1, "Message to myself")
        }
    }

    @Test
    fun testBlockUser() = kotlinx.coroutines.runBlocking {
        val messagingService = MessagingServiceImpl(
            messageRepository = createMockMessageRepository(),
            conversationRepository = createMockConversationRepository(),
            blockedUserRepository = createMockBlockedUserRepository(),
            messageReportRepository = createMockMessageReportRepository(),
            userRepository = createMockUserRepository()
        )

        val response = messagingService.blockUser(1, 2)

        assertTrue(response.success)
        assertEquals(2, response.blockedUserId)
    }

    @Test
    fun testReportMessage() = kotlinx.coroutines.runBlocking {
        val messageRepo = createMockMessageRepository()
        val conversationRepo = createMockConversationRepository()

        val messagingService = MessagingServiceImpl(
            messageRepository = messageRepo,
            conversationRepository = conversationRepo,
            blockedUserRepository = createMockBlockedUserRepository(),
            messageReportRepository = createMockMessageReportRepository(),
            userRepository = createMockUserRepository()
        )

        // First send a message
        val sentMessage = messagingService.sendMessage(1, 2, "Test message")

        // Then report it
        val reportResponse = messagingService.reportMessage(
            sentMessage.message.id,
            2,
            "Inappropriate content"
        )

        assertNotNull(reportResponse)
        assertEquals(ReportStatus.PENDING, reportResponse.status)
    }
}
