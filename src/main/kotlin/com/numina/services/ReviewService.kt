package com.numina.services

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.ReviewRepository
import com.numina.domain.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

interface ReviewService {
    suspend fun createReview(userId: Int, request: CreateReviewRequest): Review
    suspend fun getReviewById(id: Int): Review
    suspend fun getReviewsByClassId(classId: Int, page: Int = 1, pageSize: Int = 20): List<Review>
    suspend fun getReviewsByTrainerId(trainerId: String, page: Int = 1, pageSize: Int = 20): List<Review>
    suspend fun getReviewsByProviderId(providerId: String, page: Int = 1, pageSize: Int = 20): List<Review>
    suspend fun getMyReviews(userId: Int): List<Review>
    suspend fun updateReview(id: Int, userId: Int, request: UpdateReviewRequest): Review
    suspend fun deleteReview(id: Int, userId: Int): Boolean
    suspend fun voteHelpful(reviewId: Int, userId: Int): Boolean
    suspend fun voteNotHelpful(reviewId: Int, userId: Int): Boolean
    suspend fun removeVote(reviewId: Int, userId: Int): Boolean
    suspend fun reportReview(reviewId: Int, userId: Int, request: CreateReviewReportRequest): ReviewReport
}

class ReviewServiceImpl(
    private val reviewRepository: ReviewRepository,
    private val ratingAggregationService: RatingAggregationService
) : ReviewService {
    private val logger = LoggerFactory.getLogger(ReviewServiceImpl::class.java)

    override suspend fun createReview(userId: Int, request: CreateReviewRequest): Review {
        logger.info("Creating review for userId=$userId")

        // Validate input
        validateReviewRequest(request)

        // Check for duplicate review
        if (request.classId != null && reviewRepository.hasUserReviewedClass(userId, request.classId)) {
            throw ValidationException(
                message = "You have already reviewed this class",
                errorCode = "DUPLICATE_REVIEW"
            )
        }

        val review = reviewRepository.createReview(userId, request)
            ?: throw ValidationException(
                message = "Failed to create review",
                errorCode = "REVIEW_CREATION_FAILED"
            )

        logger.info("Review created successfully: reviewId=${review.id}")

        // Update rating aggregates
        when {
            review.classId != null -> {
                ratingAggregationService.updateRatingsForClass(review.classId)
            }
            review.trainerId != null -> {
                ratingAggregationService.updateRatingsForTrainer(review.trainerId)
            }
            review.providerId != null -> {
                ratingAggregationService.updateRatingsForProvider(review.providerId)
            }
        }

        return review
    }

    override suspend fun getReviewById(id: Int): Review {
        logger.debug("Fetching review by id: $id")

        return reviewRepository.getReviewById(id)
            ?: throw NotFoundException(
                message = "Review not found",
                errorCode = "REVIEW_NOT_FOUND",
                details = mapOf("reviewId" to id.toString())
            )
    }

    override suspend fun getReviewsByClassId(classId: Int, page: Int, pageSize: Int): List<Review> {
        logger.debug("Fetching reviews for classId=$classId (page=$page, pageSize=$pageSize)")

        validatePagination(page, pageSize)

        val offset = (page - 1) * pageSize
        return reviewRepository.getReviewsByClassId(classId, pageSize, offset)
    }

    override suspend fun getReviewsByTrainerId(trainerId: String, page: Int, pageSize: Int): List<Review> {
        logger.debug("Fetching reviews for trainerId=$trainerId (page=$page, pageSize=$pageSize)")

        validatePagination(page, pageSize)

        val offset = (page - 1) * pageSize
        return reviewRepository.getReviewsByTrainerId(trainerId, pageSize, offset)
    }

    override suspend fun getReviewsByProviderId(providerId: String, page: Int, pageSize: Int): List<Review> {
        logger.debug("Fetching reviews for providerId=$providerId (page=$page, pageSize=$pageSize)")

        validatePagination(page, pageSize)

        val offset = (page - 1) * pageSize
        return reviewRepository.getReviewsByProviderId(providerId, pageSize, offset)
    }

    override suspend fun getMyReviews(userId: Int): List<Review> {
        logger.debug("Fetching reviews for userId=$userId")

        return reviewRepository.getReviewsByUserId(userId)
    }

    override suspend fun updateReview(id: Int, userId: Int, request: UpdateReviewRequest): Review {
        logger.info("Updating review: id=$id (userId=$userId)")

        // Validate input
        request.rating?.let { ValidationUtils.validateRange(it, 1, 5, "rating") }
        request.title?.let {
            if (it.isBlank()) {
                throw ValidationException(
                    message = "Title cannot be empty",
                    errorCode = "INVALID_TITLE"
                )
            }
        }
        request.content?.let {
            if (it.isBlank()) {
                throw ValidationException(
                    message = "Content cannot be empty",
                    errorCode = "INVALID_CONTENT"
                )
            }
        }

        // Check if user can edit (within 30 days)
        if (!reviewRepository.canUserEditReview(id, userId)) {
            throw ForbiddenException(
                message = "You can only edit reviews within 30 days of creation",
                errorCode = "REVIEW_EDIT_FORBIDDEN"
            )
        }

        val updatedReview = reviewRepository.updateReview(id, userId, request)
            ?: throw NotFoundException(
                message = "Review not found or you don't have permission to edit it",
                errorCode = "REVIEW_NOT_FOUND"
            )

        logger.info("Review updated successfully: reviewId=$id")

        // Update rating aggregates
        when {
            updatedReview.classId != null -> {
                ratingAggregationService.updateRatingsForClass(updatedReview.classId)
            }
            updatedReview.trainerId != null -> {
                ratingAggregationService.updateRatingsForTrainer(updatedReview.trainerId)
            }
            updatedReview.providerId != null -> {
                ratingAggregationService.updateRatingsForProvider(updatedReview.providerId)
            }
        }

        return updatedReview
    }

    override suspend fun deleteReview(id: Int, userId: Int): Boolean {
        logger.info("Deleting review: id=$id (userId=$userId)")

        // Get the review first to update aggregates after deletion
        val review = getReviewById(id)

        // Check ownership
        if (review.userId != userId) {
            throw ForbiddenException(
                message = "You don't have permission to delete this review",
                errorCode = "REVIEW_DELETE_FORBIDDEN"
            )
        }

        val deleted = reviewRepository.deleteReview(id, userId)
        if (!deleted) {
            throw NotFoundException(
                message = "Review not found",
                errorCode = "REVIEW_NOT_FOUND"
            )
        }

        logger.info("Review deleted successfully: reviewId=$id")

        // Update rating aggregates
        when {
            review.classId != null -> {
                ratingAggregationService.updateRatingsForClass(review.classId)
            }
            review.trainerId != null -> {
                ratingAggregationService.updateRatingsForTrainer(review.trainerId)
            }
            review.providerId != null -> {
                ratingAggregationService.updateRatingsForProvider(review.providerId)
            }
        }

        return true
    }

    override suspend fun voteHelpful(reviewId: Int, userId: Int): Boolean {
        logger.debug("User $userId voting helpful on review $reviewId")

        // Verify review exists
        getReviewById(reviewId)

        return reviewRepository.addVote(reviewId, userId, VoteType.HELPFUL)
    }

    override suspend fun voteNotHelpful(reviewId: Int, userId: Int): Boolean {
        logger.debug("User $userId voting not helpful on review $reviewId")

        // Verify review exists
        getReviewById(reviewId)

        return reviewRepository.addVote(reviewId, userId, VoteType.NOT_HELPFUL)
    }

    override suspend fun removeVote(reviewId: Int, userId: Int): Boolean {
        logger.debug("User $userId removing vote from review $reviewId")

        return reviewRepository.removeVote(reviewId, userId)
    }

    override suspend fun reportReview(reviewId: Int, userId: Int, request: CreateReviewReportRequest): ReviewReport {
        logger.info("User $userId reporting review $reviewId")

        // Verify review exists
        getReviewById(reviewId)

        // Validate reason
        if (request.reason.isBlank()) {
            throw ValidationException(
                message = "Report reason cannot be empty",
                errorCode = "INVALID_REASON"
            )
        }

        val report = reviewRepository.reportReview(reviewId, userId, request.reason)
            ?: throw ValidationException(
                message = "Failed to create report",
                errorCode = "REPORT_CREATION_FAILED"
            )

        logger.info("Review reported successfully: reportId=${report.id}")
        return report
    }

    private fun validateReviewRequest(request: CreateReviewRequest) {
        // Ensure at least one entity is being reviewed
        if (request.classId == null && request.trainerId == null && request.providerId == null) {
            throw ValidationException(
                message = "Must specify classId, trainerId, or providerId",
                errorCode = "INVALID_REVIEW_TARGET"
            )
        }

        // Validate rating
        ValidationUtils.validateRange(request.rating, 1, 5, "rating")

        // Validate title and content
        if (request.title.isBlank()) {
            throw ValidationException(
                message = "Title cannot be empty",
                errorCode = "INVALID_TITLE"
            )
        }

        if (request.content.isBlank()) {
            throw ValidationException(
                message = "Content cannot be empty",
                errorCode = "INVALID_CONTENT"
            )
        }

        // Validate title length
        if (request.title.length > 255) {
            throw ValidationException(
                message = "Title is too long (max 255 characters)",
                errorCode = "TITLE_TOO_LONG"
            )
        }

        // Validate content length
        if (request.content.length > 5000) {
            throw ValidationException(
                message = "Content is too long (max 5000 characters)",
                errorCode = "CONTENT_TOO_LONG"
            )
        }
    }

    private fun validatePagination(page: Int, pageSize: Int) {
        if (page < 1) {
            throw ValidationException(
                message = "Page number must be >= 1",
                errorCode = "INVALID_PAGE"
            )
        }

        if (pageSize < 1 || pageSize > 100) {
            throw ValidationException(
                message = "Page size must be between 1 and 100",
                errorCode = "INVALID_PAGE_SIZE"
            )
        }
    }
}
