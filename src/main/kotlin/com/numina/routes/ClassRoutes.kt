package com.numina.routes

import com.numina.data.repositories.ClassRepository
import com.numina.domain.ClassFilters
import com.numina.domain.CreateClassRequest
import com.numina.domain.UpdateClassRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

fun Route.classRoutes() {
    val classRepository by inject<ClassRepository>()

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

            val classes = classRepository.getClasses(filters)
            call.respond(classes)
        }

        get("/{id}") {
            val classId = call.parameters["id"]?.toIntOrNull()
            if (classId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid class ID"))
                return@get
            }

            val fitnessClass = classRepository.getClassById(classId)
            if (fitnessClass == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Class not found"))
                return@get
            }

            call.respond(fitnessClass)
        }

        // Admin endpoints - require authentication
        authenticate("auth-jwt") {
            post {
                val request = call.receive<CreateClassRequest>()

                // Validate intensity
                if (request.intensity < 1 || request.intensity > 10) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Intensity must be between 1 and 10"))
                    return@post
                }

                // Validate capacity
                if (request.capacity < 1) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Capacity must be at least 1"))
                    return@post
                }

                val fitnessClass = classRepository.createClass(request)
                if (fitnessClass == null) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create class"))
                    return@post
                }

                call.respond(HttpStatusCode.Created, fitnessClass)
            }

            put("/{id}") {
                val classId = call.parameters["id"]?.toIntOrNull()
                if (classId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid class ID"))
                    return@put
                }

                val request = call.receive<UpdateClassRequest>()

                // Validate intensity if provided
                if (request.intensity != null && (request.intensity < 1 || request.intensity > 10)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Intensity must be between 1 and 10"))
                    return@put
                }

                // Validate capacity if provided
                if (request.capacity != null && request.capacity < 1) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Capacity must be at least 1"))
                    return@put
                }

                val updatedClass = classRepository.updateClass(classId, request)
                if (updatedClass == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Class not found"))
                    return@put
                }

                call.respond(updatedClass)
            }
        }
    }
}
