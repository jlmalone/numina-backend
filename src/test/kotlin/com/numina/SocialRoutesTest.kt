package com.numina

import com.numina.domain.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class SocialRoutesTest {

    private suspend fun registerAndGetToken(client: io.ktor.client.HttpClient, email: String, name: String): String {
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = email,
                password = "password123",
                name = name
            ))
        }
        val loginResponse = response.body<LoginResponse>()
        return loginResponse.token
    }

    @Test
    fun testFollowUser() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        val token2 = registerAndGetToken(client, "user2@example.com", "User Two")

        // User 1 follows User 2 (assuming user IDs are sequential starting from 1)
        val response = client.post("/api/v1/social/follow/2") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token1")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val relationship = response.body<FollowRelationship>()
        assertNotNull(relationship.id)
        assertEquals(1, relationship.followerId)
        assertEquals(2, relationship.followingId)
    }

    @Test
    fun testCannotFollowSelf() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Try to follow self
        val response = client.post("/api/v1/social/follow/1") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testUnfollowUser() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        registerAndGetToken(client, "user2@example.com", "User Two")

        // Follow user
        client.post("/api/v1/social/follow/2") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token1")
        }

        // Unfollow user
        val response = client.delete("/api/v1/social/unfollow/2") {
            header("Authorization", "Bearer $token1")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun testGetFollowers() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register three users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        val token2 = registerAndGetToken(client, "user2@example.com", "User Two")
        val token3 = registerAndGetToken(client, "user3@example.com", "User Three")

        // User 1 and User 2 follow User 3
        client.post("/api/v1/social/follow/3") {
            header("Authorization", "Bearer $token1")
        }
        client.post("/api/v1/social/follow/3") {
            header("Authorization", "Bearer $token2")
        }

        // Get User 3's followers
        val response = client.get("/api/v1/social/followers") {
            header("Authorization", "Bearer $token3")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val followers = response.body<PaginatedResponse<FollowerInfo>>()
        assertEquals(2, followers.data.size)
    }

    @Test
    fun testGetFollowing() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register three users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        registerAndGetToken(client, "user2@example.com", "User Two")
        registerAndGetToken(client, "user3@example.com", "User Three")

        // User 1 follows User 2 and User 3
        client.post("/api/v1/social/follow/2") {
            header("Authorization", "Bearer $token1")
        }
        client.post("/api/v1/social/follow/3") {
            header("Authorization", "Bearer $token1")
        }

        // Get User 1's following
        val response = client.get("/api/v1/social/following") {
            header("Authorization", "Bearer $token1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val following = response.body<PaginatedResponse<FollowerInfo>>()
        assertEquals(2, following.data.size)
    }

    @Test
    fun testCreateActivity() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        val activityRequest = CreateActivityRequest(
            activityType = ActivityType.WORKOUT_COMPLETED,
            content = "Just finished an amazing workout!",
            metadata = ActivityMetadata(
                workoutType = "Cardio",
                workoutDuration = 45
            ),
            visibility = ActivityVisibility.PUBLIC
        )

        val response = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(activityRequest)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val activity = response.body<Activity>()
        assertNotNull(activity.id)
        assertEquals("Just finished an amazing workout!", activity.content)
        assertEquals(ActivityType.WORKOUT_COMPLETED, activity.activityType)
    }

    @Test
    fun testGetFeed() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        val token2 = registerAndGetToken(client, "user2@example.com", "User Two")

        // User 1 follows User 2
        client.post("/api/v1/social/follow/2") {
            header("Authorization", "Bearer $token1")
        }

        // User 2 creates an activity
        client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token2")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout today!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }

        // User 1 gets feed
        val response = client.get("/api/v1/social/feed") {
            header("Authorization", "Bearer $token1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val feed = response.body<PaginatedResponse<Activity>>()
        assertTrue(feed.data.isNotEmpty())
    }

    @Test
    fun testLikeActivity() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Create an activity
        val activityResponse = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }
        val activity = activityResponse.body<Activity>()

        // Like the activity
        val likeResponse = client.post("/api/v1/social/activity/${activity.id}/like") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.Created, likeResponse.status)
        val like = likeResponse.body<ActivityLike>()
        assertNotNull(like.id)
        assertEquals(activity.id, like.activityId)
    }

    @Test
    fun testUnlikeActivity() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Create an activity
        val activityResponse = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }
        val activity = activityResponse.body<Activity>()

        // Like the activity
        client.post("/api/v1/social/activity/${activity.id}/like") {
            header("Authorization", "Bearer $token")
        }

        // Unlike the activity
        val unlikeResponse = client.delete("/api/v1/social/activity/${activity.id}/like") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.NoContent, unlikeResponse.status)
    }

    @Test
    fun testAddComment() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Create an activity
        val activityResponse = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }
        val activity = activityResponse.body<Activity>()

        // Add a comment
        val commentResponse = client.post("/api/v1/social/activity/${activity.id}/comment") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateCommentRequest("Nice work!"))
        }

        assertEquals(HttpStatusCode.Created, commentResponse.status)
        val comment = commentResponse.body<ActivityComment>()
        assertNotNull(comment.id)
        assertEquals("Nice work!", comment.content)
    }

    @Test
    fun testGetComments() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Create an activity
        val activityResponse = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }
        val activity = activityResponse.body<Activity>()

        // Add a comment
        client.post("/api/v1/social/activity/${activity.id}/comment") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateCommentRequest("Nice work!"))
        }

        // Get comments
        val commentsResponse = client.get("/api/v1/social/activity/${activity.id}/comments") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, commentsResponse.status)
        val comments = commentsResponse.body<PaginatedResponse<ActivityComment>>()
        assertEquals(1, comments.data.size)
    }

    @Test
    fun testDeleteActivity() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val token = registerAndGetToken(client, "user@example.com", "Test User")

        // Create an activity
        val activityResponse = client.post("/api/v1/social/activity") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(CreateActivityRequest(
                activityType = ActivityType.WORKOUT_COMPLETED,
                content = "Great workout!",
                visibility = ActivityVisibility.PUBLIC
            ))
        }
        val activity = activityResponse.body<Activity>()

        // Delete the activity
        val deleteResponse = client.delete("/api/v1/social/activity/${activity.id}") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun testGetUserProfileWithSocial() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        // Register two users
        val token1 = registerAndGetToken(client, "user1@example.com", "User One")
        registerAndGetToken(client, "user2@example.com", "User Two")

        // User 1 follows User 2
        client.post("/api/v1/social/follow/2") {
            header("Authorization", "Bearer $token1")
        }

        // User 1 views User 2's profile
        val response = client.get("/api/v1/social/users/2/profile") {
            header("Authorization", "Bearer $token1")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<UserProfileWithSocial>()
        assertEquals("User Two", profile.name)
        assertTrue(profile.isFollowing)
    }
}
