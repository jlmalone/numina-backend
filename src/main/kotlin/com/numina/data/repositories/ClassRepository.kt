package com.numina.data.repositories

import com.numina.data.tables.Classes
import com.numina.domain.ClassFilters
import com.numina.domain.CreateClassRequest
import com.numina.domain.FitnessClass
import com.numina.domain.UpdateClassRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.*

interface ClassRepository {
    suspend fun createClass(request: CreateClassRequest): FitnessClass?
    suspend fun getClassById(id: Int): FitnessClass?
    suspend fun getClasses(filters: ClassFilters): List<FitnessClass>
    suspend fun updateClass(id: Int, request: UpdateClassRequest): FitnessClass?
    suspend fun deleteClass(id: Int): Boolean
}

class ClassRepositoryImpl : ClassRepository {
    private fun resultRowToClass(row: ResultRow): FitnessClass {
        return FitnessClass(
            id = row[Classes.id].value,
            name = row[Classes.name],
            description = row[Classes.description],
            datetime = row[Classes.datetime],
            locationLat = row[Classes.locationLat],
            locationLong = row[Classes.locationLong],
            trainer = row[Classes.trainer],
            intensity = row[Classes.intensity],
            price = row[Classes.price],
            externalBookingUrl = row[Classes.externalBookingUrl],
            providerId = row[Classes.providerId],
            capacity = row[Classes.capacity],
            tags = row[Classes.tags],
            createdAt = row[Classes.createdAt]
        )
    }

    override suspend fun createClass(request: CreateClassRequest): FitnessClass? = transaction {
        val now = Clock.System.now()

        val classId = Classes.insertAndGetId {
            it[name] = request.name
            it[description] = request.description
            it[datetime] = request.datetime
            it[locationLat] = request.locationLat
            it[locationLong] = request.locationLong
            it[trainer] = request.trainer
            it[intensity] = request.intensity
            it[price] = request.price
            it[externalBookingUrl] = request.externalBookingUrl
            it[providerId] = request.providerId
            it[capacity] = request.capacity
            it[tags] = request.tags
            it[createdAt] = now
        }

        Classes.select { Classes.id eq classId }
            .map { resultRowToClass(it) }
            .singleOrNull()
    }

    override suspend fun getClassById(id: Int): FitnessClass? = transaction {
        Classes.select { Classes.id eq id }
            .map { resultRowToClass(it) }
            .singleOrNull()
    }

    override suspend fun getClasses(filters: ClassFilters): List<FitnessClass> = transaction {
        var query = Classes.selectAll()

        // Apply date filters
        filters.startDate?.let { start ->
            query = query.andWhere { Classes.datetime greaterEq start }
        }
        filters.endDate?.let { end ->
            query = query.andWhere { Classes.datetime lessEq end }
        }

        // Apply price filters
        filters.minPrice?.let { min ->
            query = query.andWhere { Classes.price greaterEq min }
        }
        filters.maxPrice?.let { max ->
            query = query.andWhere { Classes.price lessEq max }
        }

        // Apply intensity filters
        filters.minIntensity?.let { min ->
            query = query.andWhere { Classes.intensity greaterEq min }
        }
        filters.maxIntensity?.let { max ->
            query = query.andWhere { Classes.intensity lessEq max }
        }

        val results = query.map { resultRowToClass(it) }

        // Apply location filtering (radius in km)
        if (filters.locationLat != null && filters.locationLong != null && filters.radiusKm != null) {
            results.filter { fitnessClass ->
                val distance = calculateDistance(
                    filters.locationLat,
                    filters.locationLong,
                    fitnessClass.locationLat,
                    fitnessClass.locationLong
                )
                distance <= filters.radiusKm
            }
        } else {
            results
        }
    }

    override suspend fun updateClass(id: Int, request: UpdateClassRequest): FitnessClass? = transaction {
        Classes.update({ Classes.id eq id }) {
            request.name?.let { name -> it[Classes.name] = name }
            request.description?.let { desc -> it[description] = desc }
            request.datetime?.let { dt -> it[datetime] = dt }
            request.locationLat?.let { lat -> it[locationLat] = lat }
            request.locationLong?.let { long -> it[locationLong] = long }
            request.trainer?.let { t -> it[trainer] = t }
            request.intensity?.let { i -> it[intensity] = i }
            request.price?.let { p -> it[price] = p }
            request.externalBookingUrl?.let { url -> it[externalBookingUrl] = url }
            request.providerId?.let { pid -> it[providerId] = pid }
            request.capacity?.let { cap -> it[capacity] = cap }
            request.tags?.let { t -> it[tags] = t }
        }

        Classes.select { Classes.id eq id }
            .map { resultRowToClass(it) }
            .singleOrNull()
    }

    override suspend fun deleteClass(id: Int): Boolean = transaction {
        Classes.deleteWhere { Classes.id eq id } > 0
    }

    // Calculate distance between two points using Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }
}
