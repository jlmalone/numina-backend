package com.numina

import com.numina.common.models.ApiResponse
import com.numina.domain.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.Duration.Companion.days

class ReviewRoutesTest {
    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient, email: String): String {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = email,
                password = "password123",
                name = "Test User"
            ))
        }
        val loginResponse = response.body<LoginResponse>()
        return loginResponse.token
    }

    private suspend fun createTestClass(client: io.ktor.client.HttpClient, token: String): Int {
        val now = Clock.System.now()
        val classTime = now.plus(1.days)

        val response = client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "Test Yoga Class",
                description = "A test yoga class",
                datetime = classTime,
                locationLat = 40.7128,
                locationLong = -74.0060,
                trainer = "John Doe",
                intensity = 5,
                price = 25.0,
                capacity = 20,
                tags = listOf("yoga", "beginner")
            ))
        }
        val fitnessClass = response.body<FitnessClass>()
        return fitnessClass.id
    }

    @Test
    fun testCreateClassReview() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user1@example.com")
        val classId = createTestClass(client, token)

        val response = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 5,
                title = "Great class!",
                content = "I really enjoyed this yoga class. The instructor was excellent.",
                pros = "Great atmosphere",
                cons = null,
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val apiResponse = response.body<ApiResponse<Review>>()
        assertTrue(apiResponse.success)
        assertNotNull(apiResponse.data)
        assertEquals(5, apiResponse.data?.rating)
        assertEquals("Great class!", apiResponse.data?.title)
    }

    @Test
    fun testGetClassReviews() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user2@example.com")
        val classId = createTestClass(client, token)

        // Create a review
        client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 4,
                title = "Good class",
                content = "Nice experience overall",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }

        // Get reviews
        val response = client.get("/api/v1/reviews/classes/$classId")

        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<List<Review>>>()
        assertTrue(apiResponse.success)
        assertNotNull(apiResponse.data)
        assertEquals(1, apiResponse.data?.size)
    }

    @Test
    fun testUpdateReview() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user3@example.com")
        val classId = createTestClass(client, token)

        // Create a review
        val createResponse = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 3,
                title = "Okay class",
                content = "It was okay",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }
        val createApiResponse = createResponse.body<ApiResponse<Review>>()
        val reviewId = createApiResponse.data!!.id

        // Update the review
        val updateResponse = client.put("/api/v1/reviews/$reviewId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateReviewRequest(
                rating = 5,
                title = "Actually, great class!",
                content = "Changed my mind, this was excellent!"
            ))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updateApiResponse = updateResponse.body<ApiResponse<Review>>()
        assertTrue(updateApiResponse.success)
        assertEquals(5, updateApiResponse.data?.rating)
        assertEquals("Actually, great class!", updateApiResponse.data?.title)
    }

    @Test
    fun testDeleteReview() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user4@example.com")
        val classId = createTestClass(client, token)

        // Create a review
        val createResponse = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 4,
                title = "Good",
                content = "Nice class",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }
        val createApiResponse = createResponse.body<ApiResponse<Review>>()
        val reviewId = createApiResponse.data!!.id

        // Delete the review
        val deleteResponse = client.delete("/api/v1/reviews/$reviewId") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, deleteResponse.status)
    }

    @Test
    fun testVoteHelpful() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token1 = registerAndGetToken(client, "user5@example.com")
        val token2 = registerAndGetToken(client, "user6@example.com")
        val classId = createTestClass(client, token1)

        // User 1 creates a review
        val createResponse = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 5,
                title = "Excellent",
                content = "Highly recommended",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }
        val createApiResponse = createResponse.body<ApiResponse<Review>>()
        val reviewId = createApiResponse.data!!.id

        // User 2 votes helpful
        val voteResponse = client.post("/api/v1/reviews/$reviewId/helpful") {
            bearerAuth(token2)
        }

        assertEquals(HttpStatusCode.OK, voteResponse.status)
        val voteApiResponse = voteResponse.body<ApiResponse<Map<String, Boolean>>>()
        assertTrue(voteApiResponse.success)
    }

    @Test
    fun testReportReview() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token1 = registerAndGetToken(client, "user7@example.com")
        val token2 = registerAndGetToken(client, "user8@example.com")
        val classId = createTestClass(client, token1)

        // User 1 creates a review
        val createResponse = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 1,
                title = "Terrible",
                content = "Worst class ever",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }
        val createApiResponse = createResponse.body<ApiResponse<Review>>()
        val reviewId = createApiResponse.data!!.id

        // User 2 reports the review
        val reportResponse = client.post("/api/v1/reviews/$reviewId/report") {
            bearerAuth(token2)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewReportRequest(
                reason = "Inappropriate language"
            ))
        }

        assertEquals(HttpStatusCode.Created, reportResponse.status)
        val reportApiResponse = reportResponse.body<ApiResponse<ReviewReport>>()
        assertTrue(reportApiResponse.success)
        assertEquals("Inappropriate language", reportApiResponse.data?.reason)
    }

    @Test
    fun testGetMyReviews() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user9@example.com")
        val classId = createTestClass(client, token)

        // Create a review
        client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 4,
                title = "Great",
                content = "Really enjoyed it",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }

        // Get my reviews
        val response = client.get("/api/v1/reviews/my-reviews") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<List<Review>>>()
        assertTrue(apiResponse.success)
        assertNotNull(apiResponse.data)
        assertEquals(1, apiResponse.data?.size)
    }

    @Test
    fun testGetRatingSummaryForClass() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token1 = registerAndGetToken(client, "user10@example.com")
        val token2 = registerAndGetToken(client, "user11@example.com")
        val classId = createTestClass(client, token1)

        // Create two reviews
        client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 5,
                title = "Excellent",
                content = "Loved it",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }

        client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token2)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 4,
                title = "Good",
                content = "Nice class",
                attendedOn = LocalDate(2024, 1, 16)
            ))
        }

        // Get rating summary
        val response = client.get("/api/v1/ratings/classes/$classId")

        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<RatingSummary>>()
        assertTrue(apiResponse.success)
        assertNotNull(apiResponse.data)
        assertEquals(2, apiResponse.data?.totalReviews)
        assertTrue(apiResponse.data!!.averageRating >= 4.0)
    }

    @Test
    fun testDuplicateReviewPrevention() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user12@example.com")
        val classId = createTestClass(client, token)

        // Create first review
        val response1 = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 5,
                title = "Great",
                content = "Amazing class",
                attendedOn = LocalDate(2024, 1, 15)
            ))
        }
        assertEquals(HttpStatusCode.Created, response1.status)

        // Try to create duplicate review
        val response2 = client.post("/api/v1/reviews/classes/$classId") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateReviewRequest(
                rating = 4,
                title = "Good",
                content = "Nice class",
                attendedOn = LocalDate(2024, 1, 16)
            ))
        }

        // Should fail with bad request or conflict
        assertTrue(response2.status.value >= 400)
    }
}
