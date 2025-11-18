package com.numina.data.tables

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Reviews : IntIdTable("reviews") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val classId = reference("class_id", Classes, onDelete = ReferenceOption.CASCADE).nullable()
    val trainerId = varchar("trainer_id", 255).nullable()
    val providerId = varchar("provider_id", 100).nullable()
    val rating = integer("rating") // 1-5
    val title = varchar("title", 255)
    val content = text("content")
    val pros = text("pros").nullable()
    val cons = text("cons").nullable()
    val attendedOn = date("attended_on").nullable()
    val verifiedAttendance = bool("verified_attendance").default(false)
    val helpfulCount = integer("helpful_count").default(0)
    val status = varchar("status", 50).default("approved") // pending, approved, rejected, flagged
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        // Unique constraint: one review per user per class
        uniqueIndex(userId, classId)
        index(false, userId)
        index(false, classId)
        index(false, createdAt)
        index(false, status)
    }
}

object ReviewPhotos : IntIdTable("review_photos") {
    val reviewId = reference("review_id", Reviews, onDelete = ReferenceOption.CASCADE)
    val photoUrl = varchar("photo_url", 500)
    val createdAt = timestamp("created_at")
}

object ReviewVotes : IntIdTable("review_votes") {
    val reviewId = reference("review_id", Reviews, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val voteType = varchar("vote_type", 20) // helpful, not_helpful
    val createdAt = timestamp("created_at")

    init {
        // Unique constraint: one vote per user per review
        uniqueIndex(reviewId, userId)
    }
}

object ReviewReports : IntIdTable("review_reports") {
    val reviewId = reference("review_id", Reviews, onDelete = ReferenceOption.CASCADE)
    val reporterId = reference("reporter_id", Users, onDelete = ReferenceOption.CASCADE)
    val reason = varchar("reason", 500)
    val status = varchar("status", 50).default("pending") // pending, reviewed, resolved
    val createdAt = timestamp("created_at")
}

object RatingAggregates : IntIdTable("rating_aggregates") {
    val entityType = varchar("entity_type", 50) // class, trainer, provider
    val entityId = varchar("entity_id", 100) // Can be class ID or trainer/provider ID
    val averageRating = double("average_rating")
    val totalReviews = integer("total_reviews")
    val ratingDistribution = varchar("rating_distribution", 500) // JSON string: {"1":0,"2":1,"3":5,"4":10,"5":20}
    val lastUpdated = timestamp("last_updated")

    init {
        // Unique constraint: one aggregate per entity type and ID
        uniqueIndex(entityType, entityId)
    }
}
