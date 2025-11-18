package com.numina.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

// ========== Follow Models ==========

@Serializable
data class FollowRelationship(
    val id: String,
    val followerId: Int,
    val followingId: Int,
    val createdAt: Instant
)

@Serializable
data class FollowerInfo(
    val userId: Int,
    val name: String,
    val photoUrl: String? = null,
    val bio: String? = null,
    val followedAt: Instant,
    val isFollowingBack: Boolean = false
)

@Serializable
data class FollowSuggestion(
    val userId: Int,
    val name: String,
    val photoUrl: String? = null,
    val bio: String? = null,
    val matchReason: String, // e.g., "Similar fitness level", "Shared interests", "Nearby"
    val mutualFollowers: Int = 0,
    val sharedInterests: List<String> = emptyList()
)

@Serializable
data class FollowStats(
    val followersCount: Int,
    val followingCount: Int
)

// ========== Activity Feed Models ==========

@Serializable
data class Activity(
    val id: String,
    val userId: Int,
    val userName: String,
    val userPhotoUrl: String? = null,
    val activityType: ActivityType,
    val content: String,
    val metadata: ActivityMetadata? = null,
    val visibility: ActivityVisibility,
    val createdAt: Instant,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false
)

@Serializable
enum class ActivityType {
    WORKOUT_COMPLETED,
    GROUP_JOINED,
    REVIEW_POSTED,
    MILESTONE_ACHIEVED,
    CLASS_ATTENDED
}

@Serializable
enum class ActivityVisibility {
    PUBLIC,
    FOLLOWERS,
    PRIVATE
}

@Serializable
data class ActivityMetadata(
    val classId: Int? = null,
    val className: String? = null,
    val groupId: String? = null,
    val groupName: String? = null,
    val workoutType: String? = null,
    val workoutDuration: Int? = null, // in minutes
    val achievementType: String? = null,
    val achievementDetails: String? = null
)

@Serializable
data class CreateActivityRequest(
    val activityType: ActivityType,
    val content: String,
    val metadata: ActivityMetadata? = null,
    val visibility: ActivityVisibility = ActivityVisibility.PUBLIC
)

@Serializable
data class ActivityLike(
    val id: String,
    val activityId: String,
    val userId: Int,
    val userName: String,
    val userPhotoUrl: String? = null,
    val createdAt: Instant
)

@Serializable
data class ActivityComment(
    val id: String,
    val activityId: String,
    val userId: Int,
    val userName: String,
    val userPhotoUrl: String? = null,
    val content: String,
    val createdAt: Instant
)

@Serializable
data class CreateCommentRequest(
    val content: String
)

// ========== User Discovery Models ==========

@Serializable
data class DiscoverUsersRequest(
    val query: String? = null,
    val locationLat: Double? = null,
    val locationLong: Double? = null,
    val radiusKm: Double? = null,
    val fitnessInterests: List<String>? = null,
    val fitnessLevel: Int? = null,
    val minActivityLevel: Int? = null, // Minimum workouts per week
    val limit: Int = 20,
    val offset: Int = 0
)

@Serializable
data class DiscoveredUser(
    val userId: Int,
    val name: String,
    val photoUrl: String? = null,
    val bio: String? = null,
    val fitnessInterests: List<String> = emptyList(),
    val fitnessLevel: Int? = null,
    val distanceKm: Double? = null,
    val isFollowing: Boolean = false,
    val mutualFollowers: Int = 0
)

// ========== User Profile Models (Extended) ==========

@Serializable
data class UserProfileWithSocial(
    val userId: Int,
    val name: String,
    val bio: String? = null,
    val photoUrl: String? = null,
    val fitnessInterests: List<String> = emptyList(),
    val fitnessLevel: Int? = null,
    val stats: UserSocialStats,
    val isFollowing: Boolean = false,
    val isFollower: Boolean = false,
    val mutualFollowers: Int = 0
)

@Serializable
data class UserSocialStats(
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val activitiesCount: Int = 0,
    val workoutsCount: Int = 0
)

@Serializable
data class MutualConnection(
    val userId: Int,
    val name: String,
    val photoUrl: String? = null
)

// ========== Pagination Models ==========

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)
