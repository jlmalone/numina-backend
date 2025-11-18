package com.numina.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object SystemSettings : Table("system_settings") {
    val key = varchar("key", 100)
    val value = text("value")
    val type = varchar("type", 50) // string, int, boolean, json
    val description = text("description").nullable()
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(key)
}
