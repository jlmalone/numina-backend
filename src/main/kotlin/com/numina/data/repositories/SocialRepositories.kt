package com.numina.data.repositories

import com.numina.data.tables.*
import com.numina.domain.*
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlinx.serialization.json.Json

// ========== Follow Repository ==========

interface FollowRepository {
    suspend fun follow(followerId: Int, followingId: Int): FollowRelationship?
    suspend fun unfollow(followerId: Int, followingId: Int): Boolean
    suspend fun isFollowing(followerId: Int, followingId: Int): Boolean
    suspend fun getFollowers(userId: Int, limit: Int, offset: Int): List<FollowerInfo>
    suspend fun getFollowing(userId: Int, limit: Int, offset: Int): List<FollowerInfo>
    suspend fun getFollowersCount(userId: Int): Int
    suspend fun getFollowingCount(userId: Int): Int
    suspend fun getMutualConnections(userId1: Int, userId2: Int): List<MutualConnection>
    suspend fun getMutualConnectionsCount(userId1: Int, userId2: Int): Int
}

class FollowRepositoryImpl : FollowRepository {
    override suspend fun follow(followerId: Int, followingId: Int): FollowRelationship? = transaction {
        val now = kotlinx.datetime.Clock.System.now()

        // Check if already following
        val existing = Follows.select {
            (Follows.followerId eq followerId) and (Follows.followingId eq followingId)
        }.singleOrNull()

        if (existing != null) return@transaction null

        val id = Follows.insert {
            it[Follows.followerId] = followerId
            it[Follows.followingId] = followingId
            it[createdAt] = now.toJavaInstant()
        }[Follows.id]

        FollowRelationship(
            id = id.value.toString(),
            followerId = followerId,
            followingId = followingId,
            createdAt = now
        )
    }

    override suspend fun unfollow(followerId: Int, followingId: Int): Boolean = transaction {
        val deleted = Follows.deleteWhere {
            (Follows.followerId eq followerId) and (Follows.followingId eq followingId)
        }
        deleted > 0
    }

    override suspend fun isFollowing(followerId: Int, followingId: Int): Boolean = transaction {
        Follows.select {
            (Follows.followerId eq followerId) and (Follows.followingId eq followingId)
        }.count() > 0
    }

    override suspend fun getFollowers(userId: Int, limit: Int, offset: Int): List<FollowerInfo> = transaction {
        Follows
            .innerJoin(UserProfiles, { followerId }, { UserProfiles.userId })
            .select { Follows.followingId eq userId }
            .orderBy(Follows.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val followerId = row[Follows.followerId]
                FollowerInfo(
                    userId = followerId,
                    name = row[UserProfiles.name],
                    photoUrl = row[UserProfiles.photoUrl],
                    bio = row[UserProfiles.bio],
                    followedAt = row[Follows.createdAt].toKotlinInstant(),
                    isFollowingBack = isFollowing(userId, followerId)
                )
            }
    }

    override suspend fun getFollowing(userId: Int, limit: Int, offset: Int): List<FollowerInfo> = transaction {
        Follows
            .innerJoin(UserProfiles, { followingId }, { UserProfiles.userId })
            .select { Follows.followerId eq userId }
            .orderBy(Follows.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                val followingId = row[Follows.followingId]
                FollowerInfo(
                    userId = followingId,
                    name = row[UserProfiles.name],
                    photoUrl = row[UserProfiles.photoUrl],
                    bio = row[UserProfiles.bio],
                    followedAt = row[Follows.createdAt].toKotlinInstant(),
                    isFollowingBack = isFollowing(followingId, userId)
                )
            }
    }

    override suspend fun getFollowersCount(userId: Int): Int = transaction {
        Follows.select { Follows.followingId eq userId }.count().toInt()
    }

    override suspend fun getFollowingCount(userId: Int): Int = transaction {
        Follows.select { Follows.followerId eq userId }.count().toInt()
    }

