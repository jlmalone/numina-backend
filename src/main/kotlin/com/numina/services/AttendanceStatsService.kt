package com.numina.services

import com.numina.data.repositories.AttendanceStatsRepository
import com.numina.data.repositories.BookingRepository
import com.numina.domain.AttendanceStatistics
import com.numina.domain.BookingFilters
import com.numina.domain.BookingStatus
import kotlinx.datetime.*

class AttendanceStatsService(
    private val attendanceStatsRepository: AttendanceStatsRepository,
    private val bookingRepository: BookingRepository
) {
    suspend fun getStats(userId: Int): AttendanceStatistics {
        return attendanceStatsRepository.getOrCreateStats(userId)
    }

    suspend fun calculateAndUpdateStreak(userId: Int): AttendanceStatistics {
        val stats = attendanceStatsRepository.getOrCreateStats(userId)

        // Get all attended bookings for the user, ordered by date
        val attendedBookings = bookingRepository.getUserBookings(
            userId,
            BookingFilters(status = BookingStatus.ATTENDED.name.lowercase())
        ).sortedBy { it.classDatetime }

        if (attendedBookings.isEmpty()) {
            return attendanceStatsRepository.updateStreak(userId, 0, stats.longestStreak) ?: stats
        }

        // Calculate current streak
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.UTC).date

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var lastAttendedDate: LocalDate? = null

        attendedBookings.forEach { booking ->
            val bookingDate = booking.classDatetime.toLocalDateTime(TimeZone.UTC).date

            if (lastAttendedDate == null) {
                tempStreak = 1
            } else {
                val daysBetween = lastAttendedDate!!.daysUntil(bookingDate)
                if (daysBetween == 1) {
                    // Consecutive day
                    tempStreak++
                } else if (daysBetween == 0) {
                    // Same day, don't change streak
                } else {
                    // Streak broken
                    tempStreak = 1
                }
            }

            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }

            lastAttendedDate = bookingDate
        }

        // Determine if the streak is still active
        if (lastAttendedDate != null) {
            val daysSinceLastAttendance = lastAttendedDate.daysUntil(today)
            currentStreak = if (daysSinceLastAttendance <= 1) {
                // Streak is still active (attended today or yesterday)
                tempStreak
            } else {
                0
            }
        }

        // Update the longest streak if current streak is higher
        val finalLongestStreak = maxOf(longestStreak, stats.longestStreak)

        return attendanceStatsRepository.updateStreak(userId, currentStreak, finalLongestStreak) ?: stats
    }

    suspend fun updateAllStats() {
        // This would be called by a background job
        // For now, we'll just note that this is a placeholder
        // In a real implementation, we'd iterate through all users with bookings
        // and update their stats
    }
}
