package com.numina.data.repositories

import com.numina.data.tables.Bookings
import com.numina.data.tables.Classes
import com.numina.domain.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface BookingRepository {
    suspend fun createBooking(userId: Int, request: CreateBookingRequest): Booking?
    suspend fun getBookingById(id: Int): Booking?
    suspend fun getBookingWithClass(id: Int): BookingWithClass?
    suspend fun getUserBookings(userId: Int, filters: BookingFilters): List<Booking>
    suspend fun getUserBookingsWithClass(userId: Int, filters: BookingFilters): List<BookingWithClass>
    suspend fun updateBooking(id: Int, userId: Int, request: UpdateBookingRequest): Booking?
    suspend fun deleteBooking(id: Int, userId: Int): Boolean
    suspend fun markAttended(id: Int, userId: Int): Booking?
    suspend fun cancelBooking(id: Int, userId: Int, reason: String?): Booking?
    suspend fun getUpcomingBookings(userId: Int, fromDate: Instant): List<BookingWithClass>
    suspend fun getBookingsByDateRange(userId: Int, startDate: Instant, endDate: Instant): List<BookingWithClass>
    suspend fun getBookingsNeedingReminder(reminderType: String, beforeTime: Instant): List<Booking>
    suspend fun markReminderSent(bookingId: Int, reminderType: String): Boolean
}

class BookingRepositoryImpl(private val classRepository: ClassRepository) : BookingRepository {
    private fun resultRowToBooking(row: ResultRow): Booking {
        return Booking(
            id = row[Bookings.id].value,
            userId = row[Bookings.userId].value,
            classId = row[Bookings.classId].value,
            bookingSource = row[Bookings.bookingSource],
            externalBookingId = row[Bookings.externalBookingId],
            bookedAt = row[Bookings.bookedAt],
            classDatetime = row[Bookings.classDatetime],
            status = row[Bookings.status],
            attendedAt = row[Bookings.attendedAt],
            cancelledAt = row[Bookings.cancelledAt],
            cancellationReason = row[Bookings.cancellationReason],
            reminderSent1h = row[Bookings.reminderSent1h],
            reminderSent24h = row[Bookings.reminderSent24h],
            createdAt = row[Bookings.createdAt],
            updatedAt = row[Bookings.updatedAt]
        )
    }

    private suspend fun resultRowToBookingWithClass(row: ResultRow): BookingWithClass? {
        val classId = row[Bookings.classId].value
        val fitnessClass = classRepository.getClassById(classId) ?: return null

        return BookingWithClass(
            id = row[Bookings.id].value,
            userId = row[Bookings.userId].value,
            bookingSource = row[Bookings.bookingSource],
            externalBookingId = row[Bookings.externalBookingId],
            bookedAt = row[Bookings.bookedAt],
            classDatetime = row[Bookings.classDatetime],
            status = row[Bookings.status],
            attendedAt = row[Bookings.attendedAt],
            cancelledAt = row[Bookings.cancelledAt],
            cancellationReason = row[Bookings.cancellationReason],
            createdAt = row[Bookings.createdAt],
            updatedAt = row[Bookings.updatedAt],
            fitnessClass = fitnessClass
        )
    }

