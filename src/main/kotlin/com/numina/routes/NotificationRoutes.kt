package com.numina.routes

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.BadRequestException
import com.numina.domain.*
import com.numina.services.NotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.notificationRoutes() {
    val notificationService by inject<NotificationService>()

    route("/api/v1/notifications") {
        // Public/authenticated routes
        authenticate("auth-jwt") {
            // Device Registration
            post("/register-device") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<RegisterDeviceRequest>()

                val deviceToken = notificationService.registerDevice(userId, request.platform, request.token)
                    ?: throw BadRequestException("Failed to register device token")

                call.respond(HttpStatusCode.Created, deviceToken)
            }

            delete("/device/{tokenId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val tokenId = call.parameters["tokenId"]
                    ?: throw BadRequestException("Token ID is required")

                val success = notificationService.removeDevice(tokenId, userId)
                if (!success) {
                    throw NotFoundException("Device token not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Device token removed successfully"))
            }

            // Preferences
            get("/preferences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val preferences = notificationService.getUserPreferences(userId)
                call.respond(HttpStatusCode.OK, preferences)
            }

            put("/preferences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<UpdatePreferencesRequest>()

                val preferences = notificationService.updatePreferences(userId, request)
                    ?: throw BadRequestException("Failed to update preferences")

                call.respond(HttpStatusCode.OK, preferences)
            }

            // Notification History
            get("/history") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                if (page < 1 || pageSize < 1 || pageSize > 100) {
                    throw BadRequestException("Invalid pagination parameters")
                }

                val history = notificationService.getNotificationHistory(userId, page, pageSize)
                call.respond(HttpStatusCode.OK, history)
            }

            post("/{id}/mark-read") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val notificationId = call.parameters["id"]
                    ?: throw BadRequestException("Notification ID is required")

                val success = notificationService.markNotificationAsRead(notificationId, userId)
                if (!success) {
                    throw NotFoundException("Notification not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Notification marked as read"))
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val notificationId = call.parameters["id"]
                    ?: throw BadRequestException("Notification ID is required")

                val success = notificationService.deleteNotification(notificationId, userId)
                if (!success) {
                    throw NotFoundException("Notification not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Notification deleted successfully"))
            }
        }
    }

    // Admin routes
    route("/api/v1/admin/notifications") {
        authenticate("auth-jwt") {
            post("/send") {
                // In production, you'd want additional admin role verification
                val request = call.receive<SendNotificationRequest>()

                when {
                    request.userId != null -> {
                        // Send to single user
                        val notification = notificationService.sendNotification(
                            userId = request.userId,
                            type = request.type,
                            title = request.title,
                            body = request.body,
                            data = request.data,
                            priority = request.priority
                        ) ?: throw BadRequestException("Failed to send notification")

                        call.respond(HttpStatusCode.Created, notification)
                    }
                    request.userIds != null && request.userIds.isNotEmpty() -> {
                        // Send to multiple users
                        val notifications = notificationService.sendToMultipleUsers(
                            userIds = request.userIds,
                            type = request.type,
                            title = request.title,
                            body = request.body,
                            data = request.data,
                            priority = request.priority
                        )

                        call.respond(
                            HttpStatusCode.Created,
                            mapOf(
                                "sent" to notifications.size,
                                "notifications" to notifications
                            )
                        )
                    }
                    else -> {
                        throw BadRequestException("Either userId or userIds must be provided")
                    }
                }
            }

            post("/broadcast") {
                // In production, you'd want additional admin role verification
                val request = call.receive<BroadcastNotificationRequest>()

                val count = notificationService.broadcastNotification(
                    type = request.type,
                    title = request.title,
                    body = request.body,
                    data = request.data,
                    priority = request.priority
                )

                call.respond(
                    HttpStatusCode.Created,
                    mapOf(
                        "message" to "Broadcast sent successfully",
                        "recipients" to count
                    )
                )
            }
        }
    }
}
