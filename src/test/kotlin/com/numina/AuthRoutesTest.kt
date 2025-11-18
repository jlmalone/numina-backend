package com.numina

import com.numina.domain.LoginRequest
import com.numina.domain.LoginResponse
import com.numina.domain.RegisterRequest
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class AuthRoutesTest {
    @Test
    fun testRegisterUser() = testApplication {
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

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "test@example.com",
                password = "password123",
                name = "Test User"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val loginResponse = response.body<LoginResponse>()
        assertNotNull(loginResponse.token)
        assertNotNull(loginResponse.refreshToken)
        assertEquals("test@example.com", loginResponse.user.email)
    }

    @Test
    fun testRegisterDuplicateEmail() = testApplication {
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

        // Register first user
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "duplicate@example.com",
                password = "password123",
                name = "First User"
            ))
        }

        // Try to register with same email
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "duplicate@example.com",
                password = "password456",
                name = "Second User"
            ))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun testLogin() = testApplication {
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

        // Register user
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "login@example.com",
                password = "password123",
                name = "Login User"
            ))
        }

        // Login
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(
                email = "login@example.com",
                password = "password123"
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val loginResponse = response.body<LoginResponse>()
        assertNotNull(loginResponse.token)
        assertNotNull(loginResponse.refreshToken)
    }

    @Test
    fun testLoginInvalidCredentials() = testApplication {
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

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(
                email = "nonexistent@example.com",
                password = "wrongpassword"
            ))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
