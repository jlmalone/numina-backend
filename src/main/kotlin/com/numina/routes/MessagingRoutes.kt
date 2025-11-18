package com.numina.routes

import com.numina.auth.JwtConfig
import com.numina.common.exceptions.UnauthorizedException
import com.numina.messaging.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MessagingRoutes")

fun Route.messagingRoutes() {
    val messagingService by inject<MessagingService>()
    val webSocketManager by inject<WebSocketManager>()

    route("/messages") {
        authenticate("auth-jwt") {
            // Send a message
            post("/send") {
                val principal = call.principal<JWTPrincipal>()
                val senderId = principal!!.payload.getClaim("userId").asInt()

                val request = call.receive<SendMessageRequest>()
                val response = messagingService.sendMessage(
                    senderId = senderId,
                    recipientId = request.recipientId,
                    content = request.content
                )

                // Notify recipient via WebSocket if they're online
                if (webSocketManager.isUserOnline(request.recipientId)) {
                    webSocketManager.notifyNewMessage(request.recipientId, response.message)
                }

                call.respond(HttpStatusCode.Created, response)
            }

            // Get all conversations for current user
            get("/conversations") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                val response = messagingService.getConversations(userId, page, pageSize)
                call.respond(HttpStatusCode.OK, response)
            }

            // Get messages in a conversation
            get("/conversations/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val conversationId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Conversation ID is required")

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                val response = messagingService.getMessages(conversationId, userId, page, pageSize)
                call.respond(HttpStatusCode.OK, response)
            }

            // Mark conversation messages as read
            post("/conversations/{id}/mark-read") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val conversationId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Conversation ID is required")

                val success = messagingService.markAsRead(conversationId, userId)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("success" to success, "conversationId" to conversationId)
                )
            }

            // Delete a message
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val messageId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Message ID is required")

                val success = messagingService.deleteMessage(messageId, userId)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("success" to success, "messageId" to messageId)
                )
            }

            // Block a user
            post("/block/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val blockerId = principal!!.payload.getClaim("userId").asInt()

                val blockedId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                val response = messagingService.blockUser(blockerId, blockedId)
                call.respond(HttpStatusCode.OK, response)
            }

            // Unblock a user
            delete("/block/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val blockerId = principal!!.payload.getClaim("userId").asInt()

                val blockedId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                val success = messagingService.unblockUser(blockerId, blockedId)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("success" to success, "unblockedUserId" to blockedId)
                )
            }

            // Report a message
            post("/report/{messageId}") {
                val principal = call.principal<JWTPrincipal>()
                val reporterId = principal!!.payload.getClaim("userId").asInt()

                val messageId = call.parameters["messageId"]
                    ?: throw IllegalArgumentException("Message ID is required")

                val request = call.receive<ReportMessageRequest>()
                val response = messagingService.reportMessage(messageId, reporterId, request.reason)
                call.respond(HttpStatusCode.Created, response)
            }

            // Get unread message count
            get("/unread-count") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val response = messagingService.getUnreadCount(userId)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}

/**
 * WebSocket endpoint for real-time messaging
 */
fun Route.messagingWebSocket() {
    val webSocketManager by inject<WebSocketManager>()

    webSocket("/ws/messages") {
        var userId: Int? = null

        try {
            // Extract JWT token from query parameters or initial message
            val token = call.request.queryParameters["token"]
            if (token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing authentication token"))
                return@webSocket
            }

            // Verify JWT token
            try {
                val verifier = JwtConfig.verifier
                val decodedJWT = verifier.verify(token)
                userId = decodedJWT.getClaim("userId").asInt()

                if (userId == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                    return@webSocket
                }
            } catch (e: Exception) {
                logger.error("JWT verification failed", e)
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication failed"))
                return@webSocket
            }

            // Register connection
            webSocketManager.connect(userId, this)
            logger.info("WebSocket connection established for user $userId")

            // Send connection confirmation
            send(Frame.Text("""{"type":"connected","userId":$userId}"""))

            // Listen for incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("Received from user $userId: $text")
                        // Handle typing indicators, read receipts, etc.
                        // This is a placeholder for future WebSocket message handling
                    }
                    is Frame.Close -> {
                        logger.info("User $userId closed WebSocket connection")
                    }
                    else -> {
                        // Ignore binary frames, ping/pong handled automatically
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("WebSocket channel closed for user $userId")
        } catch (e: Exception) {
            logger.error("WebSocket error for user $userId", e)
        } finally {
            // Clean up connection
            if (userId != null) {
                webSocketManager.disconnect(userId)
            }
        }
    }
}
