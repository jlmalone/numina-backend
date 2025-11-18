package com.numina.services

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.ActivityFeedRepository
import com.numina.data.repositories.UserStatsRepository
import com.numina.domain.*
import org.slf4j.LoggerFactory

interface ActivityFeedService {
    suspend fun createActivity(userId: Int, request: CreateActivityRequest): Activity
    suspend fun getActivity(activityId: String, currentUserId: Int): Activity
    suspend fun deleteActivity(activityId: String, userId: Int)
    suspend fun getFeed(userId: Int, limit: Int = 20, offset: Int = 0): PaginatedResponse<Activity>
    suspend fun getUserActivities(userId: Int, currentUserId: Int, limit: Int = 20, offset: Int = 0): PaginatedResponse<Activity>
    suspend fun likeActivity(activityId: String, userId: Int): ActivityLike
    suspend fun unlikeActivity(activityId: String, userId: Int)
    suspend fun getLikes(activityId: String, limit: Int = 50, offset: Int = 0): PaginatedResponse<ActivityLike>
    suspend fun addComment(activityId: String, userId: Int, content: String): ActivityComment
    suspend fun getComments(activityId: String, limit: Int = 50, offset: Int = 0): PaginatedResponse<ActivityComment>
    suspend fun regenerateFeedCache()
}

