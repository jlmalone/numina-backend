package com.numina.services.groups

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.GroupActivityRepository
import com.numina.data.repositories.GroupMemberRepository
import com.numina.domain.groups.*
import org.slf4j.LoggerFactory

interface GroupActivityService {
    suspend fun createActivity(groupId: String, creatorId: Int, request: CreateActivityRequest): GroupActivity
    suspend fun getActivityById(activityId: String): GroupActivity
    suspend fun getGroupActivities(groupId: String, upcomingOnly: Boolean): List<GroupActivity>
    suspend fun updateActivity(activityId: String, userId: Int, request: UpdateActivityRequest): GroupActivity
    suspend fun cancelActivity(activityId: String, userId: Int)
    suspend fun rsvpToActivity(activityId: String, userId: Int, status: String)
    suspend fun getActivityRSVPs(activityId: String): List<ActivityRSVP>
}

class GroupActivityServiceImpl(
    private val activityRepository: GroupActivityRepository,
    private val memberRepository: GroupMemberRepository
) : GroupActivityService {

    private val logger = LoggerFactory.getLogger(GroupActivityServiceImpl::class.java)

    override suspend fun createActivity(
        groupId: String,
        creatorId: Int,
        request: CreateActivityRequest
    ): GroupActivity {
        logger.info("Creating activity for group: groupId=$groupId, creatorId=$creatorId")

        // Verify user is a member of the group
        verifyActiveMembership(groupId, creatorId)

        // Validate input
        ValidationUtils.validateRequired(request.title, "title")
        if (request.title.length < 3 || request.title.length > 200) {
            throw ValidationException(
                message = "Activity title must be between 3 and 200 characters",
                errorCode = "INVALID_TITLE"
            )
        }

        request.latitude?.let { ValidationUtils.validateLatitude(it) }
        request.longitude?.let { ValidationUtils.validateLongitude(it) }

        // Validate scheduled time is in the future
        val now = kotlinx.datetime.Clock.System.now()
        if (request.scheduledAt <= now) {
            throw ValidationException(
                message = "Scheduled time must be in the future",
                errorCode = "INVALID_SCHEDULED_TIME"
            )
        }

        val activity = activityRepository.createActivity(
            groupId = groupId,
            classId = request.classId,
            title = request.title,
            description = request.description,
            scheduledAt = request.scheduledAt,
            location = request.location,
            latitude = request.latitude,
            longitude = request.longitude,
            isRecurring = request.isRecurring,
            recurrenceRule = request.recurrenceRule,
            createdById = creatorId
        ) ?: throw ValidationException(
            message = "Failed to create activity",
            errorCode = "ACTIVITY_CREATION_FAILED"
        )

        logger.info("Activity created successfully: activityId=${activity.id}")
        return activity
    }

    override suspend fun getActivityById(activityId: String): GroupActivity {
        logger.debug("Fetching activity by id: $activityId")

        return activityRepository.getActivityById(activityId)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )
    }

    override suspend fun getGroupActivities(groupId: String, upcomingOnly: Boolean): List<GroupActivity> {
        logger.debug("Fetching activities for group: groupId=$groupId, upcomingOnly=$upcomingOnly")

        return activityRepository.getGroupActivities(groupId, upcomingOnly)
    }

    override suspend fun updateActivity(
        activityId: String,
        userId: Int,
        request: UpdateActivityRequest
    ): GroupActivity {
        logger.info("Updating activity: activityId=$activityId, userId=$userId")

        // Get the activity
        val activity = getActivityById(activityId)

        // Verify user has permission (creator or group admin)
        verifyActivityPermission(activity.groupId, userId, activity.createdById)

        // Validate optional fields
        request.latitude?.let { ValidationUtils.validateLatitude(it) }
        request.longitude?.let { ValidationUtils.validateLongitude(it) }
        request.scheduledAt?.let {
            val now = kotlinx.datetime.Clock.System.now()
            if (it <= now) {
                throw ValidationException(
                    message = "Scheduled time must be in the future",
                    errorCode = "INVALID_SCHEDULED_TIME"
                )
            }
        }

        val updatedActivity = activityRepository.updateActivity(activityId, request)
            ?: throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )

        logger.info("Activity updated successfully: activityId=$activityId")
        return updatedActivity
    }

    override suspend fun cancelActivity(activityId: String, userId: Int) {
        logger.info("Cancelling activity: activityId=$activityId, userId=$userId")

        // Get the activity
        val activity = getActivityById(activityId)

        // Verify user has permission
        verifyActivityPermission(activity.groupId, userId, activity.createdById)

        val cancelled = activityRepository.cancelActivity(activityId)
        if (!cancelled) {
            throw NotFoundException(
                message = "Activity not found",
                errorCode = "ACTIVITY_NOT_FOUND",
                details = mapOf("activityId" to activityId)
            )
        }

        logger.info("Activity cancelled successfully: activityId=$activityId")
    }

    override suspend fun rsvpToActivity(activityId: String, userId: Int, status: String) {
        logger.info("RSVP to activity: activityId=$activityId, userId=$userId, status=$status")

        // Get the activity
        val activity = getActivityById(activityId)

        // Verify user is a member of the group
        verifyActiveMembership(activity.groupId, userId)

        // Verify activity is not cancelled
        if (activity.cancelled) {
            throw ValidationException(
                message = "Cannot RSVP to a cancelled activity",
                errorCode = "ACTIVITY_CANCELLED"
            )
        }

        // Validate RSVP status
        val validStatuses = setOf("GOING", "MAYBE", "NOT_GOING")
        if (status !in validStatuses) {
            throw ValidationException(
                message = "Invalid RSVP status. Must be one of: ${validStatuses.joinToString(", ")}",
                errorCode = "INVALID_RSVP_STATUS"
            )
        }

        activityRepository.createOrUpdateRSVP(activityId, userId, status)

        logger.info("RSVP recorded successfully: activityId=$activityId, userId=$userId, status=$status")
    }

    override suspend fun getActivityRSVPs(activityId: String): List<ActivityRSVP> {
        logger.debug("Fetching RSVPs for activity: $activityId")

        // Verify activity exists
        getActivityById(activityId)

        return activityRepository.getActivityRSVPs(activityId)
    }

    // Helper methods

    private suspend fun verifyActiveMembership(groupId: String, userId: Int) {
        val member = memberRepository.getMember(groupId, userId)
            ?: throw ForbiddenException(
                message = "You are not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.status != "ACTIVE") {
            throw ForbiddenException(
                message = "Only active members can access activities",
                errorCode = "INACTIVE_MEMBER"
            )
        }
    }

    private suspend fun verifyActivityPermission(groupId: String, userId: Int, createdById: Int) {
        // Creator can always modify their activity
        if (userId == createdById) {
            return
        }

        // Otherwise, must be group admin or owner
        val member = memberRepository.getMember(groupId, userId)
            ?: throw ForbiddenException(
                message = "You are not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.role != "OWNER" && member.role != "ADMIN") {
            throw ForbiddenException(
                message = "You do not have permission to modify this activity",
                errorCode = "INSUFFICIENT_PERMISSIONS"
            )
        }
    }
}
