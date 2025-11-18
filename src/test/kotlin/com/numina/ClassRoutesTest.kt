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

class ClassRoutesTest {
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
    fun testCreateClass() = testApplication {
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

        val token = registerAndGetToken(client, "admin@example.com")

        val now = Clock.System.now()
        val classTime = now.plus(1.days)

        val response = client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "Morning Yoga",
                description = "Relaxing yoga session",
                datetime = classTime,
                locationLat = 40.7128,
                locationLong = -74.0060,
                trainer = "Jane Doe",
                intensity = 5,
                price = 25.0,
                capacity = 20,
                tags = listOf("yoga", "morning", "beginner")
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val fitnessClass = response.body<FitnessClass>()
        assertEquals("Morning Yoga", fitnessClass.name)
        assertEquals(5, fitnessClass.intensity)
    }

    @Test
    fun testGetClasses() = testApplication {
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

        val token = registerAndGetToken(client, "user@example.com")
        val now = Clock.System.now()

        // Create a class
        client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "HIIT Workout",
                description = "High intensity interval training",
                datetime = now.plus(1.days),
                locationLat = 40.7128,
                locationLong = -74.0060,
                intensity = 9,
                price = 30.0,
                capacity = 15,
                tags = listOf("hiit", "cardio")
            ))
        }

        // Get all classes
        val response = client.get("/api/v1/classes")

        assertEquals(HttpStatusCode.OK, response.status)
        val classes = response.body<List<FitnessClass>>()
        assertTrue(classes.isNotEmpty())
    }

    @Test
    fun testGetClassById() = testApplication {
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

        val token = registerAndGetToken(client, "getbyid@example.com")
        val now = Clock.System.now()

        // Create a class
        val createResponse = client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "Pilates",
                description = "Core strengthening",
                datetime = now.plus(1.days),
                locationLat = 40.7128,
                locationLong = -74.0060,
                intensity = 6,
                price = 28.0,
                capacity = 12,
                tags = listOf("pilates")
            ))
        }
        val createdClass = createResponse.body<FitnessClass>()

        // Get the class by ID
        val response = client.get("/api/v1/classes/${createdClass.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        val fitnessClass = response.body<FitnessClass>()
        assertEquals("Pilates", fitnessClass.name)
    }
}
