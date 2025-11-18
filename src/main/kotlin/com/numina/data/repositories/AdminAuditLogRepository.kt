package com.numina.data.repositories

import com.numina.data.tables.AdminAuditLog
import com.numina.domain.AdminAuditLogEntry
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

interface AdminAuditLogRepository {
    suspend fun createLog(
        adminUserId: Int,
        action: String,
        entityType: String,
        entityId: String?,
        changes: Map<String, String>,
        ipAddress: String?
    ): AdminAuditLogEntry

    suspend fun getLogsByAdmin(adminUserId: Int, limit: Int = 100): List<AdminAuditLogEntry>
    suspend fun getLogsByEntity(entityType: String, entityId: String, limit: Int = 100): List<AdminAuditLogEntry>
    suspend fun getAllLogs(limit: Int = 100): List<AdminAuditLogEntry>
}

class AdminAuditLogRepositoryImpl : AdminAuditLogRepository {
    override suspend fun createLog(
        adminUserId: Int,
        action: String,
        entityType: String,
        entityId: String?,
        changes: Map<String, String>,
        ipAddress: String?
    ): AdminAuditLogEntry = transaction {
        val id = AdminAuditLog.insertAndGetId {
            it[AdminAuditLog.adminUserId] = adminUserId
            it[AdminAuditLog.action] = action
            it[AdminAuditLog.entityType] = entityType
            it[AdminAuditLog.entityId] = entityId
            it[AdminAuditLog.changes] = Json.encodeToString(kotlinx.serialization.serializer(), changes)
            it[AdminAuditLog.ipAddress] = ipAddress
            it[createdAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
        }

        AdminAuditLog.select { AdminAuditLog.id eq id }.map { rowToAuditLog(it) }.single()
    }

    override suspend fun getLogsByAdmin(adminUserId: Int, limit: Int): List<AdminAuditLogEntry> = transaction {
        AdminAuditLog.select { AdminAuditLog.adminUserId eq adminUserId }
            .limit(limit)
            .orderBy(AdminAuditLog.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .map { rowToAuditLog(it) }
    }

    override suspend fun getLogsByEntity(entityType: String, entityId: String, limit: Int): List<AdminAuditLogEntry> = transaction {
        AdminAuditLog.select {
            (AdminAuditLog.entityType eq entityType) and (AdminAuditLog.entityId eq entityId)
        }
            .limit(limit)
            .orderBy(AdminAuditLog.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .map { rowToAuditLog(it) }
    }

    override suspend fun getAllLogs(limit: Int): List<AdminAuditLogEntry> = transaction {
        AdminAuditLog.selectAll()
            .limit(limit)
            .orderBy(AdminAuditLog.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .map { rowToAuditLog(it) }
    }

    private fun rowToAuditLog(row: ResultRow): AdminAuditLogEntry {
        val changesJson = row[AdminAuditLog.changes]
        val changes = Json.decodeFromString<Map<String, String>>(changesJson)

        return AdminAuditLogEntry(
            id = row[AdminAuditLog.id].value.toString(),
            adminUserId = row[AdminAuditLog.adminUserId],
            action = row[AdminAuditLog.action],
            entityType = row[AdminAuditLog.entityType],
            entityId = row[AdminAuditLog.entityId],
            changes = changes,
            ipAddress = row[AdminAuditLog.ipAddress],
            createdAt = row[AdminAuditLog.createdAt].toKotlinInstant()
        )
    }
}