    override suspend fun getMutualConnections(userId1: Int, userId2: Int): List<MutualConnection> = transaction {
        // Find users that both userId1 and userId2 follow
        val user1Following = Follows.select { Follows.followerId eq userId1 }
            .map { it[Follows.followingId] }

        Follows
            .innerJoin(UserProfiles, { followingId }, { UserProfiles.userId })
            .select {
                (Follows.followerId eq userId2) and (Follows.followingId inList user1Following)
            }
            .limit(10)
            .map { row ->
                MutualConnection(
                    userId = row[Follows.followingId],
                    name = row[UserProfiles.name],
                    photoUrl = row[UserProfiles.photoUrl]
                )
            }
    }

    override suspend fun getMutualConnectionsCount(userId1: Int, userId2: Int): Int = transaction {
        val user1Following = Follows.select { Follows.followerId eq userId1 }
            .map { it[Follows.followingId] }

        Follows.select {
            (Follows.followerId eq userId2) and (Follows.followingId inList user1Following)
        }.count().toInt()
    }
}

// ========== Activity Feed Repository ==========

interface ActivityFeedRepository {
    suspend fun createActivity(userId: Int, request: CreateActivityRequest): Activity?
    suspend fun getActivity(activityId: String, currentUserId: Int): Activity?
    suspend fun deleteActivity(activityId: String, userId: Int): Boolean
    suspend fun getFeed(userId: Int, limit: Int, offset: Int): List<Activity>
    suspend fun getUserActivities(userId: Int, currentUserId: Int, limit: Int, offset: Int): List<Activity>
    suspend fun likeActivity(activityId: String, userId: Int): ActivityLike?
    suspend fun unlikeActivity(activityId: String, userId: Int): Boolean
    suspend fun getLikes(activityId: String, limit: Int, offset: Int): List<ActivityLike>
    suspend fun getLikesCount(activityId: String): Int
    suspend fun addComment(activityId: String, userId: Int, content: String): ActivityComment?
    suspend fun getComments(activityId: String, limit: Int, offset: Int): List<ActivityComment>
    suspend fun getCommentsCount(activityId: String): Int
    suspend fun isLikedByUser(activityId: String, userId: Int): Boolean
}

class ActivityFeedRepositoryImpl : ActivityFeedRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun resultRowToActivity(row: ResultRow, currentUserId: Int): Activity {
        val activityId = row[ActivityFeed.id].value.toString()
        val metadataJson = row[ActivityFeed.metadata]
        val metadata = if (metadataJson.isNotBlank()) {
            try {
                json.decodeFromString<ActivityMetadata>(metadataJson)
            } catch (e: Exception) {
                null
            }
        } else null

