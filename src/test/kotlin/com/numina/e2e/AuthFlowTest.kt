package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthFlowTest : E2ETestBase() {
    @Test
    fun `complete authentication flow`() = runBlocking {
        val testEmail = "test_${System.currentTimeMillis()}@example.com"

        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$testEmail","password":"test123","name":"Test User"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        // Login
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$testEmail","password":"test123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        // Get profile
        authToken = extractToken(loginResponse.bodyAsText())
        val profileResponse = client.get("/api/users/me") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, profileResponse.status)

        val profileBody = profileResponse.bodyAsText()
        assertTrue(profileBody.contains(testEmail))
    }

    @Test
    fun `invalid credentials fail`() = runBlocking {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"nonexistent@example.com","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `registration with existing email fails`() = runBlocking {
        val testEmail = "duplicate_${System.currentTimeMillis()}@example.com"

        // First registration
        val firstResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$testEmail","password":"test123","name":"Test User"}""")
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        // Second registration with same email
        val secondResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$testEmail","password":"test456","name":"Another User"}""")
        }
        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
    }

    @Test
    fun `unauthorized access without token fails`() = runBlocking {
        val response = client.get("/api/users/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
