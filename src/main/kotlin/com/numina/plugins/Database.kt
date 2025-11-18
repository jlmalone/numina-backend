package com.numina.plugins

import com.numina.data.tables.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val databaseType = environment.config.propertyOrNull("database.type")?.getString() ?: "h2"
    val database = when (databaseType) {
        "postgresql" -> {
            val jdbcUrl = environment.config.property("database.jdbcUrl").getString()
            val user = environment.config.property("database.user").getString()
            val password = environment.config.property("database.password").getString()
            Database.connect(
                url = jdbcUrl,
                driver = "org.postgresql.Driver",
                user = user,
                password = password
            )
        }
        else -> {
            // H2 in-memory database for development
            Database.connect(
                url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver"
            )
        }
    }

    transaction(database) {
        SchemaUtils.create(Users, UserProfiles, Classes, RefreshTokens, Bookings, BookingReminders, AttendanceStats)
    }
}
