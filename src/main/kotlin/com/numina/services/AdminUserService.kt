package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.UserRepository
import com.numina.data.repositories.UserProfileRepository
import com.numina.domain.*
import org.slf4j.LoggerFactory

interface AdminUserService {
    suspend fun listUsers(page: Int = 0, pageSize: Int = 50): UserListResponse
    suspend fun searchUsers(query: String): List<UserSummary>
    suspend fun getUserDetails(userId: Int): UserDetailResponse
    suspend fun suspendUser(userId: Int, request: SuspendUserRequest, adminUserId: Int, auditLogService: AuditLogService, ipAddress: String?): Boolean
    suspend fun unsuspendUser(userId: Int, adminUserId: Int, auditLogService: AuditLogService, ipAddress: String?): Boolean
    suspend fun resetUserPassword(userId: Int, request: ResetPasswordRequest, adminUserId: Int, auditLogService: AuditLogService, ipAddress: String?): Boolean
}

class AdminUserServiceImpl(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository
) : AdminUserService {
    private val logger = LoggerFactory.getLogger(AdminUserServiceImpl::class.java)

    override suspend fun listUsers(page: Int, pageSize: Int): UserListResponse {
        logger.debug("Listing users: page=$page, pageSize=$pageSize")

        if (page < 0 || pageSize <= 0 || pageSize > 100) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("pagination" to "Invalid page or pageSize")
            )
        }

        val offset = page * pageSize
        val users = userRepository.getAllUsers(limit = pageSize, offset = offset)
        val total = userRepository.getUserCount()

        val userSummaries = users.map { user ->
            val profile = userProfileRepository.getProfile(user.id)
            UserSummary(
                id = user.id,
                email = user.email,
                name = profile?.name,
                createdAt = user.createdAt,
                isSuspended = userRepository.isSuspended(user.id)
            )
        }

        return UserListResponse(
            users = userSummaries,
            total = total,
            page = page,
            pageSize = pageSize
        )
    }

    override suspend fun searchUsers(query: String): List<UserSummary> {
        logger.debug("Searching users: query=$query")

        if (query.isBlank()) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("query" to "Search query cannot be empty")
            )
        }

        val users = userRepository.searchUsers(query)
        return users.map { user ->
            val profile = userProfileRepository.getProfile(user.id)
            UserSummary(
                id = user.id,
                email = user.email,
                name = profile?.name,
                createdAt = user.createdAt,
                isSuspended = userRepository.isSuspended(user.id)
            )
        }
    }

    override suspend fun getUserDetails(userId: Int): UserDetailResponse {
        logger.debug("Fetching user details: userId=$userId")

        val user = userRepository.getUserById(userId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        val profile = userProfileRepository.getProfile(userId)
        val isSuspended = userRepository.isSuspended(userId)

        // Mock activity summary - in real app would query from actual data
        val activitySummary = UserActivitySummary(
            totalClasses = 0,
            totalReviews = 0,
            totalMessages = 0,
            lastActive = user.updatedAt
        )

        return UserDetailResponse(
            id = user.id,
            email = user.email,
            profile = profile,
            createdAt = user.createdAt,
            isSuspended = isSuspended,
            activitySummary = activitySummary
        )
    }

    override suspend fun suspendUser(
        userId: Int,
        request: SuspendUserRequest,
        adminUserId: Int,
        auditLogService: AuditLogService,
        ipAddress: String?
    ): Boolean {
        logger.info("Suspending user: userId=$userId, adminUserId=$adminUserId")

        // Verify user exists
        userRepository.getUserById(userId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        val success = userRepository.suspendUser(userId, request.reason)

        if (success) {
            // Log the action
            auditLogService.logAction(
                adminUserId = adminUserId,
                action = "SUSPEND_USER",
                entityType = "USER",
                entityId = userId.toString(),
                changes = mapOf(
                    "reason" to request.reason,
                    "durationDays" to (request.durationDays?.toString() ?: "permanent")
                ),
                ipAddress = ipAddress
            )
        }

        return success
    }

    override suspend fun unsuspendUser(
        userId: Int,
        adminUserId: Int,
        auditLogService: AuditLogService,
        ipAddress: String?
    ): Boolean {
        logger.info("Unsuspending user: userId=$userId, adminUserId=$adminUserId")

        // Verify user exists
        userRepository.getUserById(userId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        val success = userRepository.unsuspendUser(userId)

        if (success) {
            // Log the action
            auditLogService.logAction(
                adminUserId = adminUserId,
                action = "UNSUSPEND_USER",
                entityType = "USER",
                entityId = userId.toString(),
                changes = emptyMap(),
                ipAddress = ipAddress
            )
        }

        return success
    }

    override suspend fun resetUserPassword(
        userId: Int,
        request: ResetPasswordRequest,
        adminUserId: Int,
        auditLogService: AuditLogService,
        ipAddress: String?
    ): Boolean {
        logger.info("Resetting user password: userId=$userId, adminUserId=$adminUserId")

        // Validate password
        if (request.newPassword.length < 8) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("newPassword" to "Password must be at least 8 characters")
            )
        }

        // Verify user exists
        userRepository.getUserById(userId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        val success = userRepository.resetPassword(userId, request.newPassword)

        if (success) {
            // Log the action
            auditLogService.logAction(
                adminUserId = adminUserId,
                action = "RESET_PASSWORD",
                entityType = "USER",
                entityId = userId.toString(),
                changes = mapOf("action" to "password_reset"),
                ipAddress = ipAddress
            )
        }

        return success
    }
}
