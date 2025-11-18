package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object DeviceTokens : UUIDTable("device_tokens") {
    val userId = reference("user_id", Users)
    val platform = varchar("platform", 50) // android, ios, web
    val token = varchar("token", 500).uniqueIndex()
    val active = bool("active").default(true)
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
}
