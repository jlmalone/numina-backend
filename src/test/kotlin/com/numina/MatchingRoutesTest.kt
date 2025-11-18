package com.numina

import com.numina.domain.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.Duration.Companion.days

class MatchingRoutesTest {
    private suspend fun registerAndGetToken(
        client: io.ktor.client.HttpClient,
        email: String,
        name: String = "Test User"
    ): Pair<String, Int> {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = email,
                password = "password123",
                name = name
            ))
        }
        val loginResponse = response.body<LoginResponse>()
        return Pair(loginResponse.token, loginResponse.user.id)
    }

    private suspend fun updateUserProfile(
        client: io.ktor.client.HttpClient,
        token: String,
        fitnessLevel: Int,
        fitnessInterests: List<String>,
        locationLat: Double,
        locationLong: Double
    ) {
        client.put("/api/v1/users/me") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(
                fitnessLevel = fitnessLevel,
                fitnessInterests = fitnessInterests,
                locationLat = locationLat,
                locationLong = locationLong,
                availability = mapOf(
                    "Monday" to listOf("morning", "evening"),
                    "Wednesday" to listOf("morning")
                )
            ))
        }
    }

    @Test
    fun testGetPartners() = testApplication {
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

        // Create two users with similar profiles
        val (token1, _) = registerAndGetToken(client, "user1@example.com", "User One")
        val (token2, _) = registerAndGetToken(client, "user2@example.com", "User Two")

        // Update profiles to be similar
        updateUserProfile(
            client, token1,
            fitnessLevel = 5,
            fitnessInterests = listOf("yoga", "running"),
            locationLat = 40.7128,
            locationLong = -74.0060
        )

        updateUserProfile(
            client, token2,
            fitnessLevel = 6,
            fitnessInterests = listOf("yoga", "cycling"),
            locationLat = 40.7580,
            locationLong = -73.9855
        )

        // Get partners for user1
        val response = client.get("/api/v1/matches/partners") {
            bearerAuth(token1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partners = response.body<List<UserMatch>>()
        assertTrue(partners.isNotEmpty(), "Should find at least one partner")
        assertTrue(partners.any { it.profile.name == "User Two" }, "Should include User Two")
    }

    @Test
    fun testGetPartnersWithFilters() = testApplication {
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

        val (token, _) = registerAndGetToken(client, "user@example.com")

        // Test with query parameters
        val response = client.get("/api/v1/matches/partners?limit=10&minScore=70&radius=5.0") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partners = response.body<List<UserMatch>>()
        assertTrue(partners.size <= 10, "Should respect limit")
        assertTrue(partners.all { it.matchScore >= 70 }, "All matches should meet minScore")
    }

    @Test
    fun testGetRecommendedClasses() = testApplication {
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

        // Create a user
        val (token, _) = registerAndGetToken(client, "classuser@example.com")
        updateUserProfile(
            client, token,
            fitnessLevel = 5,
            fitnessInterests = listOf("yoga", "pilates"),
            locationLat = 40.7128,
            locationLong = -74.0060
        )

        // Create a class
        val now = Clock.System.now()
        val classResponse = client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "Beginner Yoga",
                description = "Great for beginners",
                datetime = now + 1.days,
                locationLat = 40.7128,
                locationLong = -74.0060,
                trainer = "Jane Doe",
                intensity = 5,
                price = 20.0,
                capacity = 15,
                tags = listOf("yoga", "beginner")
            ))
        }
        assertEquals(HttpStatusCode.Created, classResponse.status)

        // Get recommended classes
        val response = client.get("/api/v1/matches/classes") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val classes = response.body<List<ClassMatch>>()
        assertTrue(classes.isNotEmpty(), "Should find at least one class match")
        assertTrue(classes.any { it.classDetails.name == "Beginner Yoga" })
    }

    @Test
    fun testRecordMatchAction() = testApplication {
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

        // Create two users
        val (token1, userId1) = registerAndGetToken(client, "match1@example.com", "Match User 1")
        val (token2, userId2) = registerAndGetToken(client, "match2@example.com", "Match User 2")

        // User 1 likes User 2
        val response1 = client.post("/api/v1/matches/action") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(
                targetUserId = userId2,
                action = MatchActionType.LIKE
            ))
        }

        assertEquals(HttpStatusCode.OK, response1.status)
        val actionResponse1 = response1.body<MatchActionResponse>()
        assertFalse(actionResponse1.mutual, "Should not be mutual yet")
        assertNull(actionResponse1.match, "Should not have match yet")

        // User 2 likes User 1 - should create mutual match
        val response2 = client.post("/api/v1/matches/action") {
            bearerAuth(token2)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(
                targetUserId = userId1,
                action = MatchActionType.LIKE
            ))
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val actionResponse2 = response2.body<MatchActionResponse>()
        assertTrue(actionResponse2.mutual, "Should be mutual now")
        assertNotNull(actionResponse2.match, "Should have match object")
    }

    @Test
    fun testGetMutualMatches() = testApplication {
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

        // Create two users and make them match
        val (token1, userId1) = registerAndGetToken(client, "mutual1@example.com")
        val (token2, userId2) = registerAndGetToken(client, "mutual2@example.com")

        // Both users like each other
        client.post("/api/v1/matches/action") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(targetUserId = userId2, action = MatchActionType.LIKE))
        }

        client.post("/api/v1/matches/action") {
            bearerAuth(token2)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(targetUserId = userId1, action = MatchActionType.LIKE))
        }

        // Get mutual matches for user 1
        val response = client.get("/api/v1/matches/mutual") {
            bearerAuth(token1)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val mutualMatches = response.body<List<MutualMatch>>()
        assertTrue(mutualMatches.isNotEmpty(), "Should have at least one mutual match")
    }

    @Test
    fun testMatchActionValidation() = testApplication {
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

        val (token, userId) = registerAndGetToken(client, "selfmatch@example.com")

        // Try to match with yourself - should fail
        val response = client.post("/api/v1/matches/action") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(
                targetUserId = userId,
                action = MatchActionType.LIKE
            ))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testPartnersQueryParameterValidation() = testApplication {
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

        val (token, _) = registerAndGetToken(client, "validate@example.com")

        // Test invalid limit
        val response1 = client.get("/api/v1/matches/partners?limit=200") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.BadRequest, response1.status)

        // Test invalid minScore
        val response2 = client.get("/api/v1/matches/partners?minScore=150") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.BadRequest, response2.status)

        // Test invalid radius
        val response3 = client.get("/api/v1/matches/partners?radius=-5") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.BadRequest, response3.status)
    }
}
