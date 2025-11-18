package com.numina.services.groups

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.GroupActivityRepository
import com.numina.data.repositories.GroupMemberRepository
import com.numina.domain.groups.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.Duration.Companion.hours

class GroupActivityServiceTest {

    private lateinit var activityRepository: GroupActivityRepository
    private lateinit var memberRepository: GroupMemberRepository
    private lateinit var activityService: GroupActivityService

    @BeforeEach
    fun setup() {
        activityRepository = mockk()
        memberRepository = mockk()
        activityService = GroupActivityServiceImpl(activityRepository, memberRepository)
    }

    @Test
    fun `createActivity should create activity when user is active member`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 1
        val scheduledAt = Clock.System.now() + 24.hours

        val request = CreateActivityRequest(
            classId = null,
            title = "Morning Run",
            description = "5k morning run",
            scheduledAt = scheduledAt,
            location = "Central Park",
            latitude = 40.785091,
            longitude = -73.968285,
            isRecurring = false,
            recurrenceRule = null
        )

        val mockMember = GroupMember(
            id = "member-1",
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        val mockActivity = GroupActivity(
            id = "activity-1",
            groupId = groupId,
            classId = null,
            title = request.title,
            description = request.description,
            scheduledAt = request.scheduledAt,
            location = request.location,
            latitude = request.latitude,
            longitude = request.longitude,
            isRecurring = request.isRecurring,
            recurrenceRule = request.recurrenceRule,
            createdById = userId,
            createdAt = Clock.System.now(),
            cancelled = false,
            rsvpStats = null
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember
        coEvery {
            activityRepository.createActivity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockActivity

        // When
        val result = activityService.createActivity(groupId, userId, request)

        // Then
        assertNotNull(result)
        assertEquals(request.title, result.title)
        assertEquals(groupId, result.groupId)
    }

    @Test
    fun `createActivity should throw ForbiddenException when user is not member`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 1
        val scheduledAt = Clock.System.now() + 24.hours

        val request = CreateActivityRequest(
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = scheduledAt,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns null

        // When/Then
        assertThrows<ForbiddenException> {
            activityService.createActivity(groupId, userId, request)
        }
    }

    @Test
    fun `createActivity should throw ValidationException when scheduled time is in past`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 1
        val scheduledAt = Clock.System.now() - 1.hours // Past time

        val request = CreateActivityRequest(
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = scheduledAt,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null
        )

        val mockMember = GroupMember(
            id = "member-1",
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember

        // When/Then
        assertThrows<ValidationException> {
            activityService.createActivity(groupId, userId, request)
        }
    }

    @Test
    fun `rsvpToActivity should record RSVP when user is active member`() = runBlocking {
        // Given
        val activityId = "activity-1"
        val groupId = "group-1"
        val userId = 2
        val status = "GOING"

        val mockActivity = GroupActivity(
            id = activityId,
            groupId = groupId,
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = Clock.System.now() + 24.hours,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null,
            createdById = 1,
            createdAt = Clock.System.now(),
            cancelled = false,
            rsvpStats = null
        )

        val mockMember = GroupMember(
            id = "member-2",
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { activityRepository.getActivityById(activityId) } returns mockActivity
        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember
        coEvery { activityRepository.createOrUpdateRSVP(activityId, userId, status) } returns mockk()

        // When
        activityService.rsvpToActivity(activityId, userId, status)

        // Then
        coVerify { activityRepository.createOrUpdateRSVP(activityId, userId, status) }
    }

    @Test
    fun `rsvpToActivity should throw ValidationException for cancelled activity`() = runBlocking {
        // Given
        val activityId = "activity-1"
        val groupId = "group-1"
        val userId = 2

        val mockActivity = GroupActivity(
            id = activityId,
            groupId = groupId,
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = Clock.System.now() + 24.hours,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null,
            createdById = 1,
            createdAt = Clock.System.now(),
            cancelled = true, // Activity is cancelled
            rsvpStats = null
        )

        val mockMember = GroupMember(
            id = "member-2",
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { activityRepository.getActivityById(activityId) } returns mockActivity
        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember

        // When/Then
        assertThrows<ValidationException> {
            activityService.rsvpToActivity(activityId, userId, "GOING")
        }
    }

    @Test
    fun `cancelActivity should succeed when user is creator`() = runBlocking {
        // Given
        val activityId = "activity-1"
        val groupId = "group-1"
        val userId = 1

        val mockActivity = GroupActivity(
            id = activityId,
            groupId = groupId,
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = Clock.System.now() + 24.hours,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null,
            createdById = userId,
            createdAt = Clock.System.now(),
            cancelled = false,
            rsvpStats = null
        )

        coEvery { activityRepository.getActivityById(activityId) } returns mockActivity
        coEvery { activityRepository.cancelActivity(activityId) } returns true

        // When
        activityService.cancelActivity(activityId, userId)

        // Then
        coVerify { activityRepository.cancelActivity(activityId) }
    }

    @Test
    fun `cancelActivity should succeed when user is admin`() = runBlocking {
        // Given
        val activityId = "activity-1"
        val groupId = "group-1"
        val userId = 2
        val creatorId = 1

        val mockActivity = GroupActivity(
            id = activityId,
            groupId = groupId,
            classId = null,
            title = "Morning Run",
            description = null,
            scheduledAt = Clock.System.now() + 24.hours,
            location = null,
            latitude = null,
            longitude = null,
            isRecurring = false,
            recurrenceRule = null,
            createdById = creatorId,
            createdAt = Clock.System.now(),
            cancelled = false,
            rsvpStats = null
        )

        val mockMember = GroupMember(
            id = "member-2",
            groupId = groupId,
            userId = userId,
            role = "ADMIN",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { activityRepository.getActivityById(activityId) } returns mockActivity
        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember
        coEvery { activityRepository.cancelActivity(activityId) } returns true

        // When
        activityService.cancelActivity(activityId, userId)

        // Then
        coVerify { activityRepository.cancelActivity(activityId) }
    }
}
