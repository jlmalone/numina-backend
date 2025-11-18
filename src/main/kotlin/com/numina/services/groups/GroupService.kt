package com.numina.services.groups

import com.numina.common.exceptions.ForbiddenException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.common.models.PaginatedResponse
import com.numina.common.models.PaginationMeta
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.GroupRepository
import com.numina.data.repositories.GroupMemberRepository
import com.numina.domain.groups.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.days

interface GroupService {
    suspend fun createGroup(ownerId: Int, request: CreateGroupRequest): Group
    suspend fun getGroupById(groupId: String): Group
    suspend fun getGroups(filters: GroupFilters, page: Int, pageSize: Int): PaginatedResponse<Group>
    suspend fun updateGroup(groupId: String, userId: Int, request: UpdateGroupRequest): Group
    suspend fun deleteGroup(groupId: String, userId: Int)
    suspend fun joinGroup(groupId: String, userId: Int)
    suspend fun leaveGroup(groupId: String, userId: Int)
    suspend fun inviteToGroup(groupId: String, inviterId: Int, request: InviteUserRequest): InviteLinkResponse?
    suspend fun kickMember(groupId: String, adminId: Int, memberId: Int)
    suspend fun getGroupMembers(groupId: String): List<GroupMemberWithUser>
    suspend fun getUserGroups(userId: Int): List<Group>
    suspend fun discoverGroups(filters: GroupFilters, page: Int, pageSize: Int): PaginatedResponse<Group>
    suspend fun getPendingRequests(groupId: String, userId: Int): List<GroupMemberWithUser>
    suspend fun approveJoinRequest(groupId: String, adminId: Int, memberId: Int)
    suspend fun rejectJoinRequest(groupId: String, adminId: Int, memberId: Int)
}

