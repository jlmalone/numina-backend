package com.numina.services.groups

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.GroupMemberRepository
import com.numina.data.repositories.GroupRepository
import com.numina.domain.groups.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GroupServiceTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var memberRepository: GroupMemberRepository
    private lateinit var groupService: GroupService

    @BeforeEach
    fun setup() {
        groupRepository = mockk()
        memberRepository = mockk()
        groupService = GroupServiceImpl(groupRepository, memberRepository)
    }

    @Test
    fun `createGroup should create group and add owner as member`() = runBlocking {
        // Given
        val ownerId = 1
        val request = CreateGroupRequest(
            name = "Test Group",
            description = "Test Description",
            photoUrl = null,
            category = "RUNNING",
            isPrivate = false,
            maxMembers = 50,
            location = "New York",
            latitude = 40.7128,
            longitude = -74.0060
        )

        val mockGroup = Group(
            id = "group-1",
            name = request.name,
            description = request.description,
            photoUrl = request.photoUrl,
            category = request.category,
            isPrivate = request.isPrivate,
            maxMembers = request.maxMembers,
            ownerId = ownerId,
            location = request.location,
            latitude = request.latitude,
            longitude = request.longitude,
            memberCount = 0,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        val mockMember = GroupMember(
            id = "member-1",
            groupId = "group-1",
            userId = ownerId,
            role = "OWNER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery {
            groupRepository.createGroup(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockGroup

        coEvery {
            memberRepository.addMember(any(), any(), any(), any())
        } returns mockMember

        // When
        val result = groupService.createGroup(ownerId, request)

        // Then
        assertNotNull(result)
        assertEquals(request.name, result.name)
        assertEquals(1, result.memberCount)

        coVerify { groupRepository.createGroup(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { memberRepository.addMember("group-1", ownerId, "OWNER", "ACTIVE") }
    }

    @Test
    fun `createGroup should throw ValidationException for invalid name`() = runBlocking {
        // Given
        val request = CreateGroupRequest(
            name = "AB", // Too short
            description = null,
            photoUrl = null,
            category = "RUNNING",
            isPrivate = false,
            maxMembers = 50,
            location = null,
            latitude = null,
            longitude = null
        )

        // When/Then
        assertThrows<ValidationException> {
            groupService.createGroup(1, request)
        }
    }

    @Test
    fun `getGroupById should return group when found`() = runBlocking {
        // Given
        val groupId = "group-1"
        val mockGroup = Group(
            id = groupId,
            name = "Test Group",
            description = null,
            photoUrl = null,
            category = "RUNNING",
            isPrivate = false,
            maxMembers = 50,
            ownerId = 1,
            location = null,
            latitude = null,
            longitude = null,
            memberCount = 5,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns mockGroup

        // When
        val result = groupService.getGroupById(groupId)

        // Then
        assertEquals(groupId, result.id)
        assertEquals("Test Group", result.name)
    }

    @Test
    fun `getGroupById should throw NotFoundException when group not found`() = runBlocking {
        // Given
        val groupId = "nonexistent"
        coEvery { groupRepository.getGroupById(groupId) } returns null

        // When/Then
        assertThrows<NotFoundException> {
            groupService.getGroupById(groupId)
        }
    }

    @Test
    fun `joinGroup should add member with ACTIVE status for public groups`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 2
        val mockGroup = Group(
            id = groupId,
            name = "Test Group",
            description = null,
            photoUrl = null,
            category = "RUNNING",
            isPrivate = false,
            maxMembers = 50,
            ownerId = 1,
            location = null,
            latitude = null,
            longitude = null,
            memberCount = 5,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns mockGroup
        coEvery { memberRepository.getMember(groupId, userId) } returns null
        coEvery { memberRepository.getMemberCount(groupId, "ACTIVE") } returns 5
        coEvery { memberRepository.addMember(any(), any(), any(), any()) } returns mockk()

        // When
        groupService.joinGroup(groupId, userId)

        // Then
        coVerify { memberRepository.addMember(groupId, userId, "MEMBER", "ACTIVE") }
    }

    @Test
    fun `joinGroup should add member with PENDING status for private groups`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 2
        val mockGroup = Group(
            id = groupId,
            name = "Test Group",
            description = null,
            photoUrl = null,
            category = "RUNNING",
            isPrivate = true,
            maxMembers = 50,
            ownerId = 1,
            location = null,
            latitude = null,
            longitude = null,
            memberCount = 5,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { groupRepository.getGroupById(groupId) } returns mockGroup
        coEvery { memberRepository.getMember(groupId, userId) } returns null
        coEvery { memberRepository.getMemberCount(groupId, "ACTIVE") } returns 5
        coEvery { memberRepository.addMember(any(), any(), any(), any()) } returns mockk()

        // When
        groupService.joinGroup(groupId, userId)

        // Then
        coVerify { memberRepository.addMember(groupId, userId, "MEMBER", "PENDING") }
    }

    @Test
    fun `leaveGroup should remove member when not owner`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 2
        val mockMember = GroupMember(
            id = "member-1",
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember
        coEvery { memberRepository.removeMember(groupId, userId) } returns true

        // When
        groupService.leaveGroup(groupId, userId)

        // Then
        coVerify { memberRepository.removeMember(groupId, userId) }
    }

    @Test
    fun `leaveGroup should throw ValidationException when user is owner`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 1
        val mockMember = GroupMember(
            id = "member-1",
            groupId = groupId,
            userId = userId,
            role = "OWNER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember

        // When/Then
        assertThrows<ValidationException> {
            groupService.leaveGroup(groupId, userId)
        }
    }

    @Test
    fun `deleteGroup should delete when user is owner`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 1
        val mockMember = GroupMember(
            id = "member-1",
            groupId = groupId,
            userId = userId,
            role = "OWNER",
            status = "ACTIVE",
            joinedAt = Clock.System.now()
        )

        coEvery { memberRepository.getMember(groupId, userId) } returns mockMember
        coEvery { groupRepository.deleteGroup(groupId) } returns true

        // When
        groupService.deleteGroup(groupId, userId)

        // Then
        coVerify { groupRepository.deleteGroup(groupId) }
    }

    @Test
    fun `deleteGroup should throw ForbiddenException when user is not owner`() = runBlocking {
        // Given
        val groupId = "group-1"
        val userId = 2
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
        assertThrows<ForbiddenException> {
            groupService.deleteGroup(groupId, userId)
        }
    }
}
