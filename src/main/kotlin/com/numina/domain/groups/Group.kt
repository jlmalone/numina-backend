package com.numina.domain.groups

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val photoUrl: String?,
    val category: String,
    val isPrivate: Boolean,
    val maxMembers: Int,
    val ownerId: Int,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val memberCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class GroupCategory {
    RUNNING,
    YOGA,
    HIIT,
    CYCLING,
    GENERAL,
    STRENGTH,
    DANCE,
    MARTIAL_ARTS,
    SWIMMING,
    OTHER
}

@Serializable
enum class MemberRole {
    OWNER,
    ADMIN,
    MEMBER
}

@Serializable
enum class MemberStatus {
    ACTIVE,
    PENDING,
    REMOVED
}

@Serializable
data class GroupMember(
    val id: String,
    val groupId: String,
    val userId: Int,
    val role: String,
    val status: String,
    val joinedAt: Instant
)

@Serializable
data class GroupActivity(
    val id: String,
    val groupId: String,
    val classId: Int?,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isRecurring: Boolean,
    val recurrenceRule: String?,
    val createdById: Int,
    val createdAt: Instant,
    val cancelled: Boolean,
    val rsvpStats: RSVPStats? = null
)

@Serializable
data class RSVPStats(
    val going: Int = 0,
    val maybe: Int = 0,
    val notGoing: Int = 0
)

@Serializable
enum class RSVPStatus {
    GOING,
    MAYBE,
    NOT_GOING
}

@Serializable
data class ActivityRSVP(
    val id: String,
    val activityId: String,
    val userId: Int,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
enum class InviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}

@Serializable
data class GroupInvite(
    val id: String,
    val groupId: String,
    val inviterId: Int,
    val inviteeId: Int?,
    val inviteCode: String?,
    val status: String,
    val createdAt: Instant,
    val expiresAt: Instant
)

// DTOs for API requests/responses

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String?,
    val photoUrl: String?,
    val category: String,
    val isPrivate: Boolean = false,
    val maxMembers: Int = 50,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?
)

@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val category: String? = null,
    val isPrivate: Boolean? = null,
    val maxMembers: Int? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class GroupFilters(
    val category: String? = null,
    val isPrivate: Boolean? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = null,
    val minMembers: Int? = null,
    val maxMembers: Int? = null,
    val search: String? = null
)

@Serializable
data class JoinGroupRequest(
    val message: String? = null
)

@Serializable
data class InviteUserRequest(
    val userId: Int? = null,
    val generateLink: Boolean = false
)

@Serializable
data class InviteLinkResponse(
    val inviteCode: String,
    val expiresAt: Instant
)

@Serializable
data class CreateActivityRequest(
    val classId: Int? = null,
    val title: String,
    val description: String?,
    val scheduledAt: Instant,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isRecurring: Boolean = false,
    val recurrenceRule: String? = null
)

@Serializable
data class UpdateActivityRequest(
    val title: String? = null,
    val description: String? = null,
    val scheduledAt: Instant? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isRecurring: Boolean? = null,
    val recurrenceRule: String? = null
)

@Serializable
data class RSVPRequest(
    val status: String
)

@Serializable
data class GroupMemberWithUser(
    val id: String,
    val groupId: String,
    val userId: Int,
    val userEmail: String,
    val userName: String?,
    val role: String,
    val status: String,
    val joinedAt: Instant
)
