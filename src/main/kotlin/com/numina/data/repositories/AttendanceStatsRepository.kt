package com.numina.data.repositories

import com.numina.data.tables.AttendanceStats
import com.numina.domain.AttendanceStatistics
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface AttendanceStatsRepository {
    suspend fun getStats(userId: Int): AttendanceStatistics?
    suspend fun createStats(userId: Int): AttendanceStatistics
    suspend fun getOrCreateStats(userId: Int): AttendanceStatistics
    suspend fun incrementBooked(userId: Int): AttendanceStatistics?
    suspend fun incrementAttended(userId: Int, attendedDate: Instant): AttendanceStatistics?
    suspend fun incrementCancelled(userId: Int): AttendanceStatistics?
    suspend fun updateStreak(userId: Int, currentStreak: Int, longestStreak: Int): AttendanceStatistics?
}

class AttendanceStatsRepositoryImpl : AttendanceStatsRepository {
    private fun resultRowToStats(row: ResultRow): AttendanceStatistics {
        return AttendanceStatistics(
            id = row[AttendanceStats.id].value,
            userId = row[AttendanceStats.userId].value,
            totalBooked = row[AttendanceStats.totalBooked],
            totalAttended = row[AttendanceStats.totalAttended],
            totalCancelled = row[AttendanceStats.totalCancelled],
            currentStreak = row[AttendanceStats.currentStreak],
            longestStreak = row[AttendanceStats.longestStreak],
            lastAttendedDate = row[AttendanceStats.lastAttendedDate],
            updatedAt = row[AttendanceStats.updatedAt]
        )
    }

    override suspend fun getStats(userId: Int): AttendanceStatistics? = transaction {
        AttendanceStats.select { AttendanceStats.userId eq userId }
            .map { resultRowToStats(it) }
            .singleOrNull()
    }

    override suspend fun createStats(userId: Int): AttendanceStatistics = transaction {
        val now = Clock.System.now()

        val id = AttendanceStats.insertAndGetId {
            it[AttendanceStats.userId] = userId
            it[updatedAt] = now
        }

        AttendanceStats.select { AttendanceStats.id eq id }
            .map { resultRowToStats(it) }
            .single()
    }

    override suspend fun getOrCreateStats(userId: Int): AttendanceStatistics = transaction {
        getStats(userId) ?: createStats(userId)
    }

    override suspend fun incrementBooked(userId: Int): AttendanceStatistics? = transaction {
        val now = Clock.System.now()

        AttendanceStats.update({ AttendanceStats.userId eq userId }) {
            it[totalBooked] = totalBooked + 1
            it[updatedAt] = now
        }

        AttendanceStats.select { AttendanceStats.userId eq userId }
            .map { resultRowToStats(it) }
            .singleOrNull()
    }

    override suspend fun incrementAttended(userId: Int, attendedDate: Instant): AttendanceStatistics? = transaction {
        val now = Clock.System.now()

        AttendanceStats.update({ AttendanceStats.userId eq userId }) {
            it[totalAttended] = totalAttended + 1
            it[lastAttendedDate] = attendedDate
            it[updatedAt] = now
        }

        AttendanceStats.select { AttendanceStats.userId eq userId }
            .map { resultRowToStats(it) }
            .singleOrNull()
    }

    override suspend fun incrementCancelled(userId: Int): AttendanceStatistics? = transaction {
        val now = Clock.System.now()

        AttendanceStats.update({ AttendanceStats.userId eq userId }) {
            it[totalCancelled] = totalCancelled + 1
            it[updatedAt] = now
        }

        AttendanceStats.select { AttendanceStats.userId eq userId }
            .map { resultRowToStats(it) }
            .singleOrNull()
    }

    override suspend fun updateStreak(userId: Int, currentStreak: Int, longestStreak: Int): AttendanceStatistics? = transaction {
        val now = Clock.System.now()

        AttendanceStats.update({ AttendanceStats.userId eq userId }) {
            it[AttendanceStats.currentStreak] = currentStreak
            it[AttendanceStats.longestStreak] = longestStreak
            it[updatedAt] = now
        }

        AttendanceStats.select { AttendanceStats.userId eq userId }
            .map { resultRowToStats(it) }
            .singleOrNull()
    }
}
