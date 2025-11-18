package com.numina.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : IntIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val isSuspended = bool("is_suspended").default(false)
    val suspensionReason = varchar("suspension_reason", 500).nullable()
    val createdAt = timestamp("created_at").index()
    val updatedAt = timestamp("updated_at")
}
