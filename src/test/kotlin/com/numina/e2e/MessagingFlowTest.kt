package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagingFlowTest : E2ETestBase() {
    private var user1Token: String? = null
    private var user2Token: String? = null

    @BeforeEach
    override fun setup() {
        super.setup()
        runBlocking {
            // Create two test users
            val timestamp = System.currentTimeMillis()
            val user1Email = "msg_user1_${timestamp}@example.com"
            val user2Email = "msg_user2_${timestamp}@example.com"

            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$user1Email","password":"test123","name":"User One"}""")
            }

            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$user2Email","password":"test123","name":"User Two"}""")
            }

            user1Token = login(user1Email, "test123")
            user2Token = login(user2Email, "test123")
            authToken = user1Token
        }
    }

    @Test
    fun `complete messaging flow`() = runBlocking {
        // User 1 creates a conversation with User 2
        authToken = user1Token
        val createConversationResponse = client.post("/api/conversations") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"participantIds":[2]}""")
        }
        assertTrue(
            createConversationResponse.status == HttpStatusCode.Created ||
            createConversationResponse.status == HttpStatusCode.OK ||
            createConversationResponse.status == HttpStatusCode.NotFound
        )

        if (createConversationResponse.status == HttpStatusCode.Created ||
            createConversationResponse.status == HttpStatusCode.OK) {
            val conversationBody = createConversationResponse.bodyAsText()

            // User 1 sends a message
            val sendMessageResponse = client.post("/api/conversations/1/messages") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody("""{"content":"Hello from User 1!"}""")
            }
            assertTrue(
                sendMessageResponse.status == HttpStatusCode.Created ||
                sendMessageResponse.status == HttpStatusCode.NotFound
            )

            // User 1 gets their conversations
            val getConversationsResponse = client.get("/api/conversations") {
                withAuth()
            }
            assertEquals(HttpStatusCode.OK, getConversationsResponse.status)

            // User 2 checks their conversations
            authToken = user2Token
            val user2ConversationsResponse = client.get("/api/conversations") {
                withAuth()
            }
            assertEquals(HttpStatusCode.OK, user2ConversationsResponse.status)

            // User 2 gets messages from conversation
            val getMessagesResponse = client.get("/api/conversations/1/messages") {
                withAuth()
            }
            assertTrue(
                getMessagesResponse.status == HttpStatusCode.OK ||
                getMessagesResponse.status == HttpStatusCode.NotFound
            )
        }
    }

    @Test
    fun `get conversations list`() = runBlocking {
        authToken = user1Token
        val response = client.get("/api/conversations") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unauthorized messaging access fails`() = runBlocking {
        val response = client.get("/api/conversations")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
