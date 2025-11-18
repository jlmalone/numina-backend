package com.numina.services

import com.numina.data.repositories.BookingRepository
import com.numina.domain.BookingWithClass
import com.numina.domain.CalendarDay
import com.numina.domain.CalendarMonth
import kotlinx.datetime.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarService(
    private val bookingRepository: BookingRepository
) {
    suspend fun getUpcomingBookings(userId: Int): List<BookingWithClass> {
        val now = Clock.System.now()
        return bookingRepository.getUpcomingBookings(userId, now)
    }

    suspend fun getMonthCalendar(userId: Int, yearMonth: String): CalendarMonth {
        // Parse yearMonth (format: YYYY-MM)
        val parts = yearMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        // Get first and last day of month
        val firstDay = LocalDate(year, month, 1)
        val lastDay = LocalDate(year, month, firstDay.month.length(isLeapYear(year)))

        val startDateTime = firstDay.atStartOfDayIn(TimeZone.UTC)
        val endDateTime = lastDay.atTime(23, 59, 59).toInstant(TimeZone.UTC)

        val bookings = bookingRepository.getBookingsByDateRange(userId, startDateTime, endDateTime)

        // Group bookings by day
        val bookingsByDay = bookings.groupBy { booking ->
            booking.classDatetime.toLocalDateTime(TimeZone.UTC).date.toString()
        }

        // Create calendar days
        val days = mutableListOf<CalendarDay>()
        var currentDate = firstDay
        while (currentDate <= lastDay) {
            val dateStr = currentDate.toString()
            val dayBookings = bookingsByDay[dateStr] ?: emptyList()
            days.add(CalendarDay(date = dateStr, bookings = dayBookings))
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }

        return CalendarMonth(month = yearMonth, days = days)
    }

    suspend fun exportToICalendar(userId: Int): String {
        val now = Clock.System.now()
        val bookings = bookingRepository.getUpcomingBookings(userId, now)

        return generateICalendar(bookings)
    }

    private fun generateICalendar(bookings: List<BookingWithClass>): String {
        val ical = StringBuilder()
        ical.appendLine("BEGIN:VCALENDAR")
        ical.appendLine("VERSION:2.0")
        ical.appendLine("PRODID:-//Numina//Fitness Classes//EN")
        ical.appendLine("CALSCALE:GREGORIAN")
        ical.appendLine("METHOD:PUBLISH")

        bookings.forEach { booking ->
            ical.appendLine("BEGIN:VEVENT")
            ical.appendLine("UID:booking-${booking.id}@numina.app")
            ical.appendLine("DTSTAMP:${formatICalDate(Clock.System.now())}")
            ical.appendLine("DTSTART:${formatICalDate(booking.classDatetime)}")
            // Assume 1 hour duration
            ical.appendLine("DTEND:${formatICalDate(booking.classDatetime.plus(1, DateTimeUnit.HOUR))}")
            ical.appendLine("SUMMARY:${escapeICalText(booking.fitnessClass.name)}")
            ical.appendLine("DESCRIPTION:${escapeICalText(booking.fitnessClass.description)}")
            ical.appendLine("LOCATION:${booking.fitnessClass.locationLat},${booking.fitnessClass.locationLong}")
            booking.fitnessClass.trainer?.let { trainer ->
                ical.appendLine("ORGANIZER:CN=${escapeICalText(trainer)}")
            }
            ical.appendLine("STATUS:CONFIRMED")
            ical.appendLine("SEQUENCE:0")
            ical.appendLine("END:VEVENT")
        }

        ical.appendLine("END:VCALENDAR")
        return ical.toString()
    }

    private fun formatICalDate(instant: Instant): String {
        // Format: 20231225T120000Z
        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
        return String.format(
            "%04d%02d%02dT%02d%02d%02dZ",
            dateTime.year,
            dateTime.monthNumber,
            dateTime.dayOfMonth,
            dateTime.hour,
            dateTime.minute,
            dateTime.second
        )
    }

    private fun escapeICalText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