    override suspend fun createBooking(userId: Int, request: CreateBookingRequest): Booking? = transaction {
        val now = Clock.System.now()

        // Get class to denormalize datetime
        val fitnessClass = classRepository.getClassById(request.classId) ?: return@transaction null

        val bookingId = Bookings.insertAndGetId {
            it[Bookings.userId] = userId
            it[classId] = request.classId
            it[bookingSource] = request.bookingSource
            it[externalBookingId] = request.externalBookingId
            it[bookedAt] = now
            it[classDatetime] = fitnessClass.datetime
            it[status] = BookingStatus.BOOKED.name.lowercase()
            it[createdAt] = now
            it[updatedAt] = now
        }

        Bookings.select { Bookings.id eq bookingId }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun getBookingById(id: Int): Booking? = transaction {
        Bookings.select { Bookings.id eq id }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun getBookingWithClass(id: Int): BookingWithClass? = transaction {
        Bookings.select { Bookings.id eq id }
            .mapNotNull { resultRowToBookingWithClass(it) }
            .singleOrNull()
    }

    override suspend fun getUserBookings(userId: Int, filters: BookingFilters): List<Booking> = transaction {
        var query = Bookings.select { Bookings.userId eq userId }

        filters.status?.let { status ->
            query = query.andWhere { Bookings.status eq status }
        }
        filters.startDate?.let { start ->
            query = query.andWhere { Bookings.classDatetime greaterEq start }
        }
        filters.endDate?.let { end ->
            query = query.andWhere { Bookings.classDatetime lessEq end }
        }
        filters.bookingSource?.let { source ->
            query = query.andWhere { Bookings.bookingSource eq source }
        }

        query.orderBy(Bookings.classDatetime to SortOrder.ASC)
            .map { resultRowToBooking(it) }
    }

    override suspend fun getUserBookingsWithClass(userId: Int, filters: BookingFilters): List<BookingWithClass> = transaction {
        var query = Bookings.select { Bookings.userId eq userId }

        filters.status?.let { status ->
            query = query.andWhere { Bookings.status eq status }
        }
        filters.startDate?.let { start ->
            query = query.andWhere { Bookings.classDatetime greaterEq start }
        }
        filters.endDate?.let { end ->
            query = query.andWhere { Bookings.classDatetime lessEq end }
        }
        filters.bookingSource?.let { source ->
            query = query.andWhere { Bookings.bookingSource eq source }
        }

        query.orderBy(Bookings.classDatetime to SortOrder.ASC)
            .mapNotNull { resultRowToBookingWithClass(it) }
    }

    override suspend fun updateBooking(id: Int, userId: Int, request: UpdateBookingRequest): Booking? = transaction {
        val now = Clock.System.now()

        Bookings.update({ (Bookings.id eq id) and (Bookings.userId eq userId) }) {
            request.status?.let { status -> it[Bookings.status] = status }
            request.externalBookingId?.let { extId -> it[externalBookingId] = extId }
            it[updatedAt] = now
        }

        Bookings.select { Bookings.id eq id }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun deleteBooking(id: Int, userId: Int): Boolean = transaction {
        Bookings.deleteWhere { (Bookings.id eq id) and (Bookings.userId eq userId) } > 0
    }

    override suspend fun markAttended(id: Int, userId: Int): Booking? = transaction {
        val now = Clock.System.now()

        Bookings.update({ (Bookings.id eq id) and (Bookings.userId eq userId) }) {
            it[status] = BookingStatus.ATTENDED.name.lowercase()
            it[attendedAt] = now
            it[updatedAt] = now
        }

        Bookings.select { Bookings.id eq id }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun cancelBooking(id: Int, userId: Int, reason: String?): Booking? = transaction {
        val now = Clock.System.now()

        Bookings.update({ (Bookings.id eq id) and (Bookings.userId eq userId) }) {
            it[status] = BookingStatus.CANCELLED.name.lowercase()
            it[cancelledAt] = now
            it[cancellationReason] = reason
            it[updatedAt] = now
        }

        Bookings.select { Bookings.id eq id }
            .map { resultRowToBooking(it) }
            .singleOrNull()
    }

    override suspend fun getUpcomingBookings(userId: Int, fromDate: Instant): List<BookingWithClass> = transaction {
        Bookings.select {
            (Bookings.userId eq userId) and
            (Bookings.classDatetime greaterEq fromDate) and
            (Bookings.status eq BookingStatus.BOOKED.name.lowercase())
        }
            .orderBy(Bookings.classDatetime to SortOrder.ASC)
            .mapNotNull { resultRowToBookingWithClass(it) }
    }

    override suspend fun getBookingsByDateRange(userId: Int, startDate: Instant, endDate: Instant): List<BookingWithClass> = transaction {
        Bookings.select {
            (Bookings.userId eq userId) and
            (Bookings.classDatetime greaterEq startDate) and
            (Bookings.classDatetime lessEq endDate)
        }
            .orderBy(Bookings.classDatetime to SortOrder.ASC)
            .mapNotNull { resultRowToBookingWithClass(it) }
    }

    override suspend fun getBookingsNeedingReminder(reminderType: String, beforeTime: Instant): List<Booking> = transaction {
        when (reminderType) {
            "1h" -> {
                Bookings.select {
                    (Bookings.status eq BookingStatus.BOOKED.name.lowercase()) and
                    (Bookings.classDatetime lessEq beforeTime) and
                    (Bookings.reminderSent1h eq false)
                }.map { resultRowToBooking(it) }
            }
            "24h" -> {
                Bookings.select {
                    (Bookings.status eq BookingStatus.BOOKED.name.lowercase()) and
                    (Bookings.classDatetime lessEq beforeTime) and
                    (Bookings.reminderSent24h eq false)
                }.map { resultRowToBooking(it) }
            }
            else -> emptyList()
        }
    }

    override suspend fun markReminderSent(bookingId: Int, reminderType: String): Boolean = transaction {
        val updated = when (reminderType) {
            "1h" -> {
                Bookings.update({ Bookings.id eq bookingId }) {
                    it[reminderSent1h] = true
                    it[updatedAt] = Clock.System.now()
                }
            }
            "24h" -> {
                Bookings.update({ Bookings.id eq bookingId }) {
                    it[reminderSent24h] = true
                    it[updatedAt] = Clock.System.now()
                }
            }
            else -> 0
        }
        updated > 0
    }
}
