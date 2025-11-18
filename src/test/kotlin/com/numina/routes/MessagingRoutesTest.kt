package com.numina.routes

import com.numina.domain.LoginResponse
import com.numina.domain.RegisterRequest
import com.numina.messaging.SendMessageRequest
import com.numina.messaging.SendMessageResponse
import com.numina.messaging.ConversationListResponse
import com.numina.messaging.UnreadCountResponse
import com.numina.messaging.ReportMessageRequest
import com.numina.messaging.ReportMessageResponse
import com.numina.module
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MessagingRoutesTest {

    private suspend fun registerAndGetToken(
        client: HttpClient,
        email: String,
        password: String = "password123"
    ): String {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = email, password = password, name = "Test User"))
        }
        val loginResponse = response.body<LoginResponse>()
        return loginResponse.token
    }

    @Test
    fun testSendMessage() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "user1@test.com")
        val token2Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = "user2@test.com", password = "password123", name = "User 2"))
        }
        val token2Data = token2Response.body<LoginResponse>()
        val user2Id = token2Data.user.id

        // Send message from user1 to user2
        val sendResponse = client.post("/api/v1/messages/send") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(recipientId = user2Id, content = "Hello, User 2!"))
        }

        assertEquals(HttpStatusCode.Created, sendResponse.status)
        val messageResponse = sendResponse.body<SendMessageResponse>()
        assertEquals("Hello, User 2!", messageResponse.message.content)
        assertNotNull(messageResponse.conversationId)
    }

    @Test
    fun testGetConversations() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "conv1@test.com")
        val token2Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = "conv2@test.com", password = "password123", name = "User 2"))
        }
        val token2Data = token2Response.body<LoginResponse>()
        val user2Id = token2Data.user.id

        // Send a message to create conversation
        client.post("/api/v1/messages/send") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(recipientId = user2Id, content = "Test message"))
        }

        // Get conversations for user1
        val conversationsResponse = client.get("/api/v1/messages/conversations") {
            bearerAuth(token1)
        }

        assertEquals(HttpStatusCode.OK, conversationsResponse.status)
        val conversations = conversationsResponse.body<ConversationListResponse>()
        assertTrue(conversations.conversations.isNotEmpty())
        assertEquals(1, conversations.total)
    }

    @Test
    fun testGetUnreadCount() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val token = registerAndGetToken(client, "unread@test.com")

        val response = client.get("/api/v1/messages/unread-count") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val unreadCount = response.body<UnreadCountResponse>()
        assertEquals(0, unreadCount.count) // No messages yet
    }

    @Test
    fun testBlockUser() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "blocker@test.com")
        val token2Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = "blocked@test.com", password = "password123", name = "Blocked User"))
        }
        val token2Data = token2Response.body<LoginResponse>()
        val user2Id = token2Data.user.id

        // Block user2
        val blockResponse = client.post("/api/v1/messages/block/$user2Id") {
            bearerAuth(token1)
        }

        assertEquals(HttpStatusCode.OK, blockResponse.status)
    }

    @Test
    fun testSendMessageUnauthorized() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val sendResponse = client.post("/api/v1/messages/send") {
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(recipientId = 2, content = "Test"))
        }

        assertEquals(HttpStatusCode.Unauthorized, sendResponse.status)
    }

    @Test
    fun testReportMessage() = testApplication {
        application { module() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "reporter@test.com")
        val token2Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email = "reported@test.com", password = "password123", name = "Reported User"))
        }
        val token2Data = token2Response.body<LoginResponse>()
        val token2 = token2Data.token
        val user1Response = client.get("/api/v1/users/me") {
            bearerAuth(token1)
        }

        // User2 sends a message
        val sendResponse = client.post("/api/v1/messages/send") {
            bearerAuth(token2)
            contentType(ContentType.Application.Json)
            setBody(SendMessageRequest(
                recipientId = user1Response.body<com.numina.domain.UserProfile>().userId,
                content = "Inappropriate message"
            ))
        }
        val messageData = sendResponse.body<SendMessageResponse>()

        // User1 reports the message
        val reportResponse = client.post("/api/v1/messages/report/${messageData.message.id}") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(ReportMessageRequest(reason = "Spam"))
        }

        assertEquals(HttpStatusCode.Created, reportResponse.status)
        val report = reportResponse.body<ReportMessageResponse>()
        assertNotNull(report.reportId)
    }
}
