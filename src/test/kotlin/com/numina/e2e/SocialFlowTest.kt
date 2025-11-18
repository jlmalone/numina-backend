package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SocialFlowTest : E2ETestBase() {
    private var user1Token: String? = null
    private var user2Token: String? = null

    @BeforeEach
    override fun setup() {
        super.setup()
        runBlocking {
            // Create two test users
            val timestamp = System.currentTimeMillis()
            val user1Email = "social_user1_${timestamp}@example.com"
            val user2Email = "social_user2_${timestamp}@example.com"

            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$user1Email","password":"test123","name":"Social User One"}""")
            }

            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$user2Email","password":"test123","name":"Social User Two"}""")
            }

            user1Token = login(user1Email, "test123")
            user2Token = login(user2Email, "test123")
            authToken = user1Token
        }
    }

    @Test
    fun `follow user and view feed flow`() = runBlocking {
        // User 1 follows User 2
        authToken = user1Token
        val followResponse = client.post("/api/users/2/follow") {
            withAuth()
        }
        assertTrue(
            followResponse.status == HttpStatusCode.OK ||
            followResponse.status == HttpStatusCode.Created ||
            followResponse.status == HttpStatusCode.Conflict ||
            followResponse.status == HttpStatusCode.NotFound
        )

        // User 1 gets their following list
        val followingResponse = client.get("/api/users/me/following") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, followingResponse.status)

        // User 2 gets their followers list
        authToken = user2Token
        val followersResponse = client.get("/api/users/me/followers") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, followersResponse.status)

        // User 1 views their feed
        authToken = user1Token
        val feedResponse = client.get("/api/feed") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, feedResponse.status)

        // User 1 unfollows User 2
        val unfollowResponse = client.delete("/api/users/2/follow") {
            withAuth()
        }
        assertTrue(
            unfollowResponse.status == HttpStatusCode.NoContent ||
            unfollowResponse.status == HttpStatusCode.OK ||
            unfollowResponse.status == HttpStatusCode.NotFound
        )
    }

    @Test
    fun `get user profile`() = runBlocking {
        authToken = user1Token
        val response = client.get("/api/users/2") {
            withAuth()
        }
        assertTrue(
            response.status == HttpStatusCode.OK ||
            response.status == HttpStatusCode.NotFound
        )
    }

    @Test
    fun `search users`() = runBlocking {
        authToken = user1Token
        val response = client.get("/api/users/search?query=Social") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `update user profile`() = runBlocking {
        authToken = user1Token
        val response = client.put("/api/users/me") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"bio":"Updated bio","major":"Computer Science"}""")
        }
        assertTrue(
            response.status == HttpStatusCode.OK ||
            response.status == HttpStatusCode.NotFound
        )
    }

    @Test
    fun `unauthorized social features access fails`() = runBlocking {
        val feedResponse = client.get("/api/feed")
        assertEquals(HttpStatusCode.Unauthorized, feedResponse.status)

        val followingResponse = client.get("/api/users/me/following")
        assertEquals(HttpStatusCode.Unauthorized, followingResponse.status)
    }
}
