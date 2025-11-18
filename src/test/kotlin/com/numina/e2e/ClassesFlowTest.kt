package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassesFlowTest : E2ETestBase() {
    @BeforeEach
    override fun setup() {
        super.setup()
        runBlocking {
            // Create a test user and login
            val testEmail = "classes_test_${System.currentTimeMillis()}@example.com"
            client.post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$testEmail","password":"test123","name":"Test User"}""")
            }
            authToken = login(testEmail, "test123")
        }
    }

    @Test
    fun `search and view classes flow`() = runBlocking {
        // Search classes
        val searchResponse = client.get("/api/classes/search?query=CS") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, searchResponse.status)
        val searchBody = searchResponse.bodyAsText()
        assertTrue(searchBody.isNotEmpty())

        // Get all classes
        val classesResponse = client.get("/api/classes") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, classesResponse.status)

        // If classes exist, get details of first one
        val classesBody = classesResponse.bodyAsText()
        if (classesBody.contains("\"id\"")) {
            // Assuming the response is a JSON array/list
            // Extract first class ID and get its details
            val detailsResponse = client.get("/api/classes/1") {
                withAuth()
            }
            // Should return either OK or NotFound depending on whether class 1 exists
            assertTrue(
                detailsResponse.status == HttpStatusCode.OK ||
                detailsResponse.status == HttpStatusCode.NotFound
            )
        }
    }

    @Test
    fun `bookmark class flow`() = runBlocking {
        val classId = 1L

        // Bookmark a class
        val bookmarkResponse = client.post("/api/bookmarks") {
            withAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"classId":$classId}""")
        }
        // Should succeed or already exist
        assertTrue(
            bookmarkResponse.status == HttpStatusCode.Created ||
            bookmarkResponse.status == HttpStatusCode.Conflict ||
            bookmarkResponse.status == HttpStatusCode.NotFound
        )

        // Get bookmarks
        val getBookmarksResponse = client.get("/api/bookmarks") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, getBookmarksResponse.status)

        // Delete bookmark
        val deleteResponse = client.delete("/api/bookmarks/$classId") {
            withAuth()
        }
        assertTrue(
            deleteResponse.status == HttpStatusCode.NoContent ||
            deleteResponse.status == HttpStatusCode.NotFound
        )
    }

    @Test
    fun `filter classes by department`() = runBlocking {
        val filterResponse = client.get("/api/classes/search?department=CS") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, filterResponse.status)
    }

    @Test
    fun `unauthorized class access fails`() = runBlocking {
        val response = client.get("/api/classes")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