        return Activity(
            id = activityId,
            userId = row[ActivityFeed.userId],
            userName = row[UserProfiles.name],
            userPhotoUrl = row[UserProfiles.photoUrl],
            activityType = ActivityType.valueOf(row[ActivityFeed.activityType].uppercase()),
            content = row[ActivityFeed.content],
            metadata = metadata,
            visibility = ActivityVisibility.valueOf(row[ActivityFeed.visibility].uppercase()),
            createdAt = row[ActivityFeed.createdAt].toKotlinInstant(),
            likesCount = getLikesCount(activityId),
            commentsCount = getCommentsCount(activityId),
            isLikedByCurrentUser = isLikedByUser(activityId, currentUserId)
        )
    }

    override suspend fun createActivity(userId: Int, request: CreateActivityRequest): Activity? = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        val metadataJson = request.metadata?.let { json.encodeToString(ActivityMetadata.serializer(), it) } ?: ""

        val id = ActivityFeed.insert {
            it[ActivityFeed.userId] = userId
            it[activityType] = request.activityType.name.lowercase()
            it[content] = request.content
            it[metadata] = metadataJson
            it[visibility] = request.visibility.name.lowercase()
            it[createdAt] = now.toJavaInstant()
        }[ActivityFeed.id]

        getActivity(id.value.toString(), userId)
    }

    override suspend fun getActivity(activityId: String, currentUserId: Int): Activity? = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityFeed
                .innerJoin(UserProfiles, { userId }, { UserProfiles.userId })
                .select { ActivityFeed.id eq uuid }
                .map { resultRowToActivity(it, currentUserId) }
                .singleOrNull()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun deleteActivity(activityId: String, userId: Int): Boolean = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            val deleted = ActivityFeed.deleteWhere {
                (ActivityFeed.id eq uuid) and (ActivityFeed.userId eq userId)
            }
            deleted > 0
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override suspend fun getFeed(userId: Int, limit: Int, offset: Int): List<Activity> = transaction {
        // Get IDs of users that the current user follows
        val followingIds = Follows.select { Follows.followerId eq userId }
            .map { it[Follows.followingId] }

        // Include the user's own activities
        val userIds = followingIds + userId

        ActivityFeed
            .innerJoin(UserProfiles, { ActivityFeed.userId }, { UserProfiles.userId })
            .select {
                (ActivityFeed.userId inList userIds) and
                ((ActivityFeed.visibility eq "public") or (ActivityFeed.visibility eq "followers"))
            }
            .orderBy(ActivityFeed.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { resultRowToActivity(it, userId) }
    }

    override suspend fun getUserActivities(userId: Int, currentUserId: Int, limit: Int, offset: Int): List<Activity> = transaction {
        val isFollowing = Follows.select {
            (Follows.followerId eq currentUserId) and (Follows.followingId eq userId)
        }.count() > 0

        val visibilityFilter = when {
            userId == currentUserId -> Op.TRUE // User can see all their own activities
            isFollowing -> (ActivityFeed.visibility eq "public") or (ActivityFeed.visibility eq "followers")
            else -> ActivityFeed.visibility eq "public"
        }

        ActivityFeed
            .innerJoin(UserProfiles, { ActivityFeed.userId }, { UserProfiles.userId })
            .select { (ActivityFeed.userId eq userId) and visibilityFilter }
            .orderBy(ActivityFeed.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { resultRowToActivity(it, currentUserId) }
    }

    override suspend fun likeActivity(activityId: String, userId: Int): ActivityLike? = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            val now = kotlinx.datetime.Clock.System.now()

            // Check if already liked
            val existing = ActivityLikes.select {
                (ActivityLikes.activityId eq uuid) and (ActivityLikes.userId eq userId)
            }.singleOrNull()

            if (existing != null) return@transaction null

            val id = ActivityLikes.insert {
                it[ActivityLikes.activityId] = uuid
                it[ActivityLikes.userId] = userId
                it[createdAt] = now.toJavaInstant()
            }[ActivityLikes.id]

            val userProfile = UserProfiles.select { UserProfiles.userId eq userId }.singleOrNull()

            ActivityLike(
                id = id.value.toString(),
                activityId = activityId,
                userId = userId,
                userName = userProfile?.get(UserProfiles.name) ?: "Unknown",
                userPhotoUrl = userProfile?.get(UserProfiles.photoUrl),
                createdAt = now
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun unlikeActivity(activityId: String, userId: Int): Boolean = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            val deleted = ActivityLikes.deleteWhere {
                (ActivityLikes.activityId eq uuid) and (ActivityLikes.userId eq userId)
            }
            deleted > 0
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override suspend fun getLikes(activityId: String, limit: Int, offset: Int): List<ActivityLike> = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityLikes
                .innerJoin(UserProfiles, { userId }, { UserProfiles.userId })
                .select { ActivityLikes.activityId eq uuid }
                .orderBy(ActivityLikes.createdAt to SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { row ->
                    ActivityLike(
                        id = row[ActivityLikes.id].value.toString(),
                        activityId = activityId,
                        userId = row[ActivityLikes.userId],
                        userName = row[UserProfiles.name],
                        userPhotoUrl = row[UserProfiles.photoUrl],
                        createdAt = row[ActivityLikes.createdAt].toKotlinInstant()
                    )
                }
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    override suspend fun getLikesCount(activityId: String): Int = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityLikes.select { ActivityLikes.activityId eq uuid }.count().toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    override suspend fun addComment(activityId: String, userId: Int, content: String): ActivityComment? = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            val now = kotlinx.datetime.Clock.System.now()

            val id = ActivityComments.insert {
                it[ActivityComments.activityId] = uuid
                it[ActivityComments.userId] = userId
                it[ActivityComments.content] = content
                it[createdAt] = now.toJavaInstant()
            }[ActivityComments.id]

            val userProfile = UserProfiles.select { UserProfiles.userId eq userId }.singleOrNull()

            ActivityComment(
                id = id.value.toString(),
                activityId = activityId,
                userId = userId,
                userName = userProfile?.get(UserProfiles.name) ?: "Unknown",
                userPhotoUrl = userProfile?.get(UserProfiles.photoUrl),
                content = content,
                createdAt = now
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override suspend fun getComments(activityId: String, limit: Int, offset: Int): List<ActivityComment> = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityComments
                .innerJoin(UserProfiles, { userId }, { UserProfiles.userId })
                .select { ActivityComments.activityId eq uuid }
                .orderBy(ActivityComments.createdAt to SortOrder.ASC)
                .limit(limit, offset.toLong())
                .map { row ->
                    ActivityComment(
                        id = row[ActivityComments.id].value.toString(),
                        activityId = activityId,
                        userId = row[ActivityComments.userId],
                        userName = row[UserProfiles.name],
                        userPhotoUrl = row[UserProfiles.photoUrl],
                        content = row[ActivityComments.content],
                        createdAt = row[ActivityComments.createdAt].toKotlinInstant()
                    )
                }
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    override suspend fun getCommentsCount(activityId: String): Int = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityComments.select { ActivityComments.activityId eq uuid }.count().toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    override suspend fun isLikedByUser(activityId: String, userId: Int): Boolean = transaction {
        try {
            val uuid = UUID.fromString(activityId)
            ActivityLikes.select {
                (ActivityLikes.activityId eq uuid) and (ActivityLikes.userId eq userId)
            }.count() > 0
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

// ========== User Stats Repository ==========

interface UserStatsRepository {
    suspend fun getStats(userId: Int): UserSocialStats
    suspend fun initializeStats(userId: Int): UserSocialStats
    suspend fun incrementFollowers(userId: Int)
    suspend fun decrementFollowers(userId: Int)
    suspend fun incrementFollowing(userId: Int)
    suspend fun decrementFollowing(userId: Int)
    suspend fun incrementActivities(userId: Int)
    suspend fun incrementWorkouts(userId: Int)
}

class UserStatsRepositoryImpl : UserStatsRepository {
    override suspend fun getStats(userId: Int): UserSocialStats = transaction {
        val stats = UserStats.select { UserStats.userId eq userId }.singleOrNull()

        if (stats == null) {
            initializeStats(userId)
        } else {
            UserSocialStats(
                followersCount = stats[UserStats.followersCount],
                followingCount = stats[UserStats.followingCount],
                activitiesCount = stats[UserStats.activitiesCount],
                workoutsCount = stats[UserStats.workoutsCount]
            )
        }
    }

    override suspend fun initializeStats(userId: Int): UserSocialStats = transaction {
        val now = kotlinx.datetime.Clock.System.now()

        UserStats.insert {
            it[UserStats.userId] = userId
            it[followersCount] = 0
            it[followingCount] = 0
            it[activitiesCount] = 0
            it[workoutsCount] = 0
            it[updatedAt] = now.toJavaInstant()
        }

        UserSocialStats(0, 0, 0, 0)
    }

    override suspend fun incrementFollowers(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[followersCount] = followersCount + 1
            it[updatedAt] = now.toJavaInstant()
        }
    }

    override suspend fun decrementFollowers(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[followersCount] = followersCount - 1
            it[updatedAt] = now.toJavaInstant()
        }
    }

    override suspend fun incrementFollowing(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[followingCount] = followingCount + 1
            it[updatedAt] = now.toJavaInstant()
        }
    }

    override suspend fun decrementFollowing(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[followingCount] = followingCount - 1
            it[updatedAt] = now.toJavaInstant()
        }
    }

    override suspend fun incrementActivities(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[activitiesCount] = activitiesCount + 1
            it[updatedAt] = now.toJavaInstant()
        }
    }

    override suspend fun incrementWorkouts(userId: Int) = transaction {
        val now = kotlinx.datetime.Clock.System.now()
        UserStats.update({ UserStats.userId eq userId }) {
            it[workoutsCount] = workoutsCount + 1
            it[updatedAt] = now.toJavaInstant()
        }
    }
}

// Helper extension to convert Java Instant to Kotlin Instant
private fun java.time.Instant.toKotlinInstant(): Instant {
    return Instant.fromEpochSeconds(this.epochSecond, this.nano)
}

private fun Instant.toJavaInstant(): java.time.Instant {
    return java.time.Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())
}
