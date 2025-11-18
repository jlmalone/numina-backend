package com.numina.data.repositories

import com.numina.data.tables.*
import com.numina.domain.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface NotificationRepository {
    suspend fun registerDevice(userId: Int, platform: DevicePlatform, token: String): DeviceToken?
    suspend fun getDeviceToken(tokenId: String): DeviceToken?
    suspend fun getUserDeviceTokens(userId: Int): List<DeviceToken>
    suspend fun getActiveUserDeviceTokens(userId: Int): List<DeviceToken>
    suspend fun updateDeviceLastUsed(tokenId: String, lastUsedAt: Instant)
    suspend fun deactivateDevice(tokenId: String)
    suspend fun removeDevice(tokenId: String, userId: Int): Boolean

    suspend fun getOrCreatePreferences(userId: Int): NotificationPreferencesData
    suspend fun updatePreferences(userId: Int, preferences: UpdatePreferencesRequest): NotificationPreferencesData?

    suspend fun createNotification(
        userId: Int,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
        priority: NotificationPriority
    ): NotificationData?

    suspend fun getNotificationById(id: String): NotificationData?
    suspend fun getUserNotifications(userId: Int, page: Int, pageSize: Int): Pair<List<NotificationData>, Long>
    suspend fun markNotificationAsRead(id: String, userId: Int): Boolean
    suspend fun markNotificationAsClicked(id: String): Boolean
    suspend fun deleteNotification(id: String, userId: Int): Boolean

    suspend fun createDeliveryLog(
        notificationId: String,
        deviceTokenId: String,
        status: DeliveryStatus,
        errorMessage: String? = null
    ): Boolean

    suspend fun updateDeliveryStatus(
        notificationId: String,
        deviceTokenId: String,
        status: DeliveryStatus,
        errorMessage: String? = null
    ): Boolean
}

class NotificationRepositoryImpl : NotificationRepository {

    private fun resultRowToDeviceToken(row: ResultRow): DeviceToken {
        return DeviceToken(
            id = row[DeviceTokens.id].value.toString(),
            userId = row[DeviceTokens.userId].value,
            platform = DevicePlatform.valueOf(row[DeviceTokens.platform].uppercase()),
            token = row[DeviceTokens.token],
            active = row[DeviceTokens.active],
            createdAt = row[DeviceTokens.createdAt],
            lastUsedAt = row[DeviceTokens.lastUsedAt]
        )
    }

    private fun resultRowToPreferences(row: ResultRow): NotificationPreferencesData {
        return NotificationPreferencesData(
            id = row[NotificationPreferences.id].value.toString(),
            userId = row[NotificationPreferences.userId].value,
            messagesEnabled = row[NotificationPreferences.messagesEnabled],
            matchesEnabled = row[NotificationPreferences.matchesEnabled],
            groupsEnabled = row[NotificationPreferences.groupsEnabled],
            classRemindersEnabled = row[NotificationPreferences.classRemindersEnabled],
            socialEnabled = row[NotificationPreferences.socialEnabled],
            emailFallback = row[NotificationPreferences.emailFallback],
            quietHoursStart = row[NotificationPreferences.quietHoursStart]?.toString(),
            quietHoursEnd = row[NotificationPreferences.quietHoursEnd]?.toString(),
            updatedAt = row[NotificationPreferences.updatedAt]
        )
    }

    private fun resultRowToNotification(row: ResultRow): NotificationData {
        return NotificationData(
            id = row[Notifications.id].value.toString(),
            userId = row[Notifications.userId].value,
            type = NotificationType.valueOf(row[Notifications.type].uppercase()),
            title = row[Notifications.title],
            body = row[Notifications.body],
            data = row[Notifications.data],
            priority = NotificationPriority.valueOf(row[Notifications.priority].uppercase()),
            read = row[Notifications.read],
            clicked = row[Notifications.clicked],
            sentAt = row[Notifications.sentAt],
            deliveredAt = row[Notifications.deliveredAt],
            readAt = row[Notifications.readAt],
            createdAt = row[Notifications.createdAt]
        )
    }

