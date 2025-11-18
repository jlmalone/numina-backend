package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.time
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object NotificationPreferences : UUIDTable("notification_preferences") {
    val userId = reference("user_id", Users).uniqueIndex()
    val messagesEnabled = bool("messages_enabled").default(true)
    val matchesEnabled = bool("matches_enabled").default(true)
    val groupsEnabled = bool("groups_enabled").default(true)
    val classRemindersEnabled = bool("class_reminders_enabled").default(true)
    val socialEnabled = bool("social_enabled").default(true)
    val emailFallback = bool("email_fallback").default(true)
    val quietHoursStart = time("quiet_hours_start").nullable()
    val quietHoursEnd = time("quiet_hours_end").nullable()
    val updatedAt = timestamp("updated_at")
}
