package com.numina.services

import com.numina.data.repositories.AdminAuditLogRepository
import com.numina.domain.AdminAuditLogEntry
import org.slf4j.LoggerFactory

interface AuditLogService {
    suspend fun logAction(
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

class AuditLogServiceImpl(
    private val auditLogRepository: AdminAuditLogRepository
) : AuditLogService {
    private val logger = LoggerFactory.getLogger(AuditLogServiceImpl::class.java)

    override suspend fun logAction(
        adminUserId: Int,
        action: String,
        entityType: String,
        entityId: String?,
        changes: Map<String, String>,
        ipAddress: String?
    ): AdminAuditLogEntry {
        logger.info("Logging admin action: adminUserId=$adminUserId, action=$action, entityType=$entityType, entityId=$entityId")

        return auditLogRepository.createLog(
            adminUserId = adminUserId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            changes = changes,
            ipAddress = ipAddress
        )
    }

    override suspend fun getLogsByAdmin(adminUserId: Int, limit: Int): List<AdminAuditLogEntry> {
        logger.debug("Fetching logs for admin userId=$adminUserId")
        return auditLogRepository.getLogsByAdmin(adminUserId, limit)
    }

    override suspend fun getLogsByEntity(entityType: String, entityId: String, limit: Int): List<AdminAuditLogEntry> {
        logger.debug("Fetching logs for entity: type=$entityType, id=$entityId")
        return auditLogRepository.getLogsByEntity(entityType, entityId, limit)
    }

    override suspend fun getAllLogs(limit: Int): List<AdminAuditLogEntry> {
        logger.debug("Fetching all audit logs (limit=$limit)")
        return auditLogRepository.getAllLogs(limit)
    }
}
