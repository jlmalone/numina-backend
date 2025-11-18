package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

enum class BookingStatus {
    BOOKED,
    ATTENDED,
    CANCELLED,
    NO_SHOW
}

enum class BookingSource {
    MINDBODY,
    CLASSPASS,
    MANUAL
}

@Serializable
data class Booking(
    val id: Int,
    val userId: Int,
    val classId: Int,
    val bookingSource: String,
    val externalBookingId: String? = null,
    val bookedAt: Instant,
    val classDatetime: Instant,
    val status: String,
    val attendedAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val cancellationReason: String? = null,
    val reminderSent1h: Boolean = false,
    val reminderSent24h: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class BookingWithClass(
    val id: Int,
    val userId: Int,
    val bookingSource: String,
    val externalBookingId: String? = null,
    val bookedAt: Instant,
    val classDatetime: Instant,
    val status: String,
    val attendedAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val cancellationReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val fitnessClass: FitnessClass
)

@Serializable
data class CreateBookingRequest(
    val classId: Int,
    val bookingSource: String = "manual",
    val externalBookingId: String? = null
)

@Serializable
data class UpdateBookingRequest(
    val status: String? = null,
    val externalBookingId: String? = null
)

@Serializable
data class CancelBookingRequest(
    val cancellationReason: String? = null
)

@Serializable
data class BookingReminderPreferences(
    val id: Int,
    val userId: Int,
    val enabled: Boolean = true,
    val reminder1h: Boolean = true,
    val reminder24h: Boolean = true,
    val emailReminders: Boolean = false,
    val pushReminders: Boolean = true,
    val updatedAt: Instant
)

@Serializable
data class UpdateReminderPreferencesRequest(
    val enabled: Boolean? = null,
    val reminder1h: Boolean? = null,
    val reminder24h: Boolean? = null,
    val emailReminders: Boolean? = null,
    val pushReminders: Boolean? = null
)

@Serializable
data class AttendanceStatistics(
    val id: Int,
    val userId: Int,
    val totalBooked: Int = 0,
    val totalAttended: Int = 0,
    val totalCancelled: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastAttendedDate: Instant? = null,
    val updatedAt: Instant
)

@Serializable
data class CalendarDay(
    val date: String, // YYYY-MM-DD
    val bookings: List<BookingWithClass>
)

@Serializable
data class CalendarMonth(
    val month: String, // YYYY-MM
    val days: List<CalendarDay>
)

@Serializable
data class BookingFilters(
    val status: String? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val bookingSource: String? = null
)
