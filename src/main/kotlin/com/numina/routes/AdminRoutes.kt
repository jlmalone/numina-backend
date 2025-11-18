package com.numina.routes

import com.numina.domain.*
import com.numina.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminRoutes() {
    val adminUserService by inject<AdminUserService>()
    val moderationService by inject<ModerationService>()
    val adminAnalyticsService by inject<AdminAnalyticsService>()
    val featureFlagService by inject<FeatureFlagService>()
    val auditLogService by inject<AuditLogService>()

    authenticate("admin-jwt") {
        route("/admin") {
            // User Management
            route("/users") {
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                    val response = adminUserService.listUsers(page, pageSize)
                    call.respond(HttpStatusCode.OK, response)
                }

                get("/search") {
                    val query = call.request.queryParameters["q"] ?: ""
                    val users = adminUserService.searchUsers(query)
                    call.respond(HttpStatusCode.OK, users)
                }

                get("/{id}") {
                    val userId = call.parameters["id"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val userDetails = adminUserService.getUserDetails(userId)
                    call.respond(HttpStatusCode.OK, userDetails)
                }

                post("/{id}/suspend") {
                    val principal = call.principal<JWTPrincipal>()
                    val adminUserId = principal!!.payload.getClaim("userId").asInt()
                    val ipAddress = call.request.origin.remoteHost

                    val userId = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val request = call.receive<SuspendUserRequest>()
                    val success = adminUserService.suspendUser(userId, request, adminUserId, auditLogService, ipAddress)

                    call.respond(HttpStatusCode.OK, mapOf("success" to success))
                }

                post("/{id}/unsuspend") {
                    val principal = call.principal<JWTPrincipal>()
                    val adminUserId = principal!!.payload.getClaim("userId").asInt()
                    val ipAddress = call.request.origin.remoteHost

                    val userId = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val success = adminUserService.unsuspendUser(userId, adminUserId, auditLogService, ipAddress)
                    call.respond(HttpStatusCode.OK, mapOf("success" to success))
                }

                post("/{id}/reset-password") {
                    val principal = call.principal<JWTPrincipal>()
                    val adminUserId = principal!!.payload.getClaim("userId").asInt()
                    val ipAddress = call.request.origin.remoteHost

                    val userId = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                    val request = call.receive<ResetPasswordRequest>()
                    val success = adminUserService.resetUserPassword(userId, request, adminUserId, auditLogService, ipAddress)

                    call.respond(HttpStatusCode.OK, mapOf("success" to success))
                }
            }

            // Content Moderation
            route("/moderation") {
                get("/queue") {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val queue = moderationService.getFlaggedContentQueue(limit)
                    call.respond(HttpStatusCode.OK, mapOf("queue" to queue))
                }

                route("/reviews") {
                    post("/{id}/approve") {
                        val principal = call.principal<JWTPrincipal>()
                        val adminUserId = principal!!.payload.getClaim("userId").asInt()
                        val ipAddress = call.request.origin.remoteHost

                        val reviewId = call.parameters["id"]
                            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid review ID"))

                        val success = moderationService.approveReview(reviewId, adminUserId, auditLogService, ipAddress)
                        call.respond(HttpStatusCode.OK, mapOf("success" to success))
                    }

                    post("/{id}/reject") {
                        val principal = call.principal<JWTPrincipal>()
                        val adminUserId = principal!!.payload.getClaim("userId").asInt()
                        val ipAddress = call.request.origin.remoteHost

                        val reviewId = call.parameters["id"]
                            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid review ID"))

                        val success = moderationService.rejectReview(reviewId, adminUserId, auditLogService, ipAddress)
                        call.respond(HttpStatusCode.OK, mapOf("success" to success))
                    }
                }

                route("/messages") {
                    post("/{id}/delete") {
                        val principal = call.principal<JWTPrincipal>()
                        val adminUserId = principal!!.payload.getClaim("userId").asInt()
                        val ipAddress = call.request.origin.remoteHost

                        val messageId = call.parameters["id"]
                            ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid message ID"))

                        val success = moderationService.deleteMessage(messageId, adminUserId, auditLogService, ipAddress)
                        call.respond(HttpStatusCode.OK, mapOf("success" to success))
                    }
                }
            }

            // Analytics
            route("/analytics") {
                get("/users") {
                    val metrics = adminAnalyticsService.getUserMetrics()
                    call.respond(HttpStatusCode.OK, metrics)
                }

                get("/classes") {
                    val metrics = adminAnalyticsService.getClassMetrics()
                    call.respond(HttpStatusCode.OK, metrics)
                }

                get("/engagement") {
                    val metrics = adminAnalyticsService.getEngagementMetrics()
                    call.respond(HttpStatusCode.OK, metrics)
                }

                get("/export") {
                    val format = call.request.queryParameters["format"] ?: "json"
                    val data = adminAnalyticsService.exportData(format)

                    when (format.lowercase()) {
                        "csv" -> {
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    "analytics-export.csv"
                                ).toString()
                            )
                            call.respondText(data, ContentType.Text.CSV)
                        }
                        else -> {
                            call.response.header(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Attachment.withParameter(
                                    ContentDisposition.Parameters.FileName,
                                    "analytics-export.json"
                                ).toString()
                            )
                            call.respondText(data, ContentType.Application.Json)
                        }
                    }
                }
            }

            // Feature Flags
            route("/feature-flags") {
                get {
                    val flags = featureFlagService.getAllFlags()
                    call.respond(HttpStatusCode.OK, flags)
                }

                post {
                    val request = call.receive<CreateFeatureFlagRequest>()
                    val flag = featureFlagService.createFlag(request)
                    call.respond(HttpStatusCode.Created, flag)
                }

                put("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid flag ID"))

                    val request = call.receive<UpdateFeatureFlagRequest>()
                    val flag = featureFlagService.updateFlag(id, request)
                    call.respond(HttpStatusCode.OK, flag)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid flag ID"))

                    val success = featureFlagService.deleteFlag(id)
                    call.respond(HttpStatusCode.OK, mapOf("success" to success))
                }
            }

            // Audit Logs
            route("/audit-logs") {
                get {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val logs = auditLogService.getAllLogs(limit)
                    call.respond(HttpStatusCode.OK, logs)
                }

                get("/admin/{adminUserId}") {
                    val adminUserId = call.parameters["adminUserId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid admin user ID"))

                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val logs = auditLogService.getLogsByAdmin(adminUserId, limit)
                    call.respond(HttpStatusCode.OK, logs)
                }

                get("/entity/{entityType}/{entityId}") {
                    val entityType = call.parameters["entityType"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid entity type"))
                    val entityId = call.parameters["entityId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid entity ID"))

                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val logs = auditLogService.getLogsByEntity(entityType, entityId, limit)
                    call.respond(HttpStatusCode.OK, logs)
                }
            }

            // System notifications (placeholder)
            route("/notifications") {
                post("/broadcast") {
                    val principal = call.principal<JWTPrincipal>()
                    val adminUserId = principal!!.payload.getClaim("userId").asInt()
                    val ipAddress = call.request.origin.remoteHost

                    val request = call.receive<BroadcastNotificationRequest>()

                    // Log the broadcast action
                    auditLogService.logAction(
                        adminUserId = adminUserId,
                        action = "BROADCAST_NOTIFICATION",
                        entityType = "NOTIFICATION",
                        entityId = null,
                        changes = mapOf(
                            "title" to request.title,
                            "targetUsers" to (request.targetUserIds?.joinToString(",") ?: "all")
                        ),
                        ipAddress = ipAddress
                    )

                    // Placeholder - would implement actual notification sending
                    call.respond(HttpStatusCode.OK, mapOf("success" to true, "message" to "Broadcast scheduled"))
                }
            }
        }
    }
}
