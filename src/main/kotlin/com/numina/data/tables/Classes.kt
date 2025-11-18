package com.numina.data.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Classes : IntIdTable("classes") {
    val name = varchar("name", 255)
    val description = text("description")
    val datetime = timestamp("datetime").index()
    val locationLat = double("location_lat")
    val locationLong = double("location_long")
    val trainer = varchar("trainer", 255).nullable()
    val intensity = integer("intensity")
    val price = double("price")
    val externalBookingUrl = varchar("external_booking_url", 500).nullable()
    val providerId = varchar("provider_id", 100).nullable().index()
    val capacity = integer("capacity")
    val tags = jsonb<List<String>>("tags", Json.Default)
    val createdAt = timestamp("created_at")
}

// TODO: Future tables to implement
// - Matches: For user matching algorithm
// - Messages: For user-to-user messaging
// - Ratings: For class and user ratings/reviews
// - ClassAttendance: Track who's attending which classes
// - UserConnections: Friend/connection relationships
