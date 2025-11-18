package com.numina

import com.numina.domain.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.Duration.Companion.days

class BookingRoutesTest {
    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient, email: String): String {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = email,
                password = "password123",
                name = "Test User"
            ))
        }
        val loginResponse = response.body<LoginResponse>()
        return loginResponse.token
    }

    private suspend fun createTestClass(client: io.ktor.client.HttpClient, token: String): FitnessClass {
        val now = Clock.System.now()
        val classTime = now.plus(1.days)

        val response = client.post("/api/v1/classes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateClassRequest(
                name = "Test Class",
                description = "Test class for bookings",
                datetime = classTime,
                locationLat = 40.7128,
                locationLong = -74.0060,
                intensity = 5,
                price = 25.0,
                capacity = 20,
                tags = listOf("test")
            ))
        }
        return response.body()
    }

    @Test
    fun testCreateBooking() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking1@example.com")
        val fitnessClass = createTestClass(client, token)

        val response = client.post("/api/v1/bookings") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateBookingRequest(
                classId = fitnessClass.id,
                bookingSource = "manual"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val booking = response.body<Booking>()
        assertEquals(fitnessClass.id, booking.classId)
        assertEquals("booked", booking.status)
    }

    @Test
    fun testGetUserBookings() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking2@example.com")
        val fitnessClass = createTestClass(client, token)

        // Create a booking
        client.post("/api/v1/bookings") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateBookingRequest(
                classId = fitnessClass.id,
                bookingSource = "manual"
            ))
        }

        // Get bookings
        val response = client.get("/api/v1/bookings") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val bookings = response.body<List<BookingWithClass>>()
        assertTrue(bookings.isNotEmpty())
        assertEquals(fitnessClass.id, bookings[0].fitnessClass.id)
    }

    @Test
    fun testMarkAttended() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking3@example.com")
        val fitnessClass = createTestClass(client, token)

        // Create a booking
        val createResponse = client.post("/api/v1/bookings") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateBookingRequest(
                classId = fitnessClass.id,
                bookingSource = "manual"
            ))
        }
        val booking = createResponse.body<Booking>()

        // Mark as attended
        val response = client.post("/api/v1/bookings/${booking.id}/mark-attended") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updatedBooking = response.body<Booking>()
        assertEquals("attended", updatedBooking.status)
        assertNotNull(updatedBooking.attendedAt)
    }

    @Test
    fun testCancelBooking() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking4@example.com")
        val fitnessClass = createTestClass(client, token)

        // Create a booking
        val createResponse = client.post("/api/v1/bookings") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateBookingRequest(
                classId = fitnessClass.id,
                bookingSource = "manual"
            ))
        }
        val booking = createResponse.body<Booking>()

        // Cancel booking
        val response = client.post("/api/v1/bookings/${booking.id}/cancel") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CancelBookingRequest(
                cancellationReason = "Schedule conflict"
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val cancelledBooking = response.body<Booking>()
        assertEquals("cancelled", cancelledBooking.status)
        assertEquals("Schedule conflict", cancelledBooking.cancellationReason)
    }

    @Test
    fun testGetAttendanceStats() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking5@example.com")

        // Get stats (should create default stats)
        val response = client.get("/api/v1/bookings/stats") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val stats = response.body<AttendanceStatistics>()
        assertEquals(0, stats.totalBooked)
        assertEquals(0, stats.totalAttended)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun testGetUpcomingBookings() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking6@example.com")
        val fitnessClass = createTestClass(client, token)

        // Create a booking
        client.post("/api/v1/bookings") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateBookingRequest(
                classId = fitnessClass.id,
                bookingSource = "manual"
            ))
        }

        // Get upcoming bookings
        val response = client.get("/api/v1/calendar/upcoming") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val bookings = response.body<List<BookingWithClass>>()
        assertTrue(bookings.isNotEmpty())
    }

    @Test
    fun testExportCalendar() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking7@example.com")

        // Export calendar
        val response = client.get("/api/v1/calendar/export") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val icalContent = response.body<String>()
        assertTrue(icalContent.contains("BEGIN:VCALENDAR"))
        assertTrue(icalContent.contains("END:VCALENDAR"))
    }

    @Test
    fun testReminderPreferences() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "booking8@example.com")

        // Get preferences (should create defaults)
        val getResponse = client.get("/api/v1/bookings/reminder-preferences") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val preferences = getResponse.body<BookingReminderPreferences>()
        assertTrue(preferences.enabled)

        // Update preferences
        val updateResponse = client.put("/api/v1/bookings/reminder-preferences") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateReminderPreferencesRequest(
                enabled = false,
                emailReminders = true
            ))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updatedPreferences = updateResponse.body<BookingReminderPreferences>()
        assertFalse(updatedPreferences.enabled)
        assertTrue(updatedPreferences.emailReminders)
    }
}