class GroupServiceImpl(
    private val groupRepository: GroupRepository,
    private val memberRepository: GroupMemberRepository
) : GroupService {

    private val logger = LoggerFactory.getLogger(GroupServiceImpl::class.java)

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MIN_GROUP_MEMBERS = 2
        private const val MAX_GROUP_MEMBERS = 500
        private const val INVITE_CODE_LENGTH = 32
        private val INVITE_EXPIRATION_DAYS = 7.days
    }

    override suspend fun createGroup(ownerId: Int, request: CreateGroupRequest): Group {
        logger.info("Creating group: ${request.name} (ownerId=$ownerId)")

        // Validate input
        ValidationUtils.validateRequired(request.name, "name")
        if (request.name.length < 3 || request.name.length > 100) {
            throw ValidationException(
                message = "Group name must be between 3 and 100 characters",
                errorCode = "INVALID_GROUP_NAME"
            )
        }

        if (request.maxMembers < MIN_GROUP_MEMBERS || request.maxMembers > MAX_GROUP_MEMBERS) {
            throw ValidationException(
                message = "Max members must be between $MIN_GROUP_MEMBERS and $MAX_GROUP_MEMBERS",
                errorCode = "INVALID_MAX_MEMBERS"
            )
        }

        request.latitude?.let { ValidationUtils.validateLatitude(it) }
        request.longitude?.let { ValidationUtils.validateLongitude(it) }

        // Create group
        val group = groupRepository.createGroup(
            name = request.name,
            description = request.description,
            photoUrl = request.photoUrl,
            category = request.category,
            isPrivate = request.isPrivate,
            maxMembers = request.maxMembers,
            ownerId = ownerId,
            location = request.location,
            latitude = request.latitude,
            longitude = request.longitude
        ) ?: throw ValidationException(
            message = "Failed to create group",
            errorCode = "GROUP_CREATION_FAILED"
        )

        // Add owner as first member
        memberRepository.addMember(
            groupId = group.id,
            userId = ownerId,
            role = "OWNER",
            status = "ACTIVE"
        )

        logger.info("Group created successfully: groupId=${group.id}")
        return group.copy(memberCount = 1)
    }

    override suspend fun getGroupById(groupId: String): Group {
        logger.debug("Fetching group by id: $groupId")

        return groupRepository.getGroupById(groupId)
            ?: throw NotFoundException(
                message = "Group not found",
                errorCode = "GROUP_NOT_FOUND",
                details = mapOf("groupId" to groupId)
            )
    }

    override suspend fun getGroups(
        filters: GroupFilters,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<Group> {
        logger.debug("Fetching groups with filters: $filters")

        val effectivePageSize = validateAndNormalizePageSize(page, pageSize)
        val allGroups = groupRepository.getGroups(filters)

        return paginateResults(allGroups, page, effectivePageSize)
    }

    override suspend fun updateGroup(groupId: String, userId: Int, request: UpdateGroupRequest): Group {
        logger.info("Updating group: id=$groupId (userId=$userId)")

        // Verify user has permission
        verifyAdminPermission(groupId, userId)

        // Validate optional fields
        request.latitude?.let { ValidationUtils.validateLatitude(it) }
        request.longitude?.let { ValidationUtils.validateLongitude(it) }
        request.maxMembers?.let {
            if (it < MIN_GROUP_MEMBERS || it > MAX_GROUP_MEMBERS) {
                throw ValidationException(
                    message = "Max members must be between $MIN_GROUP_MEMBERS and $MAX_GROUP_MEMBERS",
                    errorCode = "INVALID_MAX_MEMBERS"
                )
            }
        }

        val updatedGroup = groupRepository.updateGroup(groupId, request)
            ?: throw NotFoundException(
                message = "Group not found",
                errorCode = "GROUP_NOT_FOUND",
                details = mapOf("groupId" to groupId)
            )

        logger.info("Group updated successfully: groupId=$groupId")
        return updatedGroup
    }

    override suspend fun deleteGroup(groupId: String, userId: Int) {
        logger.info("Deleting group: id=$groupId (userId=$userId)")

        // Verify user is owner
        verifyOwnerPermission(groupId, userId)

        val deleted = groupRepository.deleteGroup(groupId)
        if (!deleted) {
            throw NotFoundException(
                message = "Group not found",
                errorCode = "GROUP_NOT_FOUND",
                details = mapOf("groupId" to groupId)
            )
        }

        logger.info("Group deleted successfully: groupId=$groupId")
    }

    override suspend fun joinGroup(groupId: String, userId: Int) {
        logger.info("User joining group: groupId=$groupId, userId=$userId")

        val group = getGroupById(groupId)

        // Check if user is already a member
        val existingMember = memberRepository.getMember(groupId, userId)
        if (existingMember != null) {
            if (existingMember.status == "ACTIVE") {
                throw ValidationException(
                    message = "User is already a member of this group",
                    errorCode = "ALREADY_MEMBER"
                )
            } else if (existingMember.status == "PENDING") {
                throw ValidationException(
                    message = "Join request is already pending",
                    errorCode = "REQUEST_PENDING"
                )
            }
        }

        // Check if group is full
        val currentMemberCount = memberRepository.getMemberCount(groupId, "ACTIVE")
        if (currentMemberCount >= group.maxMembers) {
            throw ValidationException(
                message = "Group is full",
                errorCode = "GROUP_FULL"
            )
        }

        // Add member with appropriate status
        val status = if (group.isPrivate) "PENDING" else "ACTIVE"
        memberRepository.addMember(
            groupId = groupId,
            userId = userId,
            role = "MEMBER",
            status = status
        )

        logger.info("User joined group successfully: groupId=$groupId, userId=$userId, status=$status")
    }

    override suspend fun leaveGroup(groupId: String, userId: Int) {
        logger.info("User leaving group: groupId=$groupId, userId=$userId")

        val member = memberRepository.getMember(groupId, userId)
            ?: throw NotFoundException(
                message = "User is not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.role == "OWNER") {
            throw ValidationException(
                message = "Owner cannot leave the group. Transfer ownership or delete the group.",
                errorCode = "OWNER_CANNOT_LEAVE"
            )
        }

        memberRepository.removeMember(groupId, userId)

        logger.info("User left group successfully: groupId=$groupId, userId=$userId")
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun inviteToGroup(
        groupId: String,
        inviterId: Int,
        request: InviteUserRequest
    ): InviteLinkResponse? {
        logger.info("Inviting user to group: groupId=$groupId, inviterId=$inviterId")

        // Verify inviter is a member
        val inviter = memberRepository.getMember(groupId, inviterId)
            ?: throw ForbiddenException(
                message = "You must be a member to invite others",
                errorCode = "NOT_A_MEMBER"
            )

        if (inviter.status != "ACTIVE") {
            throw ForbiddenException(
                message = "Only active members can invite others",
                errorCode = "INACTIVE_MEMBER"
            )
        }

        val expiresAt = Clock.System.now() + INVITE_EXPIRATION_DAYS

        if (request.generateLink) {
            // Generate invite link
            val inviteCode = generateInviteCode()
            memberRepository.createInvite(
                groupId = groupId,
                inviterId = inviterId,
                inviteeId = null,
                inviteCode = inviteCode,
                expiresAt = expiresAt
            )

            logger.info("Invite link created: groupId=$groupId, code=$inviteCode")
            return InviteLinkResponse(inviteCode, expiresAt)
        } else if (request.userId != null) {
            // Direct invite to user
            memberRepository.createInvite(
                groupId = groupId,
                inviterId = inviterId,
                inviteeId = request.userId,
                inviteCode = null,
                expiresAt = expiresAt
            )

            logger.info("User invited directly: groupId=$groupId, inviteeId=${request.userId}")
            return null
        } else {
            throw ValidationException(
                message = "Either userId or generateLink must be specified",
                errorCode = "INVALID_INVITE_REQUEST"
            )
        }
    }

    override suspend fun kickMember(groupId: String, adminId: Int, memberId: Int) {
        logger.info("Kicking member from group: groupId=$groupId, adminId=$adminId, memberId=$memberId")

        // Verify admin has permission
        verifyAdminPermission(groupId, adminId)

        val member = memberRepository.getMember(groupId, memberId)
            ?: throw NotFoundException(
                message = "User is not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.role == "OWNER") {
            throw ValidationException(
                message = "Cannot kick the group owner",
                errorCode = "CANNOT_KICK_OWNER"
            )
        }

        memberRepository.removeMember(groupId, memberId)

        logger.info("Member kicked successfully: groupId=$groupId, memberId=$memberId")
    }

    override suspend fun getGroupMembers(groupId: String): List<GroupMemberWithUser> {
        logger.debug("Fetching members for group: $groupId")

        // Verify group exists
        getGroupById(groupId)

        return memberRepository.getMembers(groupId, "ACTIVE")
    }

    override suspend fun getUserGroups(userId: Int): List<Group> {
        logger.debug("Fetching groups for user: $userId")

        return groupRepository.getUserGroups(userId)
    }

    override suspend fun discoverGroups(
        filters: GroupFilters,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<Group> {
        logger.debug("Discovering groups with filters: $filters")

        // Only show public groups in discovery
        val publicFilters = filters.copy(isPrivate = false)
        return getGroups(publicFilters, page, pageSize)
    }

    override suspend fun getPendingRequests(groupId: String, userId: Int): List<GroupMemberWithUser> {
        logger.debug("Fetching pending requests for group: $groupId")

        // Verify user has permission
        verifyAdminPermission(groupId, userId)

        return memberRepository.getPendingRequests(groupId)
    }

    override suspend fun approveJoinRequest(groupId: String, adminId: Int, memberId: Int) {
        logger.info("Approving join request: groupId=$groupId, adminId=$adminId, memberId=$memberId")

        // Verify admin has permission
        verifyAdminPermission(groupId, adminId)

        val member = memberRepository.getMember(groupId, memberId)
            ?: throw NotFoundException(
                message = "Join request not found",
                errorCode = "REQUEST_NOT_FOUND"
            )

        if (member.status != "PENDING") {
            throw ValidationException(
                message = "Join request is not pending",
                errorCode = "REQUEST_NOT_PENDING"
            )
        }

        memberRepository.updateMemberStatus(groupId, memberId, "ACTIVE")

        logger.info("Join request approved: groupId=$groupId, memberId=$memberId")
    }

    override suspend fun rejectJoinRequest(groupId: String, adminId: Int, memberId: Int) {
        logger.info("Rejecting join request: groupId=$groupId, adminId=$adminId, memberId=$memberId")

        // Verify admin has permission
        verifyAdminPermission(groupId, adminId)

        val member = memberRepository.getMember(groupId, memberId)
            ?: throw NotFoundException(
                message = "Join request not found",
                errorCode = "REQUEST_NOT_FOUND"
            )

        if (member.status != "PENDING") {
            throw ValidationException(
                message = "Join request is not pending",
                errorCode = "REQUEST_NOT_PENDING"
            )
        }

        memberRepository.removeMember(groupId, memberId)

        logger.info("Join request rejected: groupId=$groupId, memberId=$memberId")
    }

    // Helper methods

    private suspend fun verifyAdminPermission(groupId: String, userId: Int) {
        val member = memberRepository.getMember(groupId, userId)
            ?: throw ForbiddenException(
                message = "You are not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.role != "OWNER" && member.role != "ADMIN") {
            throw ForbiddenException(
                message = "You do not have permission to perform this action",
                errorCode = "INSUFFICIENT_PERMISSIONS"
            )
        }
    }

    private suspend fun verifyOwnerPermission(groupId: String, userId: Int) {
        val member = memberRepository.getMember(groupId, userId)
            ?: throw ForbiddenException(
                message = "You are not a member of this group",
                errorCode = "NOT_A_MEMBER"
            )

        if (member.role != "OWNER") {
            throw ForbiddenException(
                message = "Only the group owner can perform this action",
                errorCode = "OWNER_ONLY"
            )
        }
    }

    private fun validateAndNormalizePageSize(page: Int, pageSize: Int): Int {
        if (page < 1) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("page" to "Page number must be >= 1")
            )
        }

        return when {
            pageSize < 1 -> DEFAULT_PAGE_SIZE
            pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> pageSize
        }
    }

    private fun <T> paginateResults(items: List<T>, page: Int, pageSize: Int): PaginatedResponse<T> {
        val totalItems = items.size.toLong()
        val skip = (page - 1) * pageSize
        val paginatedItems = items.drop(skip).take(pageSize)
        val paginationMeta = PaginationMeta.from(page, pageSize, totalItems)

        return PaginatedResponse(
            items = paginatedItems,
            pagination = paginationMeta
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateInviteCode(): String {
        val random = SecureRandom()
        val bytes = ByteArray(INVITE_CODE_LENGTH)
        random.nextBytes(bytes)
        return Base64.UrlSafe.encode(bytes).replace("=", "").substring(0, INVITE_CODE_LENGTH)
    }
}
