package com.numina.routes

import com.numina.common.models.ApiResponse
import com.numina.domain.CreateReviewRequest
import com.numina.domain.CreateReviewReportRequest
import com.numina.domain.UpdateReviewRequest
import com.numina.services.RatingAggregationService
import com.numina.services.ReviewService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.reviewRoutes() {
    val reviewService by inject<ReviewService>()
    val ratingAggregationService by inject<RatingAggregationService>()

    route("/reviews") {
        // Create class review
        authenticate {
            post("/classes/{classId}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val classId = call.parameters["classId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid class ID")

                val request = call.receive<CreateReviewRequest>()
                val requestWithClassId = request.copy(classId = classId)

                val review = reviewService.createReview(userId, requestWithClassId)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse.success(review, "Review created successfully")
                )
            }
        }

        // Get class reviews
        get("/classes/{classId}") {
            val classId = call.parameters["classId"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid class ID")

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val reviews = reviewService.getReviewsByClassId(classId, page, pageSize)
            call.respond(HttpStatusCode.OK, ApiResponse.success(reviews))
        }

        // Create trainer review
        authenticate {
            post("/trainers/{trainerId}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val trainerId = call.parameters["trainerId"]
                    ?: throw IllegalArgumentException("Trainer ID is required")

                val request = call.receive<CreateReviewRequest>()
                val requestWithTrainerId = request.copy(trainerId = trainerId)

                val review = reviewService.createReview(userId, requestWithTrainerId)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse.success(review, "Review created successfully")
                )
            }
        }

        // Get trainer reviews
        get("/trainers/{trainerId}") {
            val trainerId = call.parameters["trainerId"]
                ?: throw IllegalArgumentException("Trainer ID is required")

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val reviews = reviewService.getReviewsByTrainerId(trainerId, page, pageSize)
            call.respond(HttpStatusCode.OK, ApiResponse.success(reviews))
        }

        // Get provider reviews
        get("/providers/{providerId}") {
            val providerId = call.parameters["providerId"]
                ?: throw IllegalArgumentException("Provider ID is required")

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val reviews = reviewService.getReviewsByProviderId(providerId, page, pageSize)
            call.respond(HttpStatusCode.OK, ApiResponse.success(reviews))
        }

        // Get my reviews
        authenticate {
            get("/my-reviews") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val reviews = reviewService.getMyReviews(userId)
                call.respond(HttpStatusCode.OK, ApiResponse.success(reviews))
            }
        }

        // Update review
        authenticate {
            put("/{reviewId}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val reviewId = call.parameters["reviewId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid review ID")

                val request = call.receive<UpdateReviewRequest>()
                val review = reviewService.updateReview(reviewId, userId, request)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(review, "Review updated successfully")
                )
            }
        }

        // Delete review
        authenticate {
            delete("/{reviewId}") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val reviewId = call.parameters["reviewId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid review ID")

                reviewService.deleteReview(reviewId, userId)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(mapOf("deleted" to true), "Review deleted successfully")
                )
            }
        }

        // Mark review as helpful
        authenticate {
            post("/{reviewId}/helpful") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val reviewId = call.parameters["reviewId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid review ID")

                val success = reviewService.voteHelpful(reviewId, userId)
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse.success(mapOf("voted" to success), "Vote recorded")
                )
            }
        }

        // Report review
        authenticate {
            post("/{reviewId}/report") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asInt()
                    ?: throw IllegalStateException("User ID not found in token")

                val reviewId = call.parameters["reviewId"]?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid review ID")

                val request = call.receive<CreateReviewReportRequest>()
                val report = reviewService.reportReview(reviewId, userId, request)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse.success(report, "Review reported successfully")
                )
            }
        }
    }

    route("/ratings") {
        // Get class rating summary
        get("/classes/{classId}") {
            val classId = call.parameters["classId"]?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid class ID")

            val summary = ratingAggregationService.getRatingSummaryForClass(classId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(summary))
        }

        // Get trainer rating summary
        get("/trainers/{trainerId}") {
            val trainerId = call.parameters["trainerId"]
                ?: throw IllegalArgumentException("Trainer ID is required")

            val summary = ratingAggregationService.getRatingSummaryForTrainer(trainerId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(summary))
        }

        // Get provider rating summary
        get("/providers/{providerId}") {
            val providerId = call.parameters["providerId"]
                ?: throw IllegalArgumentException("Provider ID is required")

            val summary = ratingAggregationService.getRatingSummaryForProvider(providerId)
            call.respond(HttpStatusCode.OK, ApiResponse.success(summary))
        }
    }
}
