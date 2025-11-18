package com.numina.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Users : IntIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}
