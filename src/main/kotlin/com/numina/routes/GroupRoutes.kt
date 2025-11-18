package com.numina.routes

import com.numina.domain.groups.*
import com.numina.services.groups.GroupService
import com.numina.services.groups.GroupActivityService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.groupRoutes() {
    val groupService by inject<GroupService>()
    val activityService by inject<GroupActivityService>()

    route("/groups") {
        // Public endpoints
        get {
            val filters = GroupFilters(
                category = call.request.queryParameters["category"],
                isPrivate = call.request.queryParameters["isPrivate"]?.toBoolean(),
                location = call.request.queryParameters["location"],
                latitude = call.request.queryParameters["lat"]?.toDoubleOrNull(),
                longitude = call.request.queryParameters["long"]?.toDoubleOrNull(),
                radiusKm = call.request.queryParameters["radius"]?.toDoubleOrNull(),
                minMembers = call.request.queryParameters["minMembers"]?.toIntOrNull(),
                maxMembers = call.request.queryParameters["maxMembers"]?.toIntOrNull(),
                search = call.request.queryParameters["search"]
            )

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val paginatedGroups = groupService.getGroups(filters, page, pageSize)
            call.respond(HttpStatusCode.OK, paginatedGroups)
        }

        get("/{id}") {
            val groupId = call.parameters["id"]
                ?: throw IllegalArgumentException("Invalid group ID")

            val group = groupService.getGroupById(groupId)
            call.respond(HttpStatusCode.OK, group)
        }

        get("/discover") {
            val filters = GroupFilters(
                category = call.request.queryParameters["category"],
                location = call.request.queryParameters["location"],
                latitude = call.request.queryParameters["lat"]?.toDoubleOrNull(),
                longitude = call.request.queryParameters["long"]?.toDoubleOrNull(),
                radiusKm = call.request.queryParameters["radius"]?.toDoubleOrNull(),
                minMembers = call.request.queryParameters["minMembers"]?.toIntOrNull(),
                maxMembers = call.request.queryParameters["maxMembers"]?.toIntOrNull(),
                search = call.request.queryParameters["search"]
            )

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val paginatedGroups = groupService.discoverGroups(filters, page, pageSize)
            call.respond(HttpStatusCode.OK, paginatedGroups)
        }

        // Authenticated endpoints
        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<CreateGroupRequest>()

                val group = groupService.createGroup(userId, request)
                call.respond(HttpStatusCode.Created, group)
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val request = call.receive<UpdateGroupRequest>()

                val updatedGroup = groupService.updateGroup(groupId, userId, request)
                call.respond(HttpStatusCode.OK, updatedGroup)
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")

                groupService.deleteGroup(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Group deleted successfully"))
            }

            get("/my-groups") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()

                val groups = groupService.getUserGroups(userId)
                call.respond(HttpStatusCode.OK, groups)
            }

            get("/{id}/members") {
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")

                val members = groupService.getGroupMembers(groupId)
                call.respond(HttpStatusCode.OK, members)
            }

            post("/{id}/join") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")

                groupService.joinGroup(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Join request submitted successfully"))
            }

            post("/{id}/leave") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")

                groupService.leaveGroup(groupId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Left group successfully"))
            }

            post("/{id}/invite") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val request = call.receive<InviteUserRequest>()

                val response = groupService.inviteToGroup(groupId, userId, request)
                if (response != null) {
                    call.respond(HttpStatusCode.Created, response)
                } else {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Invitation sent successfully"))
                }
            }

            delete("/{id}/kick/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val adminId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val memberId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                groupService.kickMember(groupId, adminId, memberId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Member removed successfully"))
            }

            // Membership requests endpoints
            get("/{id}/requests") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")

                val requests = groupService.getPendingRequests(groupId, userId)
                call.respond(HttpStatusCode.OK, requests)
            }

            post("/{id}/requests/approve/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val adminId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val memberId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                groupService.approveJoinRequest(groupId, adminId, memberId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Join request approved"))
            }

            post("/{id}/requests/reject/{userId}") {
                val principal = call.principal<JWTPrincipal>()
                val adminId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val memberId = call.parameters["userId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid user ID")

                groupService.rejectJoinRequest(groupId, adminId, memberId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Join request rejected"))
            }

            // Group activities endpoints
            post("/{id}/activities") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val request = call.receive<CreateActivityRequest>()

                val activity = activityService.createActivity(groupId, userId, request)
                call.respond(HttpStatusCode.Created, activity)
            }

            get("/{id}/activities") {
                val groupId = call.parameters["id"]
                    ?: throw IllegalArgumentException("Invalid group ID")
                val upcomingOnly = call.request.queryParameters["upcoming"]?.toBoolean() ?: false

                val activities = activityService.getGroupActivities(groupId, upcomingOnly)
                call.respond(HttpStatusCode.OK, activities)
            }

            get("/{id}/activities/{activityId}") {
                val activityId = call.parameters["activityId"]
                    ?: throw IllegalArgumentException("Invalid activity ID")

                val activity = activityService.getActivityById(activityId)
                call.respond(HttpStatusCode.OK, activity)
            }

            put("/{id}/activities/{activityId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["activityId"]
                    ?: throw IllegalArgumentException("Invalid activity ID")
                val request = call.receive<UpdateActivityRequest>()

                val updatedActivity = activityService.updateActivity(activityId, userId, request)
                call.respond(HttpStatusCode.OK, updatedActivity)
            }

            delete("/{id}/activities/{activityId}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["activityId"]
                    ?: throw IllegalArgumentException("Invalid activity ID")

                activityService.cancelActivity(activityId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Activity cancelled successfully"))
            }

            post("/{id}/activities/{activityId}/rsvp") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val activityId = call.parameters["activityId"]
                    ?: throw IllegalArgumentException("Invalid activity ID")
                val request = call.receive<RSVPRequest>()

                activityService.rsvpToActivity(activityId, userId, request.status)
                call.respond(HttpStatusCode.OK, mapOf("message" to "RSVP recorded successfully"))
            }

            get("/{id}/activities/{activityId}/rsvps") {
                val activityId = call.parameters["activityId"]
                    ?: throw IllegalArgumentException("Invalid activity ID")

                val rsvps = activityService.getActivityRSVPs(activityId)
                call.respond(HttpStatusCode.OK, rsvps)
            }
        }
    }
}
