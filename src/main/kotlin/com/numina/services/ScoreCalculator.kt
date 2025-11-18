package com.numina.services

import com.numina.domain.FitnessClass
import com.numina.domain.UserProfile
import kotlin.math.*

/**
 * Service for calculating match scores between users and between users and classes.
 * All scores are on a 0-100 scale.
 */
interface ScoreCalculator {
    /**
     * Calculate match score between two users for workout partnership.
     * Factors (weighted):
     * - Fitness level similarity (20%): ±2 levels is ideal
     * - Shared fitness interests (30%): overlap in interests
     * - Geographic proximity (25%): within reasonable distance
     * - Schedule compatibility (20%): overlapping availability
     * - Past interactions (5%): positive ratings, previous partnerships
     */
    fun calculateUserMatchScore(
        user: UserProfile,
        otherUser: UserProfile,
        distanceKm: Float?,
        hasPreviousInteraction: Boolean = false
    ): MatchScoreResult

    /**
     * Calculate match score between user and class.
     * Factors (weighted):
     * - Fitness interests match (35%): class type matches user interests
     * - Appropriate intensity (25%): class intensity aligns with user level
     * - Schedule fit (20%): class time matches user availability
     * - Location convenience (15%): class within user's preferred radius
     * - Price range (5%): within user's budget preferences
     */
    fun calculateClassMatchScore(
        user: UserProfile,
        fitnessClass: FitnessClass,
        distanceKm: Float?,
        userMaxDistance: Float = 10.0f,
        userMaxPrice: Double? = null
    ): MatchScoreResult

