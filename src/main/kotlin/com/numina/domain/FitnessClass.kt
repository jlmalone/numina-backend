package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class FitnessClass(
    val id: Int,
    val name: String,
    val description: String,
    val datetime: Instant,
    val locationLat: Double,
    val locationLong: Double,
    val trainer: String? = null,
    val intensity: Int, // 1-10 scale
    val price: Double,
    val externalBookingUrl: String? = null,
    val providerId: String? = null,
    val capacity: Int,
    val tags: List<String> = emptyList(),
    val createdAt: Instant
)

@Serializable
data class CreateClassRequest(
    val name: String,
    val description: String,
    val datetime: Instant,
    val locationLat: Double,
    val locationLong: Double,
    val trainer: String? = null,
    val intensity: Int,
    val price: Double,
    val externalBookingUrl: String? = null,
    val providerId: String? = null,
    val capacity: Int,
    val tags: List<String> = emptyList()
)

@Serializable
data class UpdateClassRequest(
    val name: String? = null,
    val description: String? = null,
    val datetime: Instant? = null,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val trainer: String? = null,
    val intensity: Int? = null,
    val price: Double? = null,
    val externalBookingUrl: String? = null,
    val providerId: String? = null,
    val capacity: Int? = null,
    val tags: List<String>? = null
)

@Serializable
data class ClassFilters(
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val radiusKm: Double? = null,
    val startDate: Instant? = null,
    val endDate: Instant? = null,
    val type: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minIntensity: Int? = null,
    val maxIntensity: Int? = null,
    val tags: List<String>? = null
)
