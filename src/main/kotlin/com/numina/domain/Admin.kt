package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import java.util.UUID

@Serializable
data class AdminUser(
    val id: String,
    val userId: Int,
    val role: AdminRole,
    val permissions: Map<String, Boolean>,
    val createdAt: Instant
)

@Serializable
enum class AdminRole {
    SUPER_ADMIN,
    ADMIN,
    MODERATOR
}

@Serializable
data class AdminAuditLogEntry(
    val id: String,
    val adminUserId: Int,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val changes: Map<String, String>,
    val ipAddress: String?,
    val createdAt: Instant
)

@Serializable
data class FeatureFlag(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val description: String?,
    val rolloutPercentage: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class SystemSetting(
    val key: String,
    val value: String,
    val type: SettingType,
    val description: String?,
    val updatedAt: Instant
)

@Serializable
enum class SettingType {
    STRING,
    INT,
    BOOLEAN,
    JSON
}

// Request/Response models
@Serializable
data class CreateAdminRequest(
    val userId: Int,
    val role: AdminRole,
    val permissions: Map<String, Boolean> = emptyMap()
)

@Serializable
data class UpdateFeatureFlagRequest(
    val enabled: Boolean?,
    val description: String?,
    val rolloutPercentage: Int?
)

@Serializable
data class CreateFeatureFlagRequest(
    val name: String,
    val enabled: Boolean = false,
    val description: String?,
    val rolloutPercentage: Int = 100
)

@Serializable
data class UpdateSystemSettingRequest(
    val value: String
)

@Serializable
data class BroadcastNotificationRequest(
    val title: String,
    val message: String,
    val targetUserIds: List<Int>? = null // null means all users
)

@Serializable
data class SuspendUserRequest(
    val reason: String,
    val durationDays: Int? = null // null means permanent
)

@Serializable
data class ResetPasswordRequest(
    val newPassword: String
)

@Serializable
data class UserListResponse(
    val users: List<UserSummary>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

@Serializable
data class UserSummary(
    val id: Int,
    val email: String,
    val name: String?,
    val createdAt: Instant,
    val isSuspended: Boolean
)

@Serializable
data class UserDetailResponse(
    val id: Int,
    val email: String,
    val profile: UserProfile?,
    val createdAt: Instant,
    val isSuspended: Boolean,
    val activitySummary: UserActivitySummary
)

@Serializable
data class UserActivitySummary(
    val totalClasses: Int,
    val totalReviews: Int,
    val totalMessages: Int,
    val lastActive: Instant?
)

@Serializable
data class AnalyticsResponse(
    val metrics: Map<String, Any>
)
