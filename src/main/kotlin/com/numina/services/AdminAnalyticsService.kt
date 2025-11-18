package com.numina.services

import com.numina.data.repositories.UserRepository
import com.numina.data.repositories.ClassRepository
import org.slf4j.LoggerFactory

interface AdminAnalyticsService {
    suspend fun getUserMetrics(): Map<String, Any>
    suspend fun getClassMetrics(): Map<String, Any>
    suspend fun getEngagementMetrics(): Map<String, Any>
    suspend fun exportData(format: String = "json"): String
}

class AdminAnalyticsServiceImpl(
    private val userRepository: UserRepository,
    private val classRepository: ClassRepository
) : AdminAnalyticsService {
    private val logger = LoggerFactory.getLogger(AdminAnalyticsServiceImpl::class.java)

    override suspend fun getUserMetrics(): Map<String, Any> {
        logger.debug("Generating user metrics")

        val totalUsers = userRepository.getUserCount()
        val users = userRepository.getAllUsers(limit = 1000)

        // Calculate growth metrics
        val now = kotlinx.datetime.Clock.System.now()
        val thirtyDaysAgo = now.minus(kotlinx.datetime.DateTimePeriod(days = 30), kotlinx.datetime.TimeZone.UTC)
        val sevenDaysAgo = now.minus(kotlinx.datetime.DateTimePeriod(days = 7), kotlinx.datetime.TimeZone.UTC)

        val newUsersLast30Days = users.count { it.createdAt >= thirtyDaysAgo }
        val newUsersLast7Days = users.count { it.createdAt >= sevenDaysAgo }

        return mapOf(
            "totalUsers" to totalUsers,
            "newUsersLast30Days" to newUsersLast30Days,
            "newUsersLast7Days" to newUsersLast7Days,
            "activeUsers" to totalUsers // Placeholder - would calculate actual active users
        )
    }

    override suspend fun getClassMetrics(): Map<String, Any> {
        logger.debug("Generating class metrics")

        val allClasses = classRepository.getAllClasses(limit = 10000, offset = 0)

        return mapOf(
            "totalClasses" to allClasses.size,
            "totalProviders" to allClasses.distinctBy { it.providerId }.size,
            "averageClassesPerProvider" to if (allClasses.isNotEmpty()) {
                allClasses.size.toDouble() / allClasses.distinctBy { it.providerId }.size
            } else 0.0,
            "popularActivities" to allClasses.groupBy { it.activityType }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(5)
                .associate { it.key to it.value }
        )
    }

    override suspend fun getEngagementMetrics(): Map<String, Any> {
        logger.debug("Generating engagement metrics")

        // Placeholder metrics - would calculate from actual engagement data
        return mapOf(
            "avgSessionDuration" to "15 minutes",
            "totalBookings" to 0, // Placeholder
            "totalReviews" to 0, // Placeholder
            "totalMessages" to 0, // Placeholder
            "bookingConversionRate" to "0%"
        )
    }

    override suspend fun exportData(format: String): String {
        logger.info("Exporting data in format: $format")

        when (format.lowercase()) {
            "json" -> {
                val data = mapOf(
                    "users" to getUserMetrics(),
                    "classes" to getClassMetrics(),
                    "engagement" to getEngagementMetrics(),
                    "exportedAt" to kotlinx.datetime.Clock.System.now().toString()
                )
                return kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.serializer(),
                    data
                )
            }
            "csv" -> {
                // Simple CSV export
                val userMetrics = getUserMetrics()
                val classMetrics = getClassMetrics()

                return buildString {
                    appendLine("Metric,Value")
                    appendLine("Total Users,${userMetrics["totalUsers"]}")
                    appendLine("New Users (30d),${userMetrics["newUsersLast30Days"]}")
                    appendLine("New Users (7d),${userMetrics["newUsersLast7Days"]}")
                    appendLine("Total Classes,${classMetrics["totalClasses"]}")
                    appendLine("Total Providers,${classMetrics["totalProviders"]}")
                }
            }
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
    }
}
