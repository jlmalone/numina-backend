package com.numina.routes

import com.numina.domain.ClassFilters
import com.numina.domain.CreateClassRequest
import com.numina.domain.UpdateClassRequest
import com.numina.services.ClassService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.classRoutes() {
    val classService by inject<ClassService>()

    route("/classes") {
        // Public endpoints - anyone can view classes
        get {
            val filters = ClassFilters(
                locationLat = call.request.queryParameters["lat"]?.toDoubleOrNull(),
                locationLong = call.request.queryParameters["long"]?.toDoubleOrNull(),
                radiusKm = call.request.queryParameters["radius"]?.toDoubleOrNull(),
                startDate = call.request.queryParameters["startDate"]?.let { Instant.parse(it) },
                endDate = call.request.queryParameters["endDate"]?.let { Instant.parse(it) },
                type = call.request.queryParameters["type"],
                minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull(),
                maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull(),
                minIntensity = call.request.queryParameters["minIntensity"]?.toIntOrNull(),
                maxIntensity = call.request.queryParameters["maxIntensity"]?.toIntOrNull(),
                tags = call.request.queryParameters.getAll("tags")
            )

            // Pagination parameters
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val paginatedClasses = classService.getClasses(filters, page, pageSize)
            call.respond(HttpStatusCode.OK, paginatedClasses)
        }

        get("/{id}") {
            val classId = call.parameters["id"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid class ID")

            val fitnessClass = classService.getClassById(classId)
            call.respond(HttpStatusCode.OK, fitnessClass)
        }

        // Admin endpoints - require authentication
        authenticate("auth-jwt") {
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val request = call.receive<CreateClassRequest>()

                val fitnessClass = classService.createClass(request, userId)
                call.respond(HttpStatusCode.Created, fitnessClass)
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val classId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid class ID")
                val request = call.receive<UpdateClassRequest>()

                val updatedClass = classService.updateClass(classId, request, userId)
                call.respond(HttpStatusCode.OK, updatedClass)
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("userId").asInt()
                val classId = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid class ID")

                classService.deleteClass(classId, userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Class deleted successfully"))
            }
        }
    }
}