    override suspend fun registerDevice(userId: Int, platform: DevicePlatform, token: String): DeviceToken? = transaction {
        // Check if token already exists
        val existing = DeviceTokens.select { DeviceTokens.token eq token }.singleOrNull()
        if (existing != null) {
            // Update last used and ensure it's active
            val tokenId = existing[DeviceTokens.id].value.toString()
            DeviceTokens.update({ DeviceTokens.id eq UUID.fromString(tokenId) }) {
                it[active] = true
                it[lastUsedAt] = Clock.System.now()
            }
            return@transaction getDeviceToken(tokenId)
        }

        val now = Clock.System.now()
        val id = DeviceTokens.insertAndGetId {
            it[DeviceTokens.userId] = userId
            it[DeviceTokens.platform] = platform.name.lowercase()
            it[DeviceTokens.token] = token
            it[active] = true
            it[createdAt] = now
            it[lastUsedAt] = now
        }

        DeviceTokens.select { DeviceTokens.id eq id }
            .map { resultRowToDeviceToken(it) }
            .singleOrNull()
    }

    override suspend fun getDeviceToken(tokenId: String): DeviceToken? = transaction {
        DeviceTokens.select { DeviceTokens.id eq UUID.fromString(tokenId) }
            .map { resultRowToDeviceToken(it) }
            .singleOrNull()
    }

    override suspend fun getUserDeviceTokens(userId: Int): List<DeviceToken> = transaction {
        DeviceTokens.select { DeviceTokens.userId eq userId }
            .map { resultRowToDeviceToken(it) }
    }

    override suspend fun getActiveUserDeviceTokens(userId: Int): List<DeviceToken> = transaction {
        DeviceTokens.select { (DeviceTokens.userId eq userId) and (DeviceTokens.active eq true) }
            .map { resultRowToDeviceToken(it) }
    }

    override suspend fun updateDeviceLastUsed(tokenId: String, lastUsedAt: Instant) = transaction {
        DeviceTokens.update({ DeviceTokens.id eq UUID.fromString(tokenId) }) {
            it[DeviceTokens.lastUsedAt] = lastUsedAt
        }
        Unit
    }

    override suspend fun deactivateDevice(tokenId: String) = transaction {
        DeviceTokens.update({ DeviceTokens.id eq UUID.fromString(tokenId) }) {
            it[active] = false
        }
        Unit
    }

    override suspend fun removeDevice(tokenId: String, userId: Int): Boolean = transaction {
        val deleted = DeviceTokens.deleteWhere {
            (DeviceTokens.id eq UUID.fromString(tokenId)) and (DeviceTokens.userId eq userId)
        }
        deleted > 0
    }

    override suspend fun getOrCreatePreferences(userId: Int): NotificationPreferencesData = transaction {
        val existing = NotificationPreferences.select { NotificationPreferences.userId eq userId }.singleOrNull()

        if (existing != null) {
            return@transaction resultRowToPreferences(existing)
        }

        val now = Clock.System.now()
        val id = NotificationPreferences.insertAndGetId {
            it[NotificationPreferences.userId] = userId
            it[updatedAt] = now
        }

        NotificationPreferences.select { NotificationPreferences.id eq id }
            .map { resultRowToPreferences(it) }
            .single()
    }

    override suspend fun updatePreferences(userId: Int, preferences: UpdatePreferencesRequest): NotificationPreferencesData? = transaction {
        val existing = getOrCreatePreferences(userId)
        val now = Clock.System.now()

        NotificationPreferences.update({ NotificationPreferences.userId eq userId }) {
            preferences.messagesEnabled?.let { value -> it[messagesEnabled] = value }
            preferences.matchesEnabled?.let { value -> it[matchesEnabled] = value }
            preferences.groupsEnabled?.let { value -> it[groupsEnabled] = value }
            preferences.classRemindersEnabled?.let { value -> it[classRemindersEnabled] = value }
            preferences.socialEnabled?.let { value -> it[socialEnabled] = value }
            preferences.emailFallback?.let { value -> it[emailFallback] = value }
            preferences.quietHoursStart?.let { value ->
                it[quietHoursStart] = if (value.isBlank()) null else LocalTime.parse(value)
            }
            preferences.quietHoursEnd?.let { value ->
                it[quietHoursEnd] = if (value.isBlank()) null else LocalTime.parse(value)
            }
            it[updatedAt] = now
        }

        NotificationPreferences.select { NotificationPreferences.userId eq userId }
            .map { resultRowToPreferences(it) }
            .singleOrNull()
    }

