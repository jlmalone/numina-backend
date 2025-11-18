package com.numina.services.matching

import com.numina.common.utils.ScoreCalculator
import com.numina.domain.ClassMatch
import com.numina.domain.FitnessClass
import com.numina.domain.UserProfile
import org.slf4j.LoggerFactory

/**
 * User-to-class matching algorithm
 *
 * Weighted scoring:
 * - Fitness interests match: 35%
 * - Appropriate intensity: 25%
 * - Schedule fit: 20%
 * - Location convenience: 15%
 * - Price range: 5%
 */
class ClassMatcher {
    private val logger = LoggerFactory.getLogger(ClassMatcher::class.java)

    companion object {
        // Scoring weights (must sum to 1.0)
        private val WEIGHTS = mapOf(
            "interests" to 0.35,
            "intensity" to 0.25,
            "schedule" to 0.20,
            "location" to 0.15,
            "price" to 0.05
        )
    }

    /**
     * Calculate match score between user and a fitness class
     */
    fun calculateMatch(
        userProfile: UserProfile,
        fitnessClass: FitnessClass,
        maxDistanceKm: Double = 10.0,
        maxPrice: Double? = null
    ): ClassMatch {
        logger.debug("Calculating class match: user=${userProfile.userId} vs class=${fitnessClass.id}")

        val scores = mutableMapOf<String, Double>()

        // 1. Fitness interests match (35%)
        // Check if class tags overlap with user's fitness interests
        scores["interests"] = ScoreCalculator.calculateSharedInterestsScore(
            userProfile.fitnessInterests,
            fitnessClass.tags
        )

        // 2. Appropriate intensity (25%)
        scores["intensity"] = ScoreCalculator.calculateIntensityMatchScore(
            userProfile.fitnessLevel,
            fitnessClass.intensity
        )

        // 3. Schedule fit (20%)
        scores["schedule"] = ScoreCalculator.calculateClassTimeFitScore(
            fitnessClass.datetime,
            userProfile.availability
        )

        // 4. Location convenience (15%)
        val distance = if (userProfile.locationLat != null && userProfile.locationLong != null) {
            ScoreCalculator.calculateDistance(
                userProfile.locationLat,
                userProfile.locationLong,
                fitnessClass.locationLat,
                fitnessClass.locationLong
            )
        } else {
            null
        }

        scores["location"] = if (distance != null) {
            ScoreCalculator.calculateProximityScore(distance, maxDistanceKm)
        } else {
            50.0 // Neutral if location unknown
        }

        // 5. Price range (5%)
        scores["price"] = calculatePriceScore(fitnessClass.price, maxPrice)

        // Calculate weighted total score
        val matchScore = ScoreCalculator.calculateWeightedScore(scores, WEIGHTS)

        // Generate match reasons
        val matchReasons = ScoreCalculator.generateMatchReasons(scores)

        // Estimate fit category
        val estimatedFit = ScoreCalculator.estimateFit(matchScore)

        logger.debug("Class match calculated: score=$matchScore, fit=$estimatedFit")

        return ClassMatch(
            classId = fitnessClass.id,
            classDetails = fitnessClass,
            matchScore = matchScore,
            matchReasons = matchReasons,
            estimatedFit = estimatedFit
        )
    }

    /**
     * Filter and rank classes by match score
     */
    fun rankClasses(
        userProfile: UserProfile,
        classes: List<FitnessClass>,
        minScore: Int = 50,
        maxDistanceKm: Double = 10.0,
        maxPrice: Double? = null,
        limit: Int = 20
    ): List<ClassMatch> {
        logger.info("Ranking ${classes.size} classes for user ${userProfile.userId}")

        return classes
            .map { calculateMatch(userProfile, it, maxDistanceKm, maxPrice) }
            .filter { it.matchScore >= minScore }
            .sortedByDescending { it.matchScore }
            .take(limit)
            .also {
                logger.info("Ranked results: ${it.size} class matches above score $minScore")
            }
    }

    /**
     * Calculate price affordability score
     */
    private fun calculatePriceScore(classPrice: Double, maxPrice: Double?): Double {
        if (maxPrice == null || maxPrice <= 0) return 50.0 // Neutral if no budget set

        return when {
            classPrice <= maxPrice * 0.5 -> 100.0 // Great deal
            classPrice <= maxPrice * 0.75 -> 80.0 // Good value
            classPrice <= maxPrice -> 60.0 // Within budget
            classPrice <= maxPrice * 1.25 -> 30.0 // Slightly over budget
            else -> 0.0 // Too expensive
        }
    }
}
