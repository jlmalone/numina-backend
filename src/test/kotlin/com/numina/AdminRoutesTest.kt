package com.numina

import com.numina.auth.AdminJwtConfig
import com.numina.domain.*
import com.numina.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AdminRoutesTest {
    private fun createAdminToken(): String {
        return AdminJwtConfig.generateToken(
            userId = 1,
            email = "admin@test.com",
            role = "SUPER_ADMIN"
        )
    }

    @Test
    fun `test admin authentication required`() = testApplication {
        application {
            configureKoin()
            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/api/v1/admin/users")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `test get feature flags with admin auth`() = testApplication {
        application {
            configureKoin()
            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val token = createAdminToken()

        val response = client.get("/api/v1/admin/feature-flags") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `test create feature flag`() = testApplication {
        application {
            configureKoin()
            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val token = createAdminToken()

        val request = CreateFeatureFlagRequest(
            name = "test_feature",
            enabled = true,
            description = "Test feature flag",
            rolloutPercentage = 100
        )

        val response = client.post("/api/v1/admin/feature-flags") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(kotlinx.serialization.serializer(), request))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `test get analytics with admin auth`() = testApplication {
        application {
            configureKoin()
            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val token = createAdminToken()

        val userMetrics = client.get("/api/v1/admin/analytics/users") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, userMetrics.status)

        val classMetrics = client.get("/api/v1/admin/analytics/classes") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, classMetrics.status)

        val engagementMetrics = client.get("/api/v1/admin/analytics/engagement") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, engagementMetrics.status)
    }

    @Test
    fun `test get audit logs with admin auth`() = testApplication {
        application {
            configureKoin()
            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val token = createAdminToken()

        val response = client.get("/api/v1/admin/audit-logs") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
