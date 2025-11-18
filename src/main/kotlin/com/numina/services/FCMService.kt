package com.numina.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.numina.domain.DeviceToken
import com.numina.domain.NotificationData
import com.numina.domain.NotificationPriority
import io.ktor.server.application.*
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.InputStream

interface FCMService {
    fun initialize(serviceAccountPath: String?)
    suspend fun sendPushNotification(deviceTokens: List<DeviceToken>, notification: NotificationData): Map<String, Boolean>
    suspend fun sendToTopic(topic: String, notification: NotificationData): Boolean
}

class FCMServiceImpl : FCMService {
    private val logger = LoggerFactory.getLogger(FCMServiceImpl::class.java)
    private var firebaseMessaging: FirebaseMessaging? = null
    private var isInitialized = false

    override fun initialize(serviceAccountPath: String?) {
        try {
            if (serviceAccountPath.isNullOrBlank()) {
                logger.warn("Firebase service account path not configured. Push notifications will be disabled.")
                return
            }

            val serviceAccount: InputStream = try {
                FileInputStream(serviceAccountPath)
            } catch (e: Exception) {
                logger.warn("Firebase service account file not found at: $serviceAccountPath. Push notifications will be disabled.")
                return
            }

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }

            firebaseMessaging = FirebaseMessaging.getInstance()
            isInitialized = true
            logger.info("Firebase Cloud Messaging initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Cloud Messaging", e)
            isInitialized = false
        }
    }

    override suspend fun sendPushNotification(
        deviceTokens: List<DeviceToken>,
        notification: NotificationData
    ): Map<String, Boolean> {
        if (!isInitialized || firebaseMessaging == null) {
            logger.warn("FCM not initialized. Skipping push notification.")
            return deviceTokens.associate { it.id to false }
        }

        val results = mutableMapOf<String, Boolean>()

        for (deviceToken in deviceTokens) {
            try {
                val message = buildMessage(deviceToken.token, notification)
                val response = firebaseMessaging!!.send(message)
                logger.info("Successfully sent FCM message to token ${deviceToken.id}: $response")
                results[deviceToken.id] = true
            } catch (e: FirebaseMessagingException) {
                logger.error("Failed to send FCM message to token ${deviceToken.id}", e)
                results[deviceToken.id] = false

                // Handle invalid tokens
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.info("Token ${deviceToken.id} is invalid or unregistered. Should be deactivated.")
                    }
                    else -> {
                        logger.error("FCM error code: ${e.messagingErrorCode}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected error sending FCM message to token ${deviceToken.id}", e)
                results[deviceToken.id] = false
            }
        }

        return results
    }

    override suspend fun sendToTopic(topic: String, notification: NotificationData): Boolean {
        if (!isInitialized || firebaseMessaging == null) {
            logger.warn("FCM not initialized. Skipping topic notification.")
            return false
        }

        return try {
            val message = Message.builder()
                .setTopic(topic)
                .setNotification(
                    Notification.builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build()
                )
                .putAllData(notification.data)
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(mapPriorityToAndroid(notification.priority))
                        .build()
                )
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(
                            Aps.builder()
                                .setContentAvailable(true)
                                .setBadge(1)
                                .build()
                        )
                        .build()
                )
                .build()

            val response = firebaseMessaging!!.send(message)
            logger.info("Successfully sent FCM message to topic $topic: $response")
            true
        } catch (e: Exception) {
            logger.error("Failed to send FCM message to topic $topic", e)
            false
        }
    }

    private fun buildMessage(token: String, notification: NotificationData): Message {
        return Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(notification.title)
                    .setBody(notification.body)
                    .build()
            )
            .putAllData(notification.data + mapOf(
                "notificationId" to notification.id,
                "type" to notification.type.name.lowercase()
            ))
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(mapPriorityToAndroid(notification.priority))
                    .setNotification(
                        AndroidNotification.builder()
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build()
                    )
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setContentAvailable(true)
                            .setBadge(1)
                            .setSound("default")
                            .build()
                    )
                    .putHeader("apns-priority", if (notification.priority == NotificationPriority.URGENT) "10" else "5")
                    .build()
            )
            .setWebpushConfig(
                WebpushConfig.builder()
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle(notification.title)
                            .setBody(notification.body)
                            .setIcon("/icon.png")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun mapPriorityToAndroid(priority: NotificationPriority): AndroidConfig.Priority {
        return when (priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH -> AndroidConfig.Priority.HIGH
            else -> AndroidConfig.Priority.NORMAL
        }
    }
}
