package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Serializable
data class Review(
    val id: Int,
    val userId: Int,
    val classId: Int? = null,
    val trainerId: String? = null,
    val providerId: String? = null,
    val rating: Int, // 1-5
    val title: String,
    val content: String,
    val pros: String? = null,
    val cons: String? = null,
    val attendedOn: LocalDate? = null,
    val verifiedAttendance: Boolean = false,
    val helpfulCount: Int = 0,
    val status: ReviewStatus = ReviewStatus.APPROVED,
    val photos: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED,
    FLAGGED
}

@Serializable
data class CreateReviewRequest(
    val classId: Int? = null,
    val trainerId: String? = null,
    val providerId: String? = null,
    val rating: Int, // 1-5
    val title: String,
    val content: String,
    val pros: String? = null,
    val cons: String? = null,
    val attendedOn: LocalDate? = null,
    val photos: List<String> = emptyList()
)

@Serializable
data class UpdateReviewRequest(
    val rating: Int? = null,
    val title: String? = null,
    val content: String? = null,
    val pros: String? = null,
    val cons: String? = null,
    val photos: List<String>? = null
)

@Serializable
data class ReviewVote(
    val id: Int,
    val reviewId: Int,
    val userId: Int,
    val voteType: VoteType,
    val createdAt: Instant
)

@Serializable
enum class VoteType {
    HELPFUL,
    NOT_HELPFUL
}

@Serializable
data class ReviewReport(
    val id: Int,
    val reviewId: Int,
    val reporterId: Int,
    val reason: String,
    val status: ReportStatus,
    val createdAt: Instant
)

@Serializable
enum class ReportStatus {
    PENDING,
    REVIEWED,
    RESOLVED
}

@Serializable
data class CreateReviewReportRequest(
    val reason: String
)

@Serializable
data class RatingAggregate(
    val entityType: EntityType,
    val entityId: String,
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int>, // rating -> count
    val lastUpdated: Instant
)

@Serializable
enum class EntityType {
    CLASS,
    TRAINER,
    PROVIDER
}

@Serializable
data class RatingSummary(
    val averageRating: Double,
    val totalReviews: Int,
    val ratingDistribution: Map<Int, Int>,
    val recentReviews: List<Review> = emptyList()
)
