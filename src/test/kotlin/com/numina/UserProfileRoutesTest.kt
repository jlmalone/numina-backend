package com.numina

import com.numina.domain.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class UserProfileRoutesTest {
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
    fun testGetMyProfile() = testApplication {
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

        val token = registerAndGetToken(client, "profile@example.com")

        val response = client.get("/api/v1/users/me") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<UserProfile>()
        assertEquals("Test User", profile.name)
    }

    @Test
    fun testUpdateProfile() = testApplication {
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

        val token = registerAndGetToken(client, "update@example.com")

        val response = client.put("/api/v1/users/me") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(
                bio = "Fitness enthusiast",
                fitnessLevel = 7,
                fitnessInterests = listOf("yoga", "running", "cycling")
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<UserProfile>()
        assertEquals("Fitness enthusiast", profile.bio)
        assertEquals(7, profile.fitnessLevel)
        assertTrue(profile.fitnessInterests.contains("yoga"))
    }

    @Test
    fun testUnauthorizedAccess() = testApplication {
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

        val response = client.get("/api/v1/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
