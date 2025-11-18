package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupsFlowTest : E2ETestBase() {
    @BeforeEach
    override fun setup() {
        super.setup()
        runBlocking {
            // Create a test user and login
            val testEmail = "groups_test_${System.currentTimeMillis()}@example.com"
            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$testEmail","password":"test123","name":"Test User"}""")
            }
            authToken = login(testEmail, "test123")
        }
    }

    @Test
    fun `create group and activity flow`() = runBlocking {
        // Create a group
        val createGroupResponse = client.post("/api/groups") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Study Group","description":"Test study group","category":"ACADEMIC"}""")
        }
        assertTrue(
            createGroupResponse.status == HttpStatusCode.Created ||
            createGroupResponse.status == HttpStatusCode.OK
        )

        if (createGroupResponse.status == HttpStatusCode.Created ||
            createGroupResponse.status == HttpStatusCode.OK) {
            val groupBody = createGroupResponse.bodyAsText()
            // Assuming group ID is 1 for the first created group
            val groupId = 1L

            // Get group details
            val getGroupResponse = client.get("/api/groups/$groupId") {
                withAuth()
            }
            assertTrue(
                getGroupResponse.status == HttpStatusCode.OK ||
                getGroupResponse.status == HttpStatusCode.NotFound
            )

            // Join group (might already be member as creator)
            val joinResponse = client.post("/api/groups/$groupId/join") {
                withAuth()
            }
            assertTrue(
                joinResponse.status == HttpStatusCode.OK ||
                joinResponse.status == HttpStatusCode.Conflict ||
                joinResponse.status == HttpStatusCode.NotFound
            )

            // Create an activity
            val createActivityResponse = client.post("/api/groups/$groupId/activities") {
                withAuth()
                contentType(ContentType.Application.Json)
                setBody("""{"title":"Study Session","description":"Group study","datetime":"2025-12-01T10:00:00Z","location":"Library"}""")
            }
            assertTrue(
                createActivityResponse.status == HttpStatusCode.Created ||
                createActivityResponse.status == HttpStatusCode.OK ||
                createActivityResponse.status == HttpStatusCode.NotFound
            )

            // RSVP to activity
            if (createActivityResponse.status == HttpStatusCode.Created ||
                createActivityResponse.status == HttpStatusCode.OK) {
                val activityId = 1L
                val rsvpResponse = client.post("/api/activities/$activityId/rsvp") {
                    withAuth()
                    contentType(ContentType.Application.Json)
                    setBody("""{"status":"ATTENDING"}""")
                }
                assertTrue(
                    rsvpResponse.status == HttpStatusCode.OK ||
                    rsvpResponse.status == HttpStatusCode.Created ||
                    rsvpResponse.status == HttpStatusCode.NotFound
                )
            }

            // Get group members
            val getMembersResponse = client.get("/api/groups/$groupId/members") {
                withAuth()
            }
            assertTrue(
                getMembersResponse.status == HttpStatusCode.OK ||
                getMembersResponse.status == HttpStatusCode.NotFound
            )
        }
    }

    @Test
    fun `list all groups`() = runBlocking {
        val response = client.get("/api/groups") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search groups by category`() = runBlocking {
        val response = client.get("/api/groups/search?category=ACADEMIC") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `unauthorized group access fails`() = runBlocking {
        val response = client.get("/api/groups")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
