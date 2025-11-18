package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AdminUsers : UUIDTable("admin_users") {
    val userId = integer("user_id").references(Users.id).uniqueIndex()
    val role = varchar("role", 50) // super_admin, admin, moderator
    val permissions = text("permissions") // JSON string
    val createdAt = timestamp("created_at")
}
