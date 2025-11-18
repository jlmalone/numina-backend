package com.numina.services

import com.numina.data.repositories.NotificationRepository
import com.numina.domain.*
import kotlinx.datetime.*
import org.slf4j.LoggerFactory

interface NotificationService {
    suspend fun registerDevice(userId: Int, platform: DevicePlatform, token: String): DeviceToken?
    suspend fun removeDevice(tokenId: String, userId: Int): Boolean

    suspend fun getUserPreferences(userId: Int): NotificationPreferencesData
    suspend fun updatePreferences(userId: Int, preferences: UpdatePreferencesRequest): NotificationPreferencesData?

    suspend fun sendNotification(
        userId: Int,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): NotificationData?

    suspend fun sendToMultipleUsers(
        userIds: List<Int>,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): List<NotificationData>

    suspend fun broadcastNotification(
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): Int

    suspend fun getNotificationHistory(userId: Int, page: Int, pageSize: Int): NotificationHistoryResponse
    suspend fun markNotificationAsRead(notificationId: String, userId: Int): Boolean
    suspend fun deleteNotification(notificationId: String, userId: Int): Boolean

    suspend fun processBatchNotifications()
}

class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val fcmService: FCMService
) : NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationServiceImpl::class.java)

    override suspend fun registerDevice(userId: Int, platform: DevicePlatform, token: String): DeviceToken? {
        return notificationRepository.registerDevice(userId, platform, token)
    }

    override suspend fun removeDevice(tokenId: String, userId: Int): Boolean {
        return notificationRepository.removeDevice(tokenId, userId)
    }

    override suspend fun getUserPreferences(userId: Int): NotificationPreferencesData {
        return notificationRepository.getOrCreatePreferences(userId)
    }

    override suspend fun updatePreferences(userId: Int, preferences: UpdatePreferencesRequest): NotificationPreferencesData? {
        return notificationRepository.updatePreferences(userId, preferences)
    }

    override suspend fun sendNotification(
        userId: Int,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
        priority: NotificationPriority
    ): NotificationData? {
        // Check user preferences
        val preferences = notificationRepository.getOrCreatePreferences(userId)
        if (!shouldSendNotification(preferences, type)) {
            logger.info("User $userId has disabled notifications of type $type")
            return null
        }

        // Check quiet hours
        if (isInQuietHours(preferences)) {
            logger.info("User $userId is in quiet hours. Notification will be stored but not pushed.")
        }

        // Create notification record
        val notification = notificationRepository.createNotification(userId, type, title, body, data, priority)
            ?: return null

        // Send push notification if not in quiet hours
        if (!isInQuietHours(preferences)) {
            sendPushNotificationToUser(userId, notification)
        }

        return notification
    }

    override suspend fun sendToMultipleUsers(
        userIds: List<Int>,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
        priority: NotificationPriority
    ): List<NotificationData> {
        val notifications = mutableListOf<NotificationData>()

        for (userId in userIds) {
            val notification = sendNotification(userId, type, title, body, data, priority)
            if (notification != null) {
                notifications.add(notification)
            }
        }

        return notifications
    }

    override suspend fun broadcastNotification(
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
        priority: NotificationPriority
    ): Int {
        // In a real implementation, you would:
        // 1. Get all user IDs from the database
        // 2. Send to all users in batches
        // 3. Or use FCM topics for efficient broadcasting

        logger.info("Broadcasting notification of type $type to all users")

        // For now, we'll use FCM topics
        // In production, you'd create a notification record for each user
        // This is a simplified implementation
        val notification = NotificationData(
            id = "",
            userId = 0,
            type = type,
            title = title,
            body = body,
            data = data,
            priority = priority,
            sentAt = Clock.System.now(),
            createdAt = Clock.System.now()
        )

        val success = fcmService.sendToTopic("all_users", notification)
        return if (success) 1 else 0
    }

    override suspend fun getNotificationHistory(userId: Int, page: Int, pageSize: Int): NotificationHistoryResponse {
        val (notifications, total) = notificationRepository.getUserNotifications(userId, page, pageSize)
        return NotificationHistoryResponse(
            notifications = notifications,
            page = page,
            pageSize = pageSize,
            total = total
        )
    }

    override suspend fun markNotificationAsRead(notificationId: String, userId: Int): Boolean {
        return notificationRepository.markNotificationAsRead(notificationId, userId)
    }

    override suspend fun deleteNotification(notificationId: String, userId: Int): Boolean {
        return notificationRepository.deleteNotification(notificationId, userId)
    }

    private suspend fun sendPushNotificationToUser(userId: Int, notification: NotificationData) {
        try {
            val deviceTokens = notificationRepository.getActiveUserDeviceTokens(userId)
            if (deviceTokens.isEmpty()) {
                logger.info("No active device tokens for user $userId")
                return
            }

            val results = fcmService.sendPushNotification(deviceTokens, notification)

            // Log delivery status for each device
            for ((tokenId, success) in results) {
                val status = if (success) DeliveryStatus.SENT else DeliveryStatus.FAILED
                val errorMessage = if (!success) "Failed to send FCM message" else null

                notificationRepository.createDeliveryLog(
                    notificationId = notification.id,
                    deviceTokenId = tokenId,
                    status = status,
                    errorMessage = errorMessage
                )

                // Deactivate invalid tokens
                if (!success) {
                    logger.info("Deactivating token $tokenId due to delivery failure")
                    notificationRepository.deactivateDevice(tokenId)
                }
            }

            // Update notification delivered status if at least one succeeded
            if (results.values.any { it }) {
                logger.info("Notification ${notification.id} delivered to at least one device")
            }
        } catch (e: Exception) {
            logger.error("Error sending push notification to user $userId", e)
        }
    }

    private fun shouldSendNotification(preferences: NotificationPreferencesData, type: NotificationType): Boolean {
        return when (type) {
            NotificationType.MESSAGE -> preferences.messagesEnabled
            NotificationType.MATCH -> preferences.matchesEnabled
            NotificationType.GROUP -> preferences.groupsEnabled
            NotificationType.REMINDER -> preferences.classRemindersEnabled
            NotificationType.SOCIAL -> preferences.socialEnabled
            NotificationType.SYSTEM -> true // Always send system notifications
        }
    }

    private fun isInQuietHours(preferences: NotificationPreferencesData): Boolean {
        val start = preferences.quietHoursStart
        val end = preferences.quietHoursEnd

        if (start == null || end == null) {
            return false
        }

        try {
            val startTime = LocalTime.parse(start)
            val endTime = LocalTime.parse(end)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time

            return if (startTime < endTime) {
                // Normal range (e.g., 22:00 to 08:00 next day)
                now >= startTime && now <= endTime
            } else {
                // Wraps around midnight (e.g., 22:00 to 08:00)
                now >= startTime || now <= endTime
            }
        } catch (e: Exception) {
            logger.error("Error parsing quiet hours", e)
            return false
        }
    }

    override suspend fun processBatchNotifications() {
        // Process pending notifications in batches for efficiency
        logger.info("Processing batch notifications...")
        // In a production system, this would:
        // 1. Query for pending notifications from a queue
        // 2. Group them by user/device
        // 3. Send them in batches to FCM
        // For now, this is a placeholder for the background job
        logger.info("Batch notification processing completed")
    }
}
