package com.numina.services

import com.numina.data.repositories.BookingRepository
import com.numina.data.repositories.ReminderPreferencesRepository
import com.numina.domain.BookingReminderPreferences
import com.numina.domain.UpdateReminderPreferencesRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import io.ktor.server.application.*

class ReminderService(
    private val bookingRepository: BookingRepository,
    private val reminderPreferencesRepository: ReminderPreferencesRepository
) {
    suspend fun getPreferences(userId: Int): BookingReminderPreferences {
        return reminderPreferencesRepository.getOrCreatePreferences(userId)
    }

    suspend fun updatePreferences(userId: Int, request: UpdateReminderPreferencesRequest): BookingReminderPreferences {
        // Ensure preferences exist
        reminderPreferencesRepository.getOrCreatePreferences(userId)

        return reminderPreferencesRepository.updatePreferences(userId, request)
            ?: throw IllegalStateException("Failed to update reminder preferences")
    }

    suspend fun processReminders(application: Application) {
        val now = Clock.System.now()

        // Process 1-hour reminders
        val oneHourFromNow = now.plus(1, DateTimeUnit.HOUR)
        val bookingsFor1h = bookingRepository.getBookingsNeedingReminder("1h", oneHourFromNow)

        bookingsFor1h.forEach { booking ->
            val preferences = reminderPreferencesRepository.getPreferences(booking.userId)
            if (preferences?.enabled == true && preferences.reminder1h) {
                sendReminder(application, booking.userId, booking.id, "1h")
                bookingRepository.markReminderSent(booking.id, "1h")
            }
        }

        // Process 24-hour reminders
        val twentyFourHoursFromNow = now.plus(24, DateTimeUnit.HOUR)
        val bookingsFor24h = bookingRepository.getBookingsNeedingReminder("24h", twentyFourHoursFromNow)

        bookingsFor24h.forEach { booking ->
            val preferences = reminderPreferencesRepository.getPreferences(booking.userId)
            if (preferences?.enabled == true && preferences.reminder24h) {
                sendReminder(application, booking.userId, booking.id, "24h")
                bookingRepository.markReminderSent(booking.id, "24h")
            }
        }
    }

    private fun sendReminder(application: Application, userId: Int, bookingId: Int, reminderType: String) {
        // Placeholder for actual notification sending
        // In a real implementation, this would integrate with:
        // - Push notification service (FCM, APNs)
        // - Email service
        application.log.info("Sending ${reminderType} reminder to user $userId for booking $bookingId")
    }
}
