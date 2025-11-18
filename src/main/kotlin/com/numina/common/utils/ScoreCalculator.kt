package com.numina.common.utils

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for calculating weighted match scores
 */
object ScoreCalculator {

    /**
     * Calculate fitness level similarity score (0-100)
     * Best match: ±2 levels, degrades outside this range
     */
    fun calculateFitnessLevelScore(userLevel: Int?, targetLevel: Int?): Double {
        if (userLevel == null || targetLevel == null) return 50.0 // neutral score

        val difference = abs(userLevel - targetLevel)
        return when {
            difference == 0 -> 100.0
            difference == 1 -> 90.0
            difference == 2 -> 80.0
            difference == 3 -> 60.0
            difference == 4 -> 40.0
            difference == 5 -> 20.0
            else -> 0.0
        }
    }

    /**
     * Calculate shared interests score (0-100)
     */
    fun calculateSharedInterestsScore(userInterests: List<String>, targetInterests: List<String>): Double {
        if (userInterests.isEmpty() || targetInterests.isEmpty()) return 0.0

        val shared = userInterests.intersect(targetInterests.toSet()).size
        val total = userInterests.union(targetInterests.toSet()).size

        // Jaccard similarity * 100
        return if (total > 0) (shared.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    /**
     * Calculate proximity score based on distance (0-100)
     * Distance in kilometers
     */
    fun calculateProximityScore(distanceKm: Double, maxDistanceKm: Double = 10.0): Double {
        if (distanceKm <= 0) return 100.0
        if (distanceKm >= maxDistanceKm) return 0.0

        // Linear decay from 100 to 0 over maxDistanceKm
        return ((maxDistanceKm - distanceKm) / maxDistanceKm) * 100.0
    }

    /**
     * Calculate schedule compatibility score (0-100)
     * Based on overlapping availability slots
     */
    fun calculateScheduleCompatibilityScore(
        userAvailability: Map<String, List<String>>,
        targetAvailability: Map<String, List<String>>
    ): Double {
        if (userAvailability.isEmpty() || targetAvailability.isEmpty()) return 50.0 // neutral

        var matchingSlots = 0
        var totalSlots = 0

        // Days of the week
        val allDays = userAvailability.keys.union(targetAvailability.keys)

        for (day in allDays) {
            val userSlots = userAvailability[day] ?: emptyList()
            val targetSlots = targetAvailability[day] ?: emptyList()

            if (userSlots.isNotEmpty() || targetSlots.isNotEmpty()) {
                totalSlots++
                if (userSlots.intersect(targetSlots.toSet()).isNotEmpty()) {
                    matchingSlots++
                }
            }
        }

        return if (totalSlots > 0) (matchingSlots.toDouble() / totalSlots.toDouble()) * 100.0 else 0.0
    }

    /**
     * Calculate class time fit score (0-100)
     * Checks if class time matches user's availability
     */
    fun calculateClassTimeFitScore(
        classDateTime: kotlinx.datetime.Instant,
        userAvailability: Map<String, List<String>>
    ): Double {
        // For simplicity, we'll check day of week and time slot
        // In a real implementation, you'd parse the datetime properly
        // This is a simplified version
        if (userAvailability.isEmpty()) return 50.0 // neutral

        // TODO: Implement proper datetime to day/time slot conversion
        // For now, return a neutral score
        return 70.0 // Assume reasonable fit
    }

    /**
     * Calculate intensity match score (0-100)
     * Class intensity should be appropriate for user's fitness level
     */
    fun calculateIntensityMatchScore(userFitnessLevel: Int?, classIntensity: Int): Double {
        if (userFitnessLevel == null) return 50.0

        // Best match: class intensity matches user level ± 1
        val difference = abs(userFitnessLevel - classIntensity)
        return when {
            difference == 0 -> 100.0
            difference == 1 -> 85.0
            difference == 2 -> 65.0
            difference == 3 -> 45.0
            difference == 4 -> 25.0
            else -> 10.0
        }
    }

    /**
     * Calculate weighted total score from component scores
     */
    fun calculateWeightedScore(scores: Map<String, Double>, weights: Map<String, Double>): Int {
        var totalScore = 0.0
        var totalWeight = 0.0

        for ((component, weight) in weights) {
            val score = scores[component] ?: 0.0
            totalScore += score * weight
            totalWeight += weight
        }

        return if (totalWeight > 0) {
            (totalScore / totalWeight).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    /**
     * Generate match reasons based on score breakdown
     */
    fun generateMatchReasons(
        scores: Map<String, Double>,
        thresholdHigh: Double = 75.0,
        thresholdMedium: Double = 50.0
    ): List<String> {
        val reasons = mutableListOf<String>()

        scores.forEach { (component, score) ->
            when {
                score >= thresholdHigh -> {
                    reasons.add(formatHighScore(component))
                }
                score >= thresholdMedium -> {
                    reasons.add(formatMediumScore(component))
                }
            }
        }

        return reasons.ifEmpty { listOf("Basic compatibility") }
    }

    private fun formatHighScore(component: String): String = when (component) {
        "fitnessLevel" -> "Very similar fitness levels"
        "interests" -> "Many shared fitness interests"
        "proximity" -> "Lives nearby"
        "schedule" -> "Highly compatible schedules"
        "intensity" -> "Perfect intensity match for your level"
        "location" -> "Conveniently located"
        else -> "Strong match in $component"
    }

    private fun formatMediumScore(component: String): String = when (component) {
        "fitnessLevel" -> "Compatible fitness levels"
        "interests" -> "Some shared fitness interests"
        "proximity" -> "Within your area"
        "schedule" -> "Some schedule overlap"
        "intensity" -> "Appropriate intensity"
        "location" -> "Accessible location"
        else -> "Good match in $component"
    }

    /**
     * Estimate fit category based on score
     */
    fun estimateFit(score: Int): String = when {
        score >= 80 -> "perfect"
        score >= 60 -> "good"
        else -> "okay"
    }

    /**
     * Calculate Haversine distance between two points (in kilometers)
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return earthRadiusKm * c
    }
}
