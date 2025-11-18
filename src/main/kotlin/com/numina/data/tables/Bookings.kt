package com.numina.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Bookings : IntIdTable("bookings") {
    val userId = reference("user_id", Users)
    val classId = reference("class_id", Classes)
    val bookingSource = varchar("booking_source", 50) // "mindbody", "classpass", "manual"
    val externalBookingId = varchar("external_booking_id", 255).nullable()
    val bookedAt = timestamp("booked_at")
    val classDatetime = timestamp("class_datetime") // Denormalized for queries
    val status = varchar("status", 20) // "booked", "attended", "cancelled", "no_show"
    val attendedAt = timestamp("attended_at").nullable()
    val cancelledAt = timestamp("cancelled_at").nullable()
    val cancellationReason = text("cancellation_reason").nullable()
    val reminderSent1h = bool("reminder_sent_1h").default(false)
    val reminderSent24h = bool("reminder_sent_24h").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        index(false, userId, classDatetime)
        index(false, classDatetime)
        index(false, status)
    }
}

object BookingReminders : IntIdTable("booking_reminders") {
    val userId = reference("user_id", Users).uniqueIndex()
    val enabled = bool("enabled").default(true)
    val reminder1h = bool("reminder_1h").default(true)
    val reminder24h = bool("reminder_24h").default(true)
    val emailReminders = bool("email_reminders").default(false)
    val pushReminders = bool("push_reminders").default(true)
    val updatedAt = timestamp("updated_at")
}

object AttendanceStats : IntIdTable("attendance_stats") {
    val userId = reference("user_id", Users).uniqueIndex()
    val totalBooked = integer("total_booked").default(0)
    val totalAttended = integer("total_attended").default(0)
    val totalCancelled = integer("total_cancelled").default(0)
    val currentStreak = integer("current_streak").default(0)
    val longestStreak = integer("longest_streak").default(0)
    val lastAttendedDate = timestamp("last_attended_date").nullable()
    val updatedAt = timestamp("updated_at")
}
