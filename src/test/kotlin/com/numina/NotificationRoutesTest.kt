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

class NotificationRoutesTest {

    private suspend fun registerAndLogin(client: io.ktor.client.HttpClient): String {
        // Register a user
        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "notification-test-${System.currentTimeMillis()}@example.com",
                password = "password123",
                name = "Test User"
            ))
        }
        val loginResponse = registerResponse.body<LoginResponse>()
        return loginResponse.token
    }

    @Test
    fun testRegisterDevice() = testApplication {
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

        val token = registerAndLogin(client)

        val response = client.post("/api/v1/notifications/register-device") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(RegisterDeviceRequest(
                platform = DevicePlatform.ANDROID,
                token = "test-fcm-token-${System.currentTimeMillis()}"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val deviceToken = response.body<DeviceToken>()
        assertNotNull(deviceToken.id)
        assertEquals(DevicePlatform.ANDROID, deviceToken.platform)
    }

    @Test
    fun testGetNotificationPreferences() = testApplication {
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

        val token = registerAndLogin(client)

        val response = client.get("/api/v1/notifications/preferences") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val preferences = response.body<NotificationPreferencesData>()
        assertNotNull(preferences.id)
        assertTrue(preferences.messagesEnabled)
        assertTrue(preferences.matchesEnabled)
    }

    @Test
    fun testUpdateNotificationPreferences() = testApplication {
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

        val token = registerAndLogin(client)

        val response = client.put("/api/v1/notifications/preferences") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdatePreferencesRequest(
                messagesEnabled = false,
                quietHoursStart = "22:00",
                quietHoursEnd = "08:00"
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val preferences = response.body<NotificationPreferencesData>()
        assertFalse(preferences.messagesEnabled)
        assertEquals("22:00", preferences.quietHoursStart)
        assertEquals("08:00", preferences.quietHoursEnd)
    }

    @Test
    fun testGetNotificationHistory() = testApplication {
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

        val token = registerAndLogin(client)

        val response = client.get("/api/v1/notifications/history?page=1&pageSize=10") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val history = response.body<NotificationHistoryResponse>()
        assertNotNull(history.notifications)
        assertEquals(1, history.page)
        assertEquals(10, history.pageSize)
    }

    @Test
    fun testSendNotificationToUser() = testApplication {
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

        // Register and get user ID
        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "recipient-${System.currentTimeMillis()}@example.com",
                password = "password123",
                name = "Recipient User"
            ))
        }
        val loginResponse = registerResponse.body<LoginResponse>()
        val recipientUserId = loginResponse.user.id
        val adminToken = loginResponse.token

        // Send notification as admin
        val response = client.post("/api/v1/admin/notifications/send") {
            contentType(ContentType.Application.Json)
            bearerAuth(adminToken)
            setBody(SendNotificationRequest(
                userId = recipientUserId,
                type = NotificationType.SYSTEM,
                title = "Test Notification",
                body = "This is a test notification",
                priority = NotificationPriority.NORMAL
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val notification = response.body<NotificationData>()
        assertNotNull(notification.id)
        assertEquals("Test Notification", notification.title)
        assertEquals(NotificationType.SYSTEM, notification.type)
    }

    @Test
    fun testMarkNotificationAsRead() = testApplication {
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

        val token = registerAndLogin(client)

        // Get login response to retrieve user ID
        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "test-read-${System.currentTimeMillis()}@example.com",
                password = "password123",
                name = "Test Read User"
            ))
        }
        val loginResponse = registerResponse.body<LoginResponse>()
        val userId = loginResponse.user.id
        val userToken = loginResponse.token

        // Create a notification
        val notificationResponse = client.post("/api/v1/admin/notifications/send") {
            contentType(ContentType.Application.Json)
            bearerAuth(userToken)
            setBody(SendNotificationRequest(
                userId = userId,
                type = NotificationType.SYSTEM,
                title = "Test Read Notification",
                body = "This notification will be marked as read",
                priority = NotificationPriority.NORMAL
            ))
        }
        val notification = notificationResponse.body<NotificationData>()

        // Mark as read
        val response = client.post("/api/v1/notifications/${notification.id}/mark-read") {
            bearerAuth(userToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testDeleteNotification() = testApplication {
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

        // Register and get user ID
        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = "test-delete-${System.currentTimeMillis()}@example.com",
                password = "password123",
                name = "Test Delete User"
            ))
        }
        val loginResponse = registerResponse.body<LoginResponse>()
        val userId = loginResponse.user.id
        val userToken = loginResponse.token

        // Create a notification
        val notificationResponse = client.post("/api/v1/admin/notifications/send") {
            contentType(ContentType.Application.Json)
            bearerAuth(userToken)
            setBody(SendNotificationRequest(
                userId = userId,
                type = NotificationType.SYSTEM,
                title = "Test Delete Notification",
                body = "This notification will be deleted",
                priority = NotificationPriority.NORMAL
            ))
        }
        val notification = notificationResponse.body<NotificationData>()

        // Delete notification
        val response = client.delete("/api/v1/notifications/${notification.id}") {
            bearerAuth(userToken)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testBroadcastNotification() = testApplication {
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

        val token = registerAndLogin(client)

        val response = client.post("/api/v1/admin/notifications/broadcast") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(BroadcastNotificationRequest(
                type = NotificationType.SYSTEM,
                title = "System Announcement",
                body = "This is a system-wide announcement",
                priority = NotificationPriority.HIGH
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun testUnauthorizedAccessToNotifications() = testApplication {
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

        val response = client.get("/api/v1/notifications/preferences")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
