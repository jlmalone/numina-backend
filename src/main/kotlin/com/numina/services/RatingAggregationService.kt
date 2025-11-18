package com.numina.services

import com.numina.data.repositories.RatingAggregateRepository
import com.numina.data.repositories.ReviewRepository
import com.numina.data.tables.Reviews
import com.numina.domain.EntityType
import com.numina.domain.RatingAggregate
import com.numina.domain.RatingSummary
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

interface RatingAggregationService {
    suspend fun updateRatingsForClass(classId: Int): RatingAggregate?
    suspend fun updateRatingsForTrainer(trainerId: String): RatingAggregate?
    suspend fun updateRatingsForProvider(providerId: String): RatingAggregate?
    suspend fun getRatingSummaryForClass(classId: Int): RatingSummary
    suspend fun getRatingSummaryForTrainer(trainerId: String): RatingSummary
    suspend fun getRatingSummaryForProvider(providerId: String): RatingSummary
}

class RatingAggregationServiceImpl(
    private val ratingAggregateRepository: RatingAggregateRepository,
    private val reviewRepository: ReviewRepository
) : RatingAggregationService {
    private val logger = LoggerFactory.getLogger(RatingAggregationServiceImpl::class.java)

    override suspend fun updateRatingsForClass(classId: Int): RatingAggregate? {
        logger.debug("Updating ratings for classId=$classId")

        val (avgRating, totalReviews, distribution) = calculateRatingsForClass(classId)

        if (totalReviews == 0) {
            // No reviews, delete aggregate if exists
            ratingAggregateRepository.deleteAggregate(EntityType.CLASS, classId.toString())
            return null
        }

        return ratingAggregateRepository.updateAggregate(
            entityType = EntityType.CLASS,
            entityId = classId.toString(),
            averageRating = avgRating,
            totalReviews = totalReviews,
            ratingDistribution = distribution
        )
    }

    override suspend fun updateRatingsForTrainer(trainerId: String): RatingAggregate? {
        logger.debug("Updating ratings for trainerId=$trainerId")

        val (avgRating, totalReviews, distribution) = calculateRatingsForTrainer(trainerId)

        if (totalReviews == 0) {
            // No reviews, delete aggregate if exists
            ratingAggregateRepository.deleteAggregate(EntityType.TRAINER, trainerId)
            return null
        }

        return ratingAggregateRepository.updateAggregate(
            entityType = EntityType.TRAINER,
            entityId = trainerId,
            averageRating = avgRating,
            totalReviews = totalReviews,
            ratingDistribution = distribution
        )
    }

    override suspend fun updateRatingsForProvider(providerId: String): RatingAggregate? {
        logger.debug("Updating ratings for providerId=$providerId")

        val (avgRating, totalReviews, distribution) = calculateRatingsForProvider(providerId)

        if (totalReviews == 0) {
            // No reviews, delete aggregate if exists
            ratingAggregateRepository.deleteAggregate(EntityType.PROVIDER, providerId)
            return null
        }

        return ratingAggregateRepository.updateAggregate(
            entityType = EntityType.PROVIDER,
            entityId = providerId,
            averageRating = avgRating,
            totalReviews = totalReviews,
            ratingDistribution = distribution
        )
    }

    override suspend fun getRatingSummaryForClass(classId: Int): RatingSummary {
        logger.debug("Getting rating summary for classId=$classId")

        val aggregate = ratingAggregateRepository.getAggregate(EntityType.CLASS, classId.toString())
        val recentReviews = reviewRepository.getReviewsByClassId(classId, limit = 5, offset = 0)

        return if (aggregate != null) {
            RatingSummary(
                averageRating = aggregate.averageRating,
                totalReviews = aggregate.totalReviews,
                ratingDistribution = aggregate.ratingDistribution,
                recentReviews = recentReviews
            )
        } else {
            // No aggregate, calculate on-the-fly
            val (avgRating, totalReviews, distribution) = calculateRatingsForClass(classId)
            RatingSummary(
                averageRating = avgRating,
                totalReviews = totalReviews,
                ratingDistribution = distribution,
                recentReviews = recentReviews
            )
        }
    }

    override suspend fun getRatingSummaryForTrainer(trainerId: String): RatingSummary {
        logger.debug("Getting rating summary for trainerId=$trainerId")

        val aggregate = ratingAggregateRepository.getAggregate(EntityType.TRAINER, trainerId)
        val recentReviews = reviewRepository.getReviewsByTrainerId(trainerId, limit = 5, offset = 0)

        return if (aggregate != null) {
            RatingSummary(
                averageRating = aggregate.averageRating,
                totalReviews = aggregate.totalReviews,
                ratingDistribution = aggregate.ratingDistribution,
                recentReviews = recentReviews
            )
        } else {
            // No aggregate, calculate on-the-fly
            val (avgRating, totalReviews, distribution) = calculateRatingsForTrainer(trainerId)
            RatingSummary(
                averageRating = avgRating,
                totalReviews = totalReviews,
                ratingDistribution = distribution,
                recentReviews = recentReviews
            )
        }
    }

    override suspend fun getRatingSummaryForProvider(providerId: String): RatingSummary {
        logger.debug("Getting rating summary for providerId=$providerId")

        val aggregate = ratingAggregateRepository.getAggregate(EntityType.PROVIDER, providerId)
        val recentReviews = reviewRepository.getReviewsByProviderId(providerId, limit = 5, offset = 0)

        return if (aggregate != null) {
            RatingSummary(
                averageRating = aggregate.averageRating,
                totalReviews = aggregate.totalReviews,
                ratingDistribution = aggregate.ratingDistribution,
                recentReviews = recentReviews
            )
        } else {
            // No aggregate, calculate on-the-fly
            val (avgRating, totalReviews, distribution) = calculateRatingsForProvider(providerId)
            RatingSummary(
                averageRating = avgRating,
                totalReviews = totalReviews,
                ratingDistribution = distribution,
                recentReviews = recentReviews
            )
        }
    }

    private data class RatingStats(
        val averageRating: Double,
        val totalReviews: Int,
        val distribution: Map<Int, Int>
    )

    private fun calculateRatingsForClass(classId: Int): RatingStats = transaction {
        val reviews = Reviews.select { Reviews.classId eq classId }
            .map { it[Reviews.rating] }

        calculateRatingStats(reviews)
    }

    private fun calculateRatingsForTrainer(trainerId: String): RatingStats = transaction {
        val reviews = Reviews.select { Reviews.trainerId eq trainerId }
            .map { it[Reviews.rating] }

        calculateRatingStats(reviews)
    }

    private fun calculateRatingsForProvider(providerId: String): RatingStats = transaction {
        val reviews = Reviews.select { Reviews.providerId eq providerId }
            .map { it[Reviews.rating] }

        calculateRatingStats(reviews)
    }

    private fun calculateRatingStats(ratings: List<Int>): RatingStats {
        if (ratings.isEmpty()) {
            return RatingStats(
                averageRating = 0.0,
                totalReviews = 0,
                distribution = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
            )
        }

        val totalReviews = ratings.size
        val sum = ratings.sum()
        val averageRating = (sum.toDouble() / totalReviews * 10).roundToInt() / 10.0

        val distribution = mutableMapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
        ratings.groupingBy { it }.eachCount().forEach { (rating, count) ->
            distribution[rating] = count
        }

        return RatingStats(
            averageRating = averageRating,
            totalReviews = totalReviews,
            distribution = distribution
        )
    }
}
