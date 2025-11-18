package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object NotificationDeliveryLog : UUIDTable("notification_delivery_log") {
    val notificationId = reference("notification_id", Notifications)
    val deviceTokenId = reference("device_token_id", DeviceTokens)
    val status = varchar("status", 50) // pending, sent, delivered, failed, clicked
    val errorMessage = text("error_message").nullable()
    val attemptedAt = timestamp("attempted_at")
}
