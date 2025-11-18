package com.numina.data.repositories

import com.numina.data.tables.BookingReminders
import com.numina.domain.BookingReminderPreferences
import com.numina.domain.UpdateReminderPreferencesRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface ReminderPreferencesRepository {
    suspend fun getPreferences(userId: Int): BookingReminderPreferences?
    suspend fun createPreferences(userId: Int): BookingReminderPreferences
    suspend fun updatePreferences(userId: Int, request: UpdateReminderPreferencesRequest): BookingReminderPreferences?
    suspend fun getOrCreatePreferences(userId: Int): BookingReminderPreferences
}

class ReminderPreferencesRepositoryImpl : ReminderPreferencesRepository {
    private fun resultRowToPreferences(row: ResultRow): BookingReminderPreferences {
        return BookingReminderPreferences(
            id = row[BookingReminders.id].value,
            userId = row[BookingReminders.userId].value,
            enabled = row[BookingReminders.enabled],
            reminder1h = row[BookingReminders.reminder1h],
            reminder24h = row[BookingReminders.reminder24h],
            emailReminders = row[BookingReminders.emailReminders],
            pushReminders = row[BookingReminders.pushReminders],
            updatedAt = row[BookingReminders.updatedAt]
        )
    }

    override suspend fun getPreferences(userId: Int): BookingReminderPreferences? = transaction {
        BookingReminders.select { BookingReminders.userId eq userId }
            .map { resultRowToPreferences(it) }
            .singleOrNull()
    }

    override suspend fun createPreferences(userId: Int): BookingReminderPreferences = transaction {
        val now = Clock.System.now()

        val id = BookingReminders.insertAndGetId {
            it[BookingReminders.userId] = userId
            it[updatedAt] = now
        }

        BookingReminders.select { BookingReminders.id eq id }
            .map { resultRowToPreferences(it) }
            .single()
    }

    override suspend fun updatePreferences(userId: Int, request: UpdateReminderPreferencesRequest): BookingReminderPreferences? = transaction {
        val now = Clock.System.now()

        BookingReminders.update({ BookingReminders.userId eq userId }) {
            request.enabled?.let { e -> it[enabled] = e }
            request.reminder1h?.let { r -> it[reminder1h] = r }
            request.reminder24h?.let { r -> it[reminder24h] = r }
            request.emailReminders?.let { e -> it[emailReminders] = e }
            request.pushReminders?.let { p -> it[pushReminders] = p }
            it[updatedAt] = now
        }

        BookingReminders.select { BookingReminders.userId eq userId }
            .map { resultRowToPreferences(it) }
            .singleOrNull()
    }

    override suspend fun getOrCreatePreferences(userId: Int): BookingReminderPreferences = transaction {
        getPreferences(userId) ?: createPreferences(userId)
    }
}
