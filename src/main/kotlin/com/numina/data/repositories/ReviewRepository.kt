package com.numina.data.repositories

import com.numina.data.tables.*
import com.numina.domain.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface ReviewRepository {
    suspend fun createReview(userId: Int, request: CreateReviewRequest): Review?
    suspend fun getReviewById(id: Int): Review?
    suspend fun getReviewsByClassId(classId: Int, limit: Int = 50, offset: Int = 0): List<Review>
    suspend fun getReviewsByTrainerId(trainerId: String, limit: Int = 50, offset: Int = 0): List<Review>
    suspend fun getReviewsByProviderId(providerId: String, limit: Int = 50, offset: Int = 0): List<Review>
    suspend fun getReviewsByUserId(userId: Int): List<Review>
    suspend fun updateReview(id: Int, userId: Int, request: UpdateReviewRequest): Review?
    suspend fun deleteReview(id: Int, userId: Int): Boolean
    suspend fun addVote(reviewId: Int, userId: Int, voteType: VoteType): Boolean
    suspend fun removeVote(reviewId: Int, userId: Int): Boolean
    suspend fun hasUserVoted(reviewId: Int, userId: Int): Boolean
    suspend fun reportReview(reviewId: Int, reporterId: Int, reason: String): ReviewReport?
    suspend fun hasUserReviewedClass(userId: Int, classId: Int): Boolean
    suspend fun canUserEditReview(reviewId: Int, userId: Int): Boolean
}

class ReviewRepositoryImpl : ReviewRepository {

    override suspend fun createReview(userId: Int, request: CreateReviewRequest): Review? = transaction {
        val now = Clock.System.now()

        val reviewId = Reviews.insertAndGetId {
            it[Reviews.userId] = userId
            it[classId] = request.classId
            it[trainerId] = request.trainerId
            it[providerId] = request.providerId
            it[rating] = request.rating
            it[title] = request.title
            it[content] = request.content
            it[pros] = request.pros
            it[cons] = request.cons
            it[attendedOn] = request.attendedOn
            it[verifiedAttendance] = request.attendedOn != null
            it[helpfulCount] = 0
            it[status] = ReviewStatus.APPROVED.name.lowercase()
            it[createdAt] = now
            it[updatedAt] = now
        }

        // Insert photos if provided
        if (request.photos.isNotEmpty()) {
            ReviewPhotos.batchInsert(request.photos) { photoUrl ->
                this[ReviewPhotos.reviewId] = reviewId
                this[ReviewPhotos.photoUrl] = photoUrl
                this[ReviewPhotos.createdAt] = now
            }
        }

        getReviewById(reviewId.value)
    }

    override suspend fun getReviewById(id: Int): Review? = transaction {
        Reviews.select { Reviews.id eq id }
            .firstOrNull()
            ?.let { mapRowToReview(it) }
    }

