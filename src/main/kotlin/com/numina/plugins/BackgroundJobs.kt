package com.numina.plugins

import com.numina.services.ReminderService
import com.numina.services.AttendanceStatsService
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun Application.configureBackgroundJobs() {
    val reminderService by inject<ReminderService>()
    val attendanceStatsService by inject<AttendanceStatsService>()

    // Launch coroutine for background jobs
    launch {
        // Reminder processor - runs every hour
        launch {
            while (isActive) {
                try {
                    log.info("Running reminder processor...")
                    reminderService.processReminders(this@configureBackgroundJobs)
                    log.info("Reminder processor completed")
                } catch (e: Exception) {
                    log.error("Error in reminder processor", e)
                }
                delay(1.hours)
            }
        }

        // Stats updater - runs every 24 hours
        launch {
            // Wait 1 minute before starting to allow server to fully initialize
            delay(1.minutes)

            while (isActive) {
                try {
                    log.info("Running stats updater...")
                    attendanceStatsService.updateAllStats()
                    log.info("Stats updater completed")
                } catch (e: Exception) {
                    log.error("Error in stats updater", e)
                }
                delay(24.hours)
            }
        }

        // Streak calculator - runs every 24 hours
        // This is now handled within the stats updater
        // Individual streak updates happen when users mark attendance
    }

    log.info("Background jobs configured and started")
}
