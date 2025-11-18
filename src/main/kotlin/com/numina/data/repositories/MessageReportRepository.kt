package com.numina.data.repositories

import com.numina.data.tables.MessageReports
import com.numina.messaging.MessageReport
import com.numina.messaging.ReportStatus
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface MessageReportRepository {
    suspend fun createReport(messageId: String, reporterId: Int, reason: String): MessageReport
    suspend fun getReportById(id: String): MessageReport?
    suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean
    suspend fun getReportsByStatus(status: ReportStatus): List<MessageReport>
    suspend fun getReportsByMessage(messageId: String): List<MessageReport>
}

class MessageReportRepositoryImpl : MessageReportRepository {

    private fun resultRowToMessageReport(row: ResultRow): MessageReport {
        return MessageReport(
            id = row[MessageReports.id],
            messageId = row[MessageReports.messageId],
            reporterId = row[MessageReports.reporterId],
            reason = row[MessageReports.reason],
            status = ReportStatus.valueOf(row[MessageReports.status]),
            createdAt = row[MessageReports.createdAt]
        )
    }

    override suspend fun createReport(messageId: String, reporterId: Int, reason: String): MessageReport = transaction {
        val now = Clock.System.now()
        val reportId = UUID.randomUUID().toString()

        MessageReports.insert {
            it[id] = reportId
            it[MessageReports.messageId] = messageId
            it[MessageReports.reporterId] = reporterId
            it[MessageReports.reason] = reason
            it[status] = ReportStatus.PENDING.name
            it[createdAt] = now
        }

        MessageReport(
            id = reportId,
            messageId = messageId,
            reporterId = reporterId,
            reason = reason,
            status = ReportStatus.PENDING,
            createdAt = now
        )
    }

    override suspend fun getReportById(id: String): MessageReport? = transaction {
        MessageReports.select { MessageReports.id eq id }
            .map { resultRowToMessageReport(it) }
            .singleOrNull()
    }

    override suspend fun updateReportStatus(id: String, status: ReportStatus): Boolean = transaction {
        val updated = MessageReports.update({ MessageReports.id eq id }) {
            it[MessageReports.status] = status.name
        }
        updated > 0
    }

    override suspend fun getReportsByStatus(status: ReportStatus): List<MessageReport> = transaction {
        MessageReports.select { MessageReports.status eq status.name }
            .orderBy(MessageReports.createdAt to SortOrder.DESC)
            .map { resultRowToMessageReport(it) }
    }

    override suspend fun getReportsByMessage(messageId: String): List<MessageReport> = transaction {
        MessageReports.select { MessageReports.messageId eq messageId }
            .orderBy(MessageReports.createdAt to SortOrder.DESC)
            .map { resultRowToMessageReport(it) }
    }
}