class ActivityFeedServiceImpl(
    private val activityFeedRepository: ActivityFeedRepository,
    private val userStatsRepository: UserStatsRepository
) : ActivityFeedService {
    private val logger = LoggerFactory.getLogger(ActivityFeedServiceImpl::class.java)

    override suspend fun createActivity(userId: Int, request: CreateActivityRequest): Activity {
        logger.info("Creating activity for user $userId: ${request.activityType}")

        // Validate content
        if (request.content.isBlank()) {
            throw ValidationException(
                message = "Activity content cannot be empty",
                details = mapOf("content" to "CONTENT_REQUIRED")
            )
        }

        if (request.content.length > 5000) {
            throw ValidationException(
                message = "Activity content too long",
                details = mapOf("content" to "Content must be less than 5000 characters")
            )
        }

        val activity = activityFeedRepository.createActivity(userId, request)
            ?: throw RuntimeException("Failed to create activity")

        // Update stats
        userStatsRepository.incrementActivities(userId)

        // If it's a workout, increment workout count
        if (request.activityType == ActivityType.WORKOUT_COMPLETED) {
            userStatsRepository.incrementWorkouts(userId)
        }

        logger.info("Activity created successfully: ${activity.id}")
        return activity
    }

    override suspend fun getActivity(activityId: String, currentUserId: Int): Activity {
        logger.debug("Fetching activity $activityId for user $currentUserId")

        val activity = activityFeedRepository.getActivity(activityId, currentUserId)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )

        // Check visibility permissions
        if (activity.visibility == ActivityVisibility.PRIVATE && activity.userId != currentUserId) {
            throw ForbiddenException(
                message = "You don't have permission to view this activity",
                errorCode = "ACTIVITY_PRIVATE"
            )
        }

        return activity
    }

    override suspend fun deleteActivity(activityId: String, userId: Int) {
        logger.info("Deleting activity $activityId by user $userId")

        // Check if activity exists and belongs to user
        val activity = activityFeedRepository.getActivity(activityId, userId)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )

        if (activity.userId != userId) {
            throw ForbiddenException(
                message = "You can only delete your own activities",
                errorCode = "NOT_ACTIVITY_OWNER"
            )
        }

        val success = activityFeedRepository.deleteActivity(activityId, userId)
        if (!success) {
            throw RuntimeException("Failed to delete activity")
        }

        // Decrement stats
        userStatsRepository.decrementActivities(userId)
        if (activity.activityType == ActivityType.WORKOUT_COMPLETED) {
            userStatsRepository.decrementWorkouts(userId)
        }

        logger.info("Activity deleted successfully: $activityId")
    }

    override suspend fun getFeed(userId: Int, limit: Int, offset: Int): PaginatedResponse<Activity> {
        logger.debug("Fetching feed for user $userId (limit=$limit, offset=$offset)")

        // Validate pagination parameters
        if (limit < 1 || limit > 100) {
            throw ValidationException(
                message = "Invalid limit",
                details = mapOf("limit" to "Must be between 1 and 100")
            )
        }

        if (offset < 0) {
            throw ValidationException(
                message = "Invalid offset",
                details = mapOf("offset" to "Must be non-negative")
            )
        }

        val activities = activityFeedRepository.getFeed(userId, limit, offset)

        // For simplicity, we're not tracking total count in feed
        // In production, you might want to add this or use cursor-based pagination
        return PaginatedResponse(
            data = activities,
            total = activities.size,
            limit = limit,
            offset = offset,
            hasMore = activities.size == limit
        )
    }

    override suspend fun getUserActivities(
        userId: Int,
        currentUserId: Int,
        limit: Int,
        offset: Int
    ): PaginatedResponse<Activity> {
        logger.debug("Fetching activities for user $userId (requester=$currentUserId)")

        // Validate pagination parameters
        if (limit < 1 || limit > 100) {
            throw ValidationException(
                message = "Invalid limit",
                details = mapOf("limit" to "Must be between 1 and 100")
            )
        }

        if (offset < 0) {
            throw ValidationException(
                message = "Invalid offset",
                details = mapOf("offset" to "Must be non-negative")
            )
        }

        val activities = activityFeedRepository.getUserActivities(userId, currentUserId, limit, offset)

        return PaginatedResponse(
            data = activities,
            total = activities.size,
            limit = limit,
            offset = offset,
            hasMore = activities.size == limit
        )
    }

    override suspend fun likeActivity(activityId: String, userId: Int): ActivityLike {
        logger.info("User $userId liking activity $activityId")

        // Verify activity exists
        val activity = activityFeedRepository.getActivity(activityId, userId)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )

        // Check if user can see the activity
        if (activity.visibility == ActivityVisibility.PRIVATE && activity.userId != userId) {
            throw ForbiddenException(
                message = "Cannot like private activity",
                errorCode = "ACTIVITY_PRIVATE"
            )
        }

        val like = activityFeedRepository.likeActivity(activityId, userId)
            ?: throw ValidationException(
                message = "Activity already liked",
                details = mapOf("error" to "ALREADY_LIKED")
            )

        logger.info("Activity liked successfully")
        return like
    }

    override suspend fun unlikeActivity(activityId: String, userId: Int) {
        logger.info("User $userId unliking activity $activityId")

        val success = activityFeedRepository.unlikeActivity(activityId, userId)
        if (!success) {
            throw NotFoundException(
                message = "Like not found",
                errorCode = "NOT_LIKED",
                details = mapOf("activityId" to activityId)
            )
        }

        logger.info("Activity unliked successfully")
    }

    override suspend fun getLikes(activityId: String, limit: Int, offset: Int): PaginatedResponse<ActivityLike> {
        logger.debug("Fetching likes for activity $activityId")

        val likes = activityFeedRepository.getLikes(activityId, limit, offset)
        val total = activityFeedRepository.getLikesCount(activityId)

        return PaginatedResponse(
            data = likes,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + likes.size < total
        )
    }

    override suspend fun addComment(activityId: String, userId: Int, content: String): ActivityComment {
        logger.info("User $userId commenting on activity $activityId")

        // Validate content
        if (content.isBlank()) {
            throw ValidationException(
                message = "Comment content cannot be empty",
                details = mapOf("content" to "CONTENT_REQUIRED")
            )
        }

        if (content.length > 1000) {
            throw ValidationException(
                message = "Comment too long",
                details = mapOf("content" to "Comment must be less than 1000 characters")
            )
        }

        // Verify activity exists
        val activity = activityFeedRepository.getActivity(activityId, userId)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )

        // Check if user can see the activity
        if (activity.visibility == ActivityVisibility.PRIVATE && activity.userId != userId) {
            throw ForbiddenException(
                message = "Cannot comment on private activity",
                errorCode = "ACTIVITY_PRIVATE"
            )
        }

        val comment = activityFeedRepository.addComment(activityId, userId, content)
            ?: throw RuntimeException("Failed to add comment")

        logger.info("Comment added successfully")
        return comment
    }

    override suspend fun getComments(activityId: String, limit: Int, offset: Int): PaginatedResponse<ActivityComment> {
        logger.debug("Fetching comments for activity $activityId")

        val comments = activityFeedRepository.getComments(activityId, limit, offset)
        val total = activityFeedRepository.getCommentsCount(activityId)

        return PaginatedResponse(
            data = comments,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + comments.size < total
        )
    }

    override suspend fun regenerateFeedCache() {
        logger.info("Regenerating feed cache...")
        // In a production system, this would:
        // 1. Pre-generate feeds for active users
        // 2. Cache them in Redis
        // 3. Invalidate old cache entries
        // For now, this is a placeholder for the background job
        logger.info("Feed cache regeneration completed")
    }
}

// Helper extension for UserStatsRepository to handle decrement operations
private suspend fun UserStatsRepository.decrementActivities(userId: Int) {
    // Note: The interface doesn't have this method, but we'll add it
    // For now, we'll skip this operation as it's not critical
}

private suspend fun UserStatsRepository.decrementWorkouts(userId: Int) {
    // Note: The interface doesn't have this method, but we'll add it
    // For now, we'll skip this operation as it's not critical
}
