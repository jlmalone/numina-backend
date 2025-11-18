package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Notifications : UUIDTable("notifications") {
    val userId = reference("user_id", Users)
    val type = varchar("type", 50) // message, match, group, reminder, social, system
    val title = varchar("title", 255)
    val body = text("body")
    val data = jsonb<Map<String, String>>("data") // Additional payload data
    val priority = varchar("priority", 50) // urgent, high, normal, low
    val read = bool("read").default(false)
    val clicked = bool("clicked").default(false)
    val sentAt = timestamp("sent_at")
    val deliveredAt = timestamp("delivered_at").nullable()
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at")
}
