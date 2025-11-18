package com.numina.routes

import com.numina.domain.CreateActivityRequest
import com.numina.domain.CreateCommentRequest
import com.numina.domain.DiscoverUsersRequest
import com.numina.services.ActivityFeedService
import com.numina.services.SocialService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.socialRoutes() {
    val socialService by inject<SocialService>()
    val activityFeedService by inject<ActivityFeedService>()

    authenticate("auth-jwt") {
        route("/social") {
            // ========== Following Endpoints ==========

            post("/follow/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val followerId = principal!!.payload.getClaim("userId").asInt()
                val followingId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                val relationship = socialService.followUser(followerId, followingId)
                call.respond(HttpStatusCode.Created, relationship)
            }

            delete("/unfollow/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val followerId = principal!!.payload.getClaim("userId").asInt()
                val followingId = call.parameters["userId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                socialService.unfollowUser(followerId, followingId)
                call.respond(HttpStatusCode.NoContent)
            }

            get("/following") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val following = socialService.getFollowing(userId, limit, offset)
                call.respond(HttpStatusCode.OK, following)
            }

            get("/followers") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val followers = socialService.getFollowers(userId, limit, offset)
                call.respond(HttpStatusCode.OK, followers)
            }

            get("/suggestions") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

                val suggestions = socialService.getFollowSuggestions(userId, limit)
                call.respond(HttpStatusCode.OK, suggestions)
            }

            // ========== Activity Feed Endpoints ==========

            get("/feed") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val feed = activityFeedService.getFeed(userId, limit, offset)
                call.respond(HttpStatusCode.OK, feed)
            }

            post("/activity") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<CreateActivityRequest>()

                val activity = activityFeedService.createActivity(userId, request)
                call.respond(HttpStatusCode.Created, activity)
            }

            get("/activity/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))

                val activity = activityFeedService.getActivity(activityId, currentUserId)
                call.respond(HttpStatusCode.OK, activity)
            }

            delete("/activity/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))

                activityFeedService.deleteActivity(activityId, userId)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/activity/{id}/like") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))

                val like = activityFeedService.likeActivity(activityId, userId)
                call.respond(HttpStatusCode.Created, like)
            }

            delete("/activity/{id}/like") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))

                activityFeedService.unlikeActivity(activityId, userId)
                call.respond(HttpStatusCode.NoContent)
            }

            post("/activity/{id}/comment") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["id"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))
                val request = call.receive<CreateCommentRequest>()

                val comment = activityFeedService.addComment(activityId, userId, request.content)
                call.respond(HttpStatusCode.Created, comment)
            }

            get("/activity/{id}/comments") {
                val activityId = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid activity ID"))
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val comments = activityFeedService.getComments(activityId, limit, offset)
                call.respond(HttpStatusCode.OK, comments)
            }

            // ========== User Discovery Endpoints ==========

            get("/discover-users") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val query = call.request.queryParameters["query"]
                val locationLat = call.request.queryParameters["locationLat"]?.toDoubleOrNull()
                val locationLong = call.request.queryParameters["locationLong"]?.toDoubleOrNull()
                val radiusKm = call.request.queryParameters["radiusKm"]?.toDoubleOrNull()
                val fitnessInterests = call.request.queryParameters["fitnessInterests"]?.split(",")
                val fitnessLevel = call.request.queryParameters["fitnessLevel"]?.toIntOrNull()
                val minActivityLevel = call.request.queryParameters["minActivityLevel"]?.toIntOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val request = DiscoverUsersRequest(
                    query = query,
                    locationLat = locationLat,
                    locationLong = locationLong,
                    radiusKm = radiusKm,
                    fitnessInterests = fitnessInterests,
                    fitnessLevel = fitnessLevel,
                    minActivityLevel = minActivityLevel,
                    limit = limit,
                    offset = offset
                )

                val users = socialService.discoverUsers(userId, request)
                call.respond(HttpStatusCode.OK, users)
            }

            get("/users/{id}/profile") {
                val principal = call.principal<JWTPrincipal>()
                val requesterId = principal!!.payload.getClaim("userId").asInt()
                val userId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                val profile = socialService.getUserProfileWithSocial(userId, requesterId)
                call.respond(HttpStatusCode.OK, profile)
            }

            get("/users/{id}/activities") {
                val principal = call.principal<JWTPrincipal>()
                val currentUserId = principal!!.payload.getClaim("userId").asInt()
                val userId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

                val activities = activityFeedService.getUserActivities(userId, currentUserId, limit, offset)
                call.respond(HttpStatusCode.OK, activities)
            }

            get("/mutual-connections/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val otherUserId = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user ID"))

                val mutualConnections = socialService.getMutualConnections(userId, otherUserId)
                call.respond(HttpStatusCode.OK, mutualConnections)
            }
        }
    }
}
