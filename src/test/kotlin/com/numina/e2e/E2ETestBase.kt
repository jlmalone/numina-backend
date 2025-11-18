package com.numina.e2e

import com.numina.module
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class E2ETestBase {
    protected lateinit var client: HttpClient
    protected var authToken: String? = null

    @BeforeEach
    fun setup() = testApplication {
        application { module() }

        client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @AfterEach
    fun teardown() {
        client.close()
    }

    protected suspend fun login(email: String, password: String): String {
        val response = client.post("/api/auth/login") {
            header("Content-Type", "application/json")
            setBody("""{"email":"$email","password":"$password"}""")
        }
        return extractToken(response.bodyAsText())
    }

    protected fun extractToken(responseBody: String): String {
        val json = Json.parseToJsonElement(responseBody)
        return json.jsonObject["token"]?.jsonPrimitive?.content ?: ""
    }

    protected fun HttpRequestBuilder.withAuth() {
        authToken?.let { header("Authorization", "Bearer $it") }
    }
}
