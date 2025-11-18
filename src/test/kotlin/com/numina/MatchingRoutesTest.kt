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

    @Test
    fun testGetPartnerMatches() = testApplication {
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

        val token = registerAndGetToken(client, "match1@example.com")

        val response = client.get("/api/v1/matches/partners") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val matches = response.body<List<UserMatch>>()
        assertNotNull(matches)
    }

    @Test
    fun testGetClassMatches() = testApplication {
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

        val token = registerAndGetToken(client, "match2@example.com")

        val response = client.get("/api/v1/matches/classes") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val matches = response.body<List<ClassMatch>>()
        assertNotNull(matches)
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

        val token = registerAndGetToken(client, "match3@example.com")

        val response = client.get("/api/v1/matches/mutual") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val matches = response.body<List<MutualMatch>>()
        assertNotNull(matches)
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
        val token1Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "user1@example.com",
                password = "password123",
                name = "User One"
            ))
        }
        val loginResponse1 = token1Response.body<LoginResponse>()
        val token1 = loginResponse1.token
        val user1Id = loginResponse1.user.id

        val token2Response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "user2@example.com",
                password = "password123",
                name = "User Two"
            ))
        }
        val loginResponse2 = token2Response.body<LoginResponse>()
        val user2Id = loginResponse2.user.id

        // User 1 likes User 2
        val response = client.post("/api/v1/matches/action") {
            bearerAuth(token1)
            contentType(ContentType.Application.Json)
            setBody(MatchActionRequest(
                targetUserId = user2Id,
                action = MatchAction.LIKE
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val actionResponse = response.body<MatchActionResponse>()
        assertNotNull(actionResponse)
        assertFalse(actionResponse.mutual) // Not mutual yet
    }
}
