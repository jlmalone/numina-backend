package com.numina.plugins

import com.numina.services.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

fun Application.configureBackgroundJobs() {
    val logger = LoggerFactory.getLogger("BackgroundJobs")
    val reminderService by inject<ReminderService>()
    val attendanceStatsService by inject<AttendanceStatsService>()
    val notificationService by inject<NotificationService>()
    val ratingAggregateService by inject<RatingAggregateService>()
    val activityFeedService by inject<ActivityFeedService>()

    // Launch coroutine for background jobs
    launch {
        // Notification batch processor - runs every 30 seconds for batching
        launch {
            delay(30.minutes) // Initial delay
            while (isActive) {
                try {
                    logger.info("Running notification batch processor...")
                    notificationService.processBatchNotifications()
                    logger.info("Notification batch processor completed")
                } catch (e: Exception) {
                    logger.error("Error in notification batch processor", e)
                }
                delay(30.minutes)
            }
        }

        // Rating aggregates updater - runs every 15 minutes
        launch {
            delay(2.minutes) // Initial delay
            while (isActive) {
                try {
                    logger.info("Running rating aggregates updater...")
                    ratingAggregateService.updateAllAggregates()
                    logger.info("Rating aggregates updater completed")
                } catch (e: Exception) {
                    logger.error("Error in rating aggregates updater", e)
                }
                delay(15.minutes)
            }
        }

        // Feed generator - runs every 5 minutes
        launch {
            delay(1.minutes) // Initial delay
            while (isActive) {
                try {
                    logger.info("Running feed generator...")
                    activityFeedService.regenerateFeedCache()
                    logger.info("Feed generator completed")
                } catch (e: Exception) {
                    logger.error("Error in feed generator", e)
                }
                delay(5.minutes)
            }
        }

        // Reminder processor - runs every hour
        launch {
            delay(5.minutes) // Initial delay
            while (isActive) {
                try {
                    logger.info("Running reminder processor...")
                    reminderService.processReminders(this@configureBackgroundJobs)
                    logger.info("Reminder processor completed")
                } catch (e: Exception) {
                    logger.error("Error in reminder processor", e)
                }
                delay(1.hours)
            }
        }

        // Stats updater - runs every 24 hours
        launch {
            delay(10.minutes) // Initial delay
            while (isActive) {
                try {
                    logger.info("Running stats updater...")
                    attendanceStatsService.updateAllStats()
                    logger.info("Stats updater completed")
                } catch (e: Exception) {
                    logger.error("Error in stats updater", e)
                }
                delay(24.hours)
            }
        }
    }

    logger.info("Background jobs configured and started")
}