    /**
     * Calculate distance in km between two coordinates using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float
}

data class MatchScoreResult(
    val score: Int, // 0-100
    val reasons: List<String>,
    val breakdown: Map<String, Int> = emptyMap() // For debugging/transparency
)

class ScoreCalculatorImpl : ScoreCalculator {
    companion object {
        // User-to-User weights
        const val WEIGHT_FITNESS_LEVEL = 0.20f
        const val WEIGHT_SHARED_INTERESTS = 0.30f
        const val WEIGHT_PROXIMITY = 0.25f
        const val WEIGHT_SCHEDULE = 0.20f
        const val WEIGHT_INTERACTIONS = 0.05f

        // User-to-Class weights
        const val WEIGHT_CLASS_INTERESTS = 0.35f
        const val WEIGHT_CLASS_INTENSITY = 0.25f
        const val WEIGHT_CLASS_SCHEDULE = 0.20f
        const val WEIGHT_CLASS_LOCATION = 0.15f
        const val WEIGHT_CLASS_PRICE = 0.05f

        // Earth radius in km
        const val EARTH_RADIUS_KM = 6371.0
    }

    override fun calculateUserMatchScore(
        user: UserProfile,
        otherUser: UserProfile,
        distanceKm: Float?,
        hasPreviousInteraction: Boolean
    ): MatchScoreResult {
        val reasons = mutableListOf<String>()
        val breakdown = mutableMapOf<String, Int>()

        // 1. Fitness level similarity (20%)
        val fitnessScore = if (user.fitnessLevel != null && otherUser.fitnessLevel != null) {
            val diff = abs(user.fitnessLevel - otherUser.fitnessLevel)
            when {
                diff == 0 -> {
                    reasons.add("Same fitness level (${user.fitnessLevel}/10)")
                    100
                }
                diff <= 2 -> {
                    reasons.add("Similar fitness levels (${user.fitnessLevel} vs ${otherUser.fitnessLevel})")
                    max(0, 100 - (diff * 25))
                }
                else -> max(0, 100 - (diff * 30))
            }
        } else {
            50 // Neutral score if levels unknown
        }
        breakdown["fitnessLevel"] = fitnessScore

        // 2. Shared interests (30%)
        val sharedInterests = user.fitnessInterests.intersect(otherUser.fitnessInterests.toSet())
        val interestsScore = when {
            sharedInterests.isEmpty() -> 0
            sharedInterests.size >= 3 -> {
                reasons.add("${sharedInterests.size} shared interests: ${sharedInterests.take(3).joinToString(", ")}")
                100
            }
            else -> {
                reasons.add("${sharedInterests.size} shared interests: ${sharedInterests.joinToString(", ")}")
                (sharedInterests.size * 50).coerceAtMost(100)
            }
        }
        breakdown["sharedInterests"] = interestsScore

        // 3. Geographic proximity (25%)
        val proximityScore = if (distanceKm != null) {
            when {
                distanceKm <= 2 -> {
                    reasons.add("Very close (${String.format("%.1f", distanceKm)}km away)")
                    100
                }
                distanceKm <= 5 -> {
                    reasons.add("Nearby (${String.format("%.1f", distanceKm)}km away)")
                    80
                }
                distanceKm <= 10 -> {
                    reasons.add("Within ${String.format("%.0f", distanceKm)}km")
                    60
                }
                distanceKm <= 20 -> 40
                else -> 20
            }
        } else {
            50 // Neutral if location unknown
        }
        breakdown["proximity"] = proximityScore

        // 4. Schedule compatibility (20%)
        val scheduleScore = calculateScheduleCompatibility(user.availability, otherUser.availability, reasons)
        breakdown["schedule"] = scheduleScore

        // 5. Past interactions (5%)
        val interactionScore = if (hasPreviousInteraction) {
            reasons.add("You've worked out together before")
            100
        } else {
            0
        }
        breakdown["interactions"] = interactionScore

        // Calculate weighted total
        val totalScore = (
            fitnessScore * WEIGHT_FITNESS_LEVEL +
            interestsScore * WEIGHT_SHARED_INTERESTS +
            proximityScore * WEIGHT_PROXIMITY +
            scheduleScore * WEIGHT_SCHEDULE +
            interactionScore * WEIGHT_INTERACTIONS
        ).toInt()

        return MatchScoreResult(
            score = totalScore.coerceIn(0, 100),
            reasons = reasons,
            breakdown = breakdown
        )
    }

    override fun calculateClassMatchScore(
        user: UserProfile,
        fitnessClass: FitnessClass,
        distanceKm: Float?,
        userMaxDistance: Float,
        userMaxPrice: Double?
    ): MatchScoreResult {
        val reasons = mutableListOf<String>()
        val breakdown = mutableMapOf<String, Int>()

        // 1. Fitness interests match (35%)
        val interestsScore = if (user.fitnessInterests.isNotEmpty()) {
            val matchingTags = user.fitnessInterests.intersect(fitnessClass.tags.toSet())
            when {
                matchingTags.isNotEmpty() -> {
                    reasons.add("Matches your interests: ${matchingTags.take(2).joinToString(", ")}")
                    min(100, matchingTags.size * 50)
                }
                else -> 30 // Some base score even without explicit matches
            }
        } else {
            50 // Neutral if no interests specified
        }
        breakdown["interests"] = interestsScore

        // 2. Appropriate intensity (25%)
        val intensityScore = if (user.fitnessLevel != null) {
            val diff = abs(user.fitnessLevel - fitnessClass.intensity)
            when {
                diff == 0 -> {
                    reasons.add("Perfect intensity match (${fitnessClass.intensity}/10)")
                    100
                }
                diff <= 1 -> {
                    reasons.add("Great intensity level for you")
                    85
                }
                diff <= 2 -> {
                    reasons.add("Good intensity match")
                    70
                }
                diff <= 3 -> 50
                else -> 30
            }
        } else {
            50 // Neutral if level unknown
        }
        breakdown["intensity"] = intensityScore

        // 3. Schedule fit (20%) - simplified for now
        // In real implementation, would check if class time matches user's availability
        val scheduleScore = 75 // Placeholder
        breakdown["schedule"] = scheduleScore

        // 4. Location convenience (15%)
        val locationScore = if (distanceKm != null) {
            val maxDist = userMaxDistance
            when {
                distanceKm <= maxDist * 0.3 -> {
                    reasons.add("Very convenient location (${String.format("%.1f", distanceKm)}km)")
                    100
                }
                distanceKm <= maxDist * 0.6 -> {
                    reasons.add("Nearby (${String.format("%.1f", distanceKm)}km)")
                    80
                }
                distanceKm <= maxDist -> 60
                distanceKm <= maxDist * 1.5 -> 40
                else -> 20
            }
        } else {
            50 // Neutral if location unknown
        }
        breakdown["location"] = locationScore

        // 5. Price range (5%)
        val priceScore = if (userMaxPrice != null) {
            when {
                fitnessClass.price <= userMaxPrice * 0.7 -> 100
                fitnessClass.price <= userMaxPrice -> {
                    reasons.add("Within your budget")
                    80
                }
                fitnessClass.price <= userMaxPrice * 1.2 -> 50
                else -> 20
            }
        } else {
            50 // Neutral if no budget specified
        }
        breakdown["price"] = priceScore

        // Calculate weighted total
        val totalScore = (
            interestsScore * WEIGHT_CLASS_INTERESTS +
            intensityScore * WEIGHT_CLASS_INTENSITY +
            scheduleScore * WEIGHT_CLASS_SCHEDULE +
            locationScore * WEIGHT_CLASS_LOCATION +
            priceScore * WEIGHT_CLASS_PRICE
        ).toInt()

        return MatchScoreResult(
            score = totalScore.coerceIn(0, 100),
            reasons = reasons,
            breakdown = breakdown
        )
    }

    private fun calculateScheduleCompatibility(
        availability1: Map<String, List<String>>,
        availability2: Map<String, List<String>>,
        reasons: MutableList<String>
    ): Int {
        if (availability1.isEmpty() || availability2.isEmpty()) {
            return 50 // Neutral if schedules unknown
        }

        var overlappingSlots = 0
        val overlappingDays = mutableListOf<String>()

        for ((day, times1) in availability1) {
            val times2 = availability2[day] ?: continue
            val overlap = times1.intersect(times2.toSet())
            if (overlap.isNotEmpty()) {
                overlappingSlots += overlap.size
                overlappingDays.add(day)
            }
        }

        return when {
            overlappingSlots >= 5 -> {
                reasons.add("Great schedule overlap (${overlappingDays.size} days)")
                100
            }
            overlappingSlots >= 3 -> {
                reasons.add("Good schedule compatibility")
                75
            }
            overlappingSlots >= 1 -> {
                reasons.add("Some schedule overlap")
                50
            }
            else -> 25
        }
    }

    override fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        // Haversine formula
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EARTH_RADIUS_KM * c).toFloat()
    }
}
