package com.numina.services

import com.numina.common.exceptions.ConflictException
import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.data.repositories.*
import com.numina.domain.*
import org.slf4j.LoggerFactory
import kotlin.math.*

interface SocialService {
    suspend fun followUser(followerId: Int, followingId: Int): FollowRelationship
    suspend fun unfollowUser(followerId: Int, followingId: Int)
    suspend fun getFollowers(userId: Int, limit: Int = 20, offset: Int = 0): PaginatedResponse<FollowerInfo>
    suspend fun getFollowing(userId: Int, limit: Int = 20, offset: Int = 0): PaginatedResponse<FollowerInfo>
    suspend fun getFollowStats(userId: Int): FollowStats
    suspend fun getFollowSuggestions(userId: Int, limit: Int = 10): List<FollowSuggestion>
    suspend fun discoverUsers(userId: Int, request: DiscoverUsersRequest): PaginatedResponse<DiscoveredUser>
    suspend fun getUserProfileWithSocial(userId: Int, requesterId: Int): UserProfileWithSocial
    suspend fun getMutualConnections(userId: Int, otherUserId: Int): List<MutualConnection>
}

class SocialServiceImpl(
    private val followRepository: FollowRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userStatsRepository: UserStatsRepository
) : SocialService {
    private val logger = LoggerFactory.getLogger(SocialServiceImpl::class.java)

    override suspend fun followUser(followerId: Int, followingId: Int): FollowRelationship {
        logger.info("User $followerId attempting to follow user $followingId")

        // Validate that users are not the same
        if (followerId == followingId) {
            throw ValidationException(
                message = "Cannot follow yourself",
                details = mapOf("error" to "SELF_FOLLOW_NOT_ALLOWED")
            )
        }

        // Check if following user exists
        val followingProfile = userProfileRepository.getProfile(followingId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to followingId.toString())
            )

        // Check if already following
        if (followRepository.isFollowing(followerId, followingId)) {
            throw ConflictException(
                message = "Already following this user",
                errorCode = "ALREADY_FOLLOWING"
            )
        }

        val relationship = followRepository.follow(followerId, followingId)
            ?: throw ConflictException(
                message = "Unable to follow user",
                errorCode = "FOLLOW_FAILED"
            )

        // Update stats
        userStatsRepository.incrementFollowing(followerId)
        userStatsRepository.incrementFollowers(followingId)

        logger.info("User $followerId successfully followed user $followingId")
        return relationship
    }

    override suspend fun unfollowUser(followerId: Int, followingId: Int) {
        logger.info("User $followerId attempting to unfollow user $followingId")

        if (followerId == followingId) {
            throw ValidationException(
                message = "Invalid operation",
                details = mapOf("error" to "INVALID_OPERATION")
            )
        }

        val success = followRepository.unfollow(followerId, followingId)
        if (!success) {
            throw NotFoundException(
                message = "Follow relationship not found",
                errorCode = "NOT_FOLLOWING"
            )
        }

        // Update stats
        userStatsRepository.decrementFollowing(followerId)
        userStatsRepository.decrementFollowers(followingId)

        logger.info("User $followerId successfully unfollowed user $followingId")
    }

    override suspend fun getFollowers(userId: Int, limit: Int, offset: Int): PaginatedResponse<FollowerInfo> {
        logger.debug("Fetching followers for user $userId (limit=$limit, offset=$offset)")

        val followers = followRepository.getFollowers(userId, limit, offset)
        val total = followRepository.getFollowersCount(userId)

        return PaginatedResponse(
            data = followers,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + followers.size < total
        )
    }

    override suspend fun getFollowing(userId: Int, limit: Int, offset: Int): PaginatedResponse<FollowerInfo> {
        logger.debug("Fetching following for user $userId (limit=$limit, offset=$offset)")

        val following = followRepository.getFollowing(userId, limit, offset)
        val total = followRepository.getFollowingCount(userId)

        return PaginatedResponse(
            data = following,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + following.size < total
        )
    }

    override suspend fun getFollowStats(userId: Int): FollowStats {
        val followersCount = followRepository.getFollowersCount(userId)
        val followingCount = followRepository.getFollowingCount(userId)

        return FollowStats(
            followersCount = followersCount,
            followingCount = followingCount
        )
    }

    override suspend fun getFollowSuggestions(userId: Int, limit: Int): List<FollowSuggestion> {
        logger.debug("Getting follow suggestions for user $userId")

        val userProfile = userProfileRepository.getProfile(userId)
            ?: return emptyList()

        // For now, return empty list - would need more complex logic
        // In a real implementation, this would:
        // 1. Find users with similar interests
        // 2. Find users nearby
        // 3. Find users followed by people you follow
        // 4. Filter out users already followed

        return emptyList()
    }

    override suspend fun discoverUsers(userId: Int, request: DiscoverUsersRequest): PaginatedResponse<DiscoveredUser> {
        logger.debug("Discovering users for user $userId with filters: $request")

        // This is a simplified implementation
        // In production, this would use more sophisticated filtering and search

        return PaginatedResponse(
            data = emptyList(),
            total = 0,
            limit = request.limit,
            offset = request.offset,
            hasMore = false
        )
    }

    override suspend fun getUserProfileWithSocial(userId: Int, requesterId: Int): UserProfileWithSocial {
        logger.debug("Fetching profile with social info for user $userId (requester=$requesterId)")

        val profile = userProfileRepository.getPublicProfile(userId, requesterId)
            ?: throw NotFoundException(
                message = "User not found",
                errorCode = "USER_NOT_FOUND",
                details = mapOf("userId" to userId.toString())
            )

        val stats = userStatsRepository.getStats(userId)
        val isFollowing = followRepository.isFollowing(requesterId, userId)
        val isFollower = followRepository.isFollowing(userId, requesterId)
        val mutualCount = followRepository.getMutualConnectionsCount(requesterId, userId)

        return UserProfileWithSocial(
            userId = profile.userId,
            name = profile.name,
            bio = profile.bio,
            photoUrl = profile.photoUrl,
            fitnessInterests = profile.fitnessInterests ?: emptyList(),
            fitnessLevel = profile.fitnessLevel,
            stats = stats,
            isFollowing = isFollowing,
            isFollower = isFollower,
            mutualFollowers = mutualCount
        )
    }

    override suspend fun getMutualConnections(userId: Int, otherUserId: Int): List<MutualConnection> {
        logger.debug("Fetching mutual connections between $userId and $otherUserId")
        return followRepository.getMutualConnections(userId, otherUserId)
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }
}