    override suspend fun getReviewsByClassId(classId: Int, limit: Int, offset: Int): List<Review> = transaction {
        Reviews.select { Reviews.classId eq classId }
            .orderBy(Reviews.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { mapRowToReview(it) }
    }

    override suspend fun getReviewsByTrainerId(trainerId: String, limit: Int, offset: Int): List<Review> = transaction {
        Reviews.select { Reviews.trainerId eq trainerId }
            .orderBy(Reviews.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { mapRowToReview(it) }
    }

    override suspend fun getReviewsByProviderId(providerId: String, limit: Int, offset: Int): List<Review> = transaction {
        Reviews.select { Reviews.providerId eq providerId }
            .orderBy(Reviews.createdAt, SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { mapRowToReview(it) }
    }

    override suspend fun getReviewsByUserId(userId: Int): List<Review> = transaction {
        Reviews.select { Reviews.userId eq userId }
            .orderBy(Reviews.createdAt, SortOrder.DESC)
            .map { mapRowToReview(it) }
    }

    override suspend fun updateReview(id: Int, userId: Int, request: UpdateReviewRequest): Review? = transaction {
        val now = Clock.System.now()

        val updateCount = Reviews.update({ (Reviews.id eq id) and (Reviews.userId eq userId) }) {
            request.rating?.let { rating -> it[Reviews.rating] = rating }
            request.title?.let { title -> it[Reviews.title] = title }
            request.content?.let { content -> it[Reviews.content] = content }
            request.pros?.let { pros -> it[Reviews.pros] = pros }
            request.cons?.let { cons -> it[Reviews.cons] = cons }
            it[updatedAt] = now
        }

        if (updateCount > 0) {
            // Update photos if provided
            request.photos?.let { photos ->
                // Delete existing photos
                ReviewPhotos.deleteWhere { ReviewPhotos.reviewId eq id }

                // Insert new photos
                if (photos.isNotEmpty()) {
                    ReviewPhotos.batchInsert(photos) { photoUrl ->
                        this[ReviewPhotos.reviewId] = id
                        this[ReviewPhotos.photoUrl] = photoUrl
                        this[ReviewPhotos.createdAt] = now
                    }
                }
            }

            getReviewById(id)
        } else {
            null
        }
    }

    override suspend fun deleteReview(id: Int, userId: Int): Boolean = transaction {
        Reviews.deleteWhere { (Reviews.id eq id) and (Reviews.userId eq userId) } > 0
    }

    override suspend fun addVote(reviewId: Int, userId: Int, voteType: VoteType): Boolean = transaction {
        try {
            // Remove existing vote if any
            ReviewVotes.deleteWhere { (ReviewVotes.reviewId eq reviewId) and (ReviewVotes.userId eq userId) }

            // Insert new vote
            ReviewVotes.insert {
                it[ReviewVotes.reviewId] = reviewId
                it[ReviewVotes.userId] = userId
                it[ReviewVotes.voteType] = voteType.name.lowercase()
                it[createdAt] = Clock.System.now()
            }

            // Update helpful count
            val helpfulVotes = ReviewVotes.select {
                (ReviewVotes.reviewId eq reviewId) and (ReviewVotes.voteType eq VoteType.HELPFUL.name.lowercase())
            }.count()

            Reviews.update({ Reviews.id eq reviewId }) {
                it[helpfulCount] = helpfulVotes.toInt()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun removeVote(reviewId: Int, userId: Int): Boolean = transaction {
        val deleted = ReviewVotes.deleteWhere {
            (ReviewVotes.reviewId eq reviewId) and (ReviewVotes.userId eq userId)
        } > 0

        if (deleted) {
            // Update helpful count
            val helpfulVotes = ReviewVotes.select {
                (ReviewVotes.reviewId eq reviewId) and (ReviewVotes.voteType eq VoteType.HELPFUL.name.lowercase())
            }.count()

            Reviews.update({ Reviews.id eq reviewId }) {
                it[helpfulCount] = helpfulVotes.toInt()
            }
        }

        deleted
    }

    override suspend fun hasUserVoted(reviewId: Int, userId: Int): Boolean = transaction {
        ReviewVotes.select {
            (ReviewVotes.reviewId eq reviewId) and (ReviewVotes.userId eq userId)
        }.count() > 0
    }

    override suspend fun reportReview(reviewId: Int, reporterId: Int, reason: String): ReviewReport? = transaction {
        val reportId = ReviewReports.insertAndGetId {
            it[ReviewReports.reviewId] = reviewId
            it[ReviewReports.reporterId] = reporterId
            it[ReviewReports.reason] = reason
            it[status] = ReportStatus.PENDING.name.lowercase()
            it[createdAt] = Clock.System.now()
        }

        ReviewReports.select { ReviewReports.id eq reportId }
            .firstOrNull()
            ?.let { row ->
                ReviewReport(
                    id = row[ReviewReports.id].value,
                    reviewId = row[ReviewReports.reviewId].value,
                    reporterId = row[ReviewReports.reporterId].value,
                    reason = row[ReviewReports.reason],
                    status = ReportStatus.valueOf(row[ReviewReports.status].uppercase()),
                    createdAt = row[ReviewReports.createdAt]
                )
            }
    }

    override suspend fun hasUserReviewedClass(userId: Int, classId: Int): Boolean = transaction {
        Reviews.select {
            (Reviews.userId eq userId) and (Reviews.classId eq classId)
        }.count() > 0
    }

    override suspend fun canUserEditReview(reviewId: Int, userId: Int): Boolean = transaction {
        val review = Reviews.select { (Reviews.id eq reviewId) and (Reviews.userId eq userId) }
            .firstOrNull()
            ?: return@transaction false

        val createdAt = review[Reviews.createdAt]
        val now = Clock.System.now()
        val daysSinceCreation = (now.epochSeconds - createdAt.epochSeconds) / (24 * 3600)

        daysSinceCreation <= 30
    }

    private fun mapRowToReview(row: ResultRow): Review {
        val reviewId = row[Reviews.id].value

        // Get photos for this review
        val photos = transaction {
            ReviewPhotos.select { ReviewPhotos.reviewId eq reviewId }
                .map { it[ReviewPhotos.photoUrl] }
        }

        return Review(
            id = reviewId,
            userId = row[Reviews.userId].value,
            classId = row[Reviews.classId]?.value,
            trainerId = row[Reviews.trainerId],
            providerId = row[Reviews.providerId],
            rating = row[Reviews.rating],
            title = row[Reviews.title],
            content = row[Reviews.content],
            pros = row[Reviews.pros],
            cons = row[Reviews.cons],
            attendedOn = row[Reviews.attendedOn],
            verifiedAttendance = row[Reviews.verifiedAttendance],
            helpfulCount = row[Reviews.helpfulCount],
            status = ReviewStatus.valueOf(row[Reviews.status].uppercase()),
            photos = photos,
            createdAt = row[Reviews.createdAt],
            updatedAt = row[Reviews.updatedAt]
        )
    }
}
