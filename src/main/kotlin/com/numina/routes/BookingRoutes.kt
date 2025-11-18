package com.numina.routes

import com.numina.domain.*
import com.numina.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.bookingRoutes() {
    val bookingService by inject<BookingService>()
    val calendarService by inject<CalendarService>()
    val reminderService by inject<ReminderService>()
    val attendanceStatsService by inject<AttendanceStatsService>()

    authenticate("auth-jwt") {
        route("/bookings") {
            // Create booking
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<CreateBookingRequest>()

                val booking = bookingService.createBooking(userId, request)
                call.respond(HttpStatusCode.Created, booking)
            }

            // List my bookings
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val filters = BookingFilters(
                    status = call.request.queryParameters["status"],
                    startDate = call.request.queryParameters["startDate"]?.let { Instant.parse(it) },
                    endDate = call.request.queryParameters["endDate"]?.let { Instant.parse(it) },
                    bookingSource = call.request.queryParameters["bookingSource"]
                )

                val bookings = bookingService.getUserBookings(userId, filters)
                call.respond(HttpStatusCode.OK, bookings)
            }

            // Get booking details
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val bookingId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid booking ID")

                val booking = bookingService.getBookingById(bookingId, userId)
                call.respond(HttpStatusCode.OK, booking)
            }

            // Update booking
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val bookingId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid booking ID")
                val request = call.receive<UpdateBookingRequest>()

                val booking = bookingService.updateBooking(bookingId, userId, request)
                call.respond(HttpStatusCode.OK, booking)
            }

            // Delete booking
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val bookingId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid booking ID")

                bookingService.deleteBooking(bookingId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Booking deleted successfully"))
            }

            // Mark as attended
            post("/{id}/mark-attended") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val bookingId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid booking ID")

                val booking = bookingService.markAttended(bookingId, userId)

                // Update streak calculation
                attendanceStatsService.calculateAndUpdateStreak(userId)

                call.respond(HttpStatusCode.OK, booking)
            }

            // Cancel booking
            post("/{id}/cancel") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val bookingId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid booking ID")
                val request = call.receive<CancelBookingRequest>()

                val booking = bookingService.cancelBooking(bookingId, userId, request)
                call.respond(HttpStatusCode.OK, booking)
            }

            // Get reminder preferences
            get("/reminder-preferences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val preferences = reminderService.getPreferences(userId)
                call.respond(HttpStatusCode.OK, preferences)
            }

            // Update reminder preferences
            put("/reminder-preferences") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<UpdateReminderPreferencesRequest>()

                val preferences = reminderService.updatePreferences(userId, request)
                call.respond(HttpStatusCode.OK, preferences)
            }

            // Get attendance stats
            get("/stats") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val stats = attendanceStatsService.getStats(userId)
                call.respond(HttpStatusCode.OK, stats)
            }

            // Get current streak
            get("/streak") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val stats = attendanceStatsService.calculateAndUpdateStreak(userId)
                call.respond(HttpStatusCode.OK, mapOf(
                    "currentStreak" to stats.currentStreak,
                    "longestStreak" to stats.longestStreak,
                    "lastAttendedDate" to stats.lastAttendedDate
                ))
            }
        }

        route("/calendar") {
            // Get upcoming classes
            get("/upcoming") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val bookings = calendarService.getUpcomingBookings(userId)
                call.respond(HttpStatusCode.OK, bookings)
            }

            // Calendar month view
            get("/month/{yyyy-MM}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val yearMonth = call.parameters["yyyy-MM"]
                    ?: throw IllegalArgumentException("Invalid month format")

                val calendar = calendarService.getMonthCalendar(userId, yearMonth)
                call.respond(HttpStatusCode.OK, calendar)
            }

            // Export iCal format
            get("/export") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val icalContent = calendarService.exportToICalendar(userId)
                call.respondText(
                    icalContent,
                    ContentType.parse("text/calendar"),
                    HttpStatusCode.OK
                )
            }
        }
    }
}
