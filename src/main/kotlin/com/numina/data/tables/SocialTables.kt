package com.numina.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Table for tracking follow relationships between users
 */
object Follows : UUIDTable("follows") {
    val followerId = integer("follower_id").references(Users.id)
    val followingId = integer("following_id").references(Users.id)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(followerId, followingId)
        index(false, followerId)
        index(false, followingId)
    }
}

/**
 * Table for user activity feed items
 */
object ActivityFeed : UUIDTable("activity_feed") {
    val userId = integer("user_id").references(Users.id)
    val activityType = varchar("activity_type", 50) // workout_completed, group_joined, review_posted, milestone_achieved, class_attended
    val content = text("content")
    val metadata = text("metadata") // JSON string
    val visibility = varchar("visibility", 20).default("public") // public, followers, private
    val createdAt = timestamp("created_at")

    init {
        index(false, userId)
        index(false, createdAt)
        index(false, visibility)
    }
}

/**
 * Table for activity likes
 */
object ActivityLikes : UUIDTable("activity_likes") {
    val activityId = uuid("activity_id").references(ActivityFeed.id)
    val userId = integer("user_id").references(Users.id)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex(activityId, userId)
        index(false, activityId)
    }
}

/**
 * Table for activity comments
 */
object ActivityComments : UUIDTable("activity_comments") {
    val activityId = uuid("activity_id").references(ActivityFeed.id)
    val userId = integer("user_id").references(Users.id)
    val content = text("content")
    val createdAt = timestamp("created_at")

    init {
        index(false, activityId)
        index(false, userId)
    }
}

/**
 * Denormalized table for user statistics (for performance)
 */
object UserStats : UUIDTable("user_stats") {
    val userId = integer("user_id").references(Users.id).uniqueIndex()
    val followersCount = integer("followers_count").default(0)
    val followingCount = integer("following_count").default(0)
    val activitiesCount = integer("activities_count").default(0)
    val workoutsCount = integer("workouts_count").default(0)
    val updatedAt = timestamp("updated_at")
}
