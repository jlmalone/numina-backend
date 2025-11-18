package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

@Serializable
data class DeviceToken(
    val id: String,
    val userId: Int,
    val platform: DevicePlatform,
    val token: String,
    val active: Boolean,
    val createdAt: Instant,
    val lastUsedAt: Instant
)

@Serializable
enum class DevicePlatform {
    ANDROID, IOS, WEB
}

@Serializable
data class NotificationPreferencesData(
    val id: String,
    val userId: Int,
    val messagesEnabled: Boolean = true,
    val matchesEnabled: Boolean = true,
    val groupsEnabled: Boolean = true,
    val classRemindersEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val emailFallback: Boolean = true,
    val quietHoursStart: String? = null, // HH:mm format
    val quietHoursEnd: String? = null, // HH:mm format
    val updatedAt: Instant
)

@Serializable
data class NotificationData(
    val id: String,
    val userId: Int,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val priority: NotificationPriority,
    val read: Boolean = false,
    val clicked: Boolean = false,
    val sentAt: Instant,
    val deliveredAt: Instant? = null,
    val readAt: Instant? = null,
    val createdAt: Instant
)

@Serializable
enum class NotificationType {
    MESSAGE, MATCH, GROUP, REMINDER, SOCIAL, SYSTEM
}

@Serializable
enum class NotificationPriority {
    URGENT, HIGH, NORMAL, LOW
}

@Serializable
enum class DeliveryStatus {
    PENDING, SENT, DELIVERED, FAILED, CLICKED
}

// Request DTOs
@Serializable
data class RegisterDeviceRequest(
    val platform: DevicePlatform,
    val token: String
)

@Serializable
data class UpdatePreferencesRequest(
    val messagesEnabled: Boolean? = null,
    val matchesEnabled: Boolean? = null,
    val groupsEnabled: Boolean? = null,
    val classRemindersEnabled: Boolean? = null,
    val socialEnabled: Boolean? = null,
    val emailFallback: Boolean? = null,
    val quietHoursStart: String? = null, // HH:mm format
    val quietHoursEnd: String? = null // HH:mm format
)

@Serializable
data class SendNotificationRequest(
    val userId: Int? = null,
    val userIds: List<Int>? = null,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

@Serializable
data class BroadcastNotificationRequest(
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

// Response DTOs
@Serializable
data class NotificationHistoryResponse(
    val notifications: List<NotificationData>,
    val page: Int,
    val pageSize: Int,
    val total: Long
)
