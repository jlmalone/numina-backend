package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.UnauthorizedException
import com.numina.data.repositories.BookingRepository
import com.numina.data.repositories.AttendanceStatsRepository
import com.numina.domain.*
import kotlinx.datetime.Clock

class BookingService(
    private val bookingRepository: BookingRepository,
    private val attendanceStatsRepository: AttendanceStatsRepository
) {
    suspend fun createBooking(userId: Int, request: CreateBookingRequest): Booking {
        val booking = bookingRepository.createBooking(userId, request)
            ?: throw NotFoundException("Class not found")

        // Update stats
        attendanceStatsRepository.getOrCreateStats(userId)
        attendanceStatsRepository.incrementBooked(userId)

        return booking
    }

    suspend fun getBookingById(id: Int, userId: Int): BookingWithClass {
        val booking = bookingRepository.getBookingWithClass(id)
            ?: throw NotFoundException("Booking not found")

        if (booking.userId != userId) {
            throw UnauthorizedException("Not authorized to view this booking")
        }

        return booking
    }

    suspend fun getUserBookings(userId: Int, filters: BookingFilters): List<BookingWithClass> {
        return bookingRepository.getUserBookingsWithClass(userId, filters)
    }

    suspend fun updateBooking(id: Int, userId: Int, request: UpdateBookingRequest): Booking {
        val booking = bookingRepository.updateBooking(id, userId, request)
            ?: throw NotFoundException("Booking not found")

        return booking
    }

    suspend fun deleteBooking(id: Int, userId: Int): Boolean {
        val deleted = bookingRepository.deleteBooking(id, userId)
        if (!deleted) {
            throw NotFoundException("Booking not found")
        }
        return true
    }

    suspend fun markAttended(id: Int, userId: Int): Booking {
        // Get the booking first to verify ownership
        val existingBooking = bookingRepository.getBookingById(id)
            ?: throw NotFoundException("Booking not found")

        if (existingBooking.userId != userId) {
            throw UnauthorizedException("Not authorized to modify this booking")
        }

        val booking = bookingRepository.markAttended(id, userId)
            ?: throw NotFoundException("Booking not found")

        // Update stats
        attendanceStatsRepository.getOrCreateStats(userId)
        attendanceStatsRepository.incrementAttended(userId, Clock.System.now())

        return booking
    }

    suspend fun cancelBooking(id: Int, userId: Int, request: CancelBookingRequest): Booking {
        // Get the booking first to verify ownership
        val existingBooking = bookingRepository.getBookingById(id)
            ?: throw NotFoundException("Booking not found")

        if (existingBooking.userId != userId) {
            throw UnauthorizedException("Not authorized to modify this booking")
        }

        val booking = bookingRepository.cancelBooking(id, userId, request.cancellationReason)
            ?: throw NotFoundException("Booking not found")

        // Update stats
        attendanceStatsRepository.getOrCreateStats(userId)
        attendanceStatsRepository.incrementCancelled(userId)

        return booking
    }
}
