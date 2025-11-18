package com.numina.messaging

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebSocket connections for real-time messaging
 */
class WebSocketManager {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    // Map of userId to WebSocket session
    private val connections = ConcurrentHashMap<Int, DefaultWebSocketSession>()
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Register a new WebSocket connection for a user
     */
    suspend fun connect(userId: Int, session: DefaultWebSocketSession) {
        mutex.withLock {
            connections[userId] = session
            logger.info("User $userId connected to WebSocket")
        }

        // Notify other users that this user is online
        broadcastUserStatus(userId, true)
    }

    /**
     * Remove a WebSocket connection when user disconnects
     */
    suspend fun disconnect(userId: Int) {
        mutex.withLock {
            connections.remove(userId)
            logger.info("User $userId disconnected from WebSocket")
        }

        // Notify other users that this user is offline
        broadcastUserStatus(userId, false)
    }

    /**
     * Send a message to a specific user
     */
    suspend fun sendToUser(userId: Int, message: WebSocketMessage) {
        val session = connections[userId]
        if (session != null && !session.outgoing.isClosedForSend) {
            try {
                val messageJson = json.encodeToString(message)
                session.send(Frame.Text(messageJson))
                logger.debug("Sent message to user $userId: ${message::class.simpleName}")
            } catch (e: Exception) {
                logger.error("Error sending message to user $userId", e)
                disconnect(userId)
            }
        }
    }

    /**
     * Broadcast a message to all connected users
     */
    suspend fun broadcast(message: WebSocketMessage) {
        connections.forEach { (userId, session) ->
            try {
                if (!session.outgoing.isClosedForSend) {
                    val messageJson = json.encodeToString(message)
                    session.send(Frame.Text(messageJson))
                }
            } catch (e: Exception) {
                logger.error("Error broadcasting to user $userId", e)
            }
        }
    }

    /**
     * Broadcast user online/offline status
     */
    private suspend fun broadcastUserStatus(userId: Int, online: Boolean) {
        val now = Clock.System.now()
        val statusMessage = WebSocketMessage.UserOnlineStatus(
            userId = userId,
            online = online,
            lastSeen = if (online) null else now
        )
        broadcast(statusMessage)
    }

    /**
     * Send a new message notification to a user
     */
    suspend fun notifyNewMessage(recipientId: Int, message: Message) {
        sendToUser(recipientId, WebSocketMessage.NewMessage(message))
    }

    /**
     * Notify that a message was delivered
     */
    suspend fun notifyMessageDelivered(userId: Int, messageId: String) {
        val now = Clock.System.now()
        sendToUser(userId, WebSocketMessage.MessageDelivered(messageId, now))
    }

    /**
     * Notify that a message was read
     */
    suspend fun notifyMessageRead(userId: Int, messageId: String) {
        val now = Clock.System.now()
        sendToUser(userId, WebSocketMessage.MessageRead(messageId, now))
    }

    /**
     * Send typing indicator
     */
    suspend fun sendTypingIndicator(conversationId: String, userId: Int, recipientId: Int, typing: Boolean) {
        val indicator = WebSocketMessage.TypingIndicator(conversationId, userId, typing)
        sendToUser(recipientId, indicator)
    }

    /**
     * Check if a user is currently connected
     */
    fun isUserOnline(userId: Int): Boolean {
        return connections.containsKey(userId)
    }

    /**
     * Get count of currently connected users
     */
    fun getConnectionCount(): Int {
        return connections.size
    }

    /**
     * Send error message to a user
     */
    suspend fun sendError(userId: Int, errorMessage: String, errorCode: String) {
        sendToUser(userId, WebSocketMessage.Error(errorMessage, errorCode))
    }
}
