package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AdminAuditLog : UUIDTable("admin_audit_log") {
    val adminUserId = integer("admin_user_id").references(Users.id)
    val action = varchar("action", 255)
    val entityType = varchar("entity_type", 100)
    val entityId = varchar("entity_id", 100).nullable()
    val changes = text("changes") // JSON string
    val ipAddress = varchar("ip_address", 50).nullable()
    val createdAt = timestamp("created_at")
}