    override suspend fun createNotification(
        userId: Int,
        type: NotificationType,
        title: String,
        body: String,
        data: Map<String, String>,
        priority: NotificationPriority
    ): NotificationData? = transaction {
        val now = Clock.System.now()
        val id = Notifications.insertAndGetId {
            it[Notifications.userId] = userId
            it[Notifications.type] = type.name.lowercase()
            it[Notifications.title] = title
            it[Notifications.body] = body
            it[Notifications.data] = data
            it[Notifications.priority] = priority.name.lowercase()
            it[sentAt] = now
            it[createdAt] = now
        }

        Notifications.select { Notifications.id eq id }
            .map { resultRowToNotification(it) }
            .singleOrNull()
    }

    override suspend fun getNotificationById(id: String): NotificationData? = transaction {
        Notifications.select { Notifications.id eq UUID.fromString(id) }
            .map { resultRowToNotification(it) }
            .singleOrNull()
    }

    override suspend fun getUserNotifications(userId: Int, page: Int, pageSize: Int): Pair<List<NotificationData>, Long> = transaction {
        val total = Notifications.select { Notifications.userId eq userId }.count()
        val notifications = Notifications.select { Notifications.userId eq userId }
            .orderBy(Notifications.createdAt to SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { resultRowToNotification(it) }

        Pair(notifications, total)
    }

    override suspend fun markNotificationAsRead(id: String, userId: Int): Boolean = transaction {
        val updated = Notifications.update({
            (Notifications.id eq UUID.fromString(id)) and (Notifications.userId eq userId)
        }) {
            it[read] = true
            it[readAt] = Clock.System.now()
        }
        updated > 0
    }

    override suspend fun markNotificationAsClicked(id: String): Boolean = transaction {
        val updated = Notifications.update({ Notifications.id eq UUID.fromString(id) }) {
            it[clicked] = true
        }
        updated > 0
    }

    override suspend fun deleteNotification(id: String, userId: Int): Boolean = transaction {
        val deleted = Notifications.deleteWhere {
            (Notifications.id eq UUID.fromString(id)) and (Notifications.userId eq userId)
        }
        deleted > 0
    }

    override suspend fun createDeliveryLog(
        notificationId: String,
        deviceTokenId: String,
        status: DeliveryStatus,
        errorMessage: String?
    ): Boolean = transaction {
        NotificationDeliveryLog.insert {
            it[NotificationDeliveryLog.notificationId] = UUID.fromString(notificationId)
            it[NotificationDeliveryLog.deviceTokenId] = UUID.fromString(deviceTokenId)
            it[NotificationDeliveryLog.status] = status.name.lowercase()
            it[NotificationDeliveryLog.errorMessage] = errorMessage
            it[attemptedAt] = Clock.System.now()
        }
        true
    }

    override suspend fun updateDeliveryStatus(
        notificationId: String,
        deviceTokenId: String,
        status: DeliveryStatus,
        errorMessage: String?
    ): Boolean = transaction {
        // Update existing or create new
        val existing = NotificationDeliveryLog.select {
            (NotificationDeliveryLog.notificationId eq UUID.fromString(notificationId)) and
            (NotificationDeliveryLog.deviceTokenId eq UUID.fromString(deviceTokenId))
        }.orderBy(NotificationDeliveryLog.attemptedAt to SortOrder.DESC).limit(1).singleOrNull()

        if (existing != null) {
            NotificationDeliveryLog.update({
                NotificationDeliveryLog.id eq existing[NotificationDeliveryLog.id]
            }) {
                it[NotificationDeliveryLog.status] = status.name.lowercase()
                it[NotificationDeliveryLog.errorMessage] = errorMessage
                it[attemptedAt] = Clock.System.now()
            }
        } else {
            createDeliveryLog(notificationId, deviceTokenId, status, errorMessage)
        }
        true
    }
}
