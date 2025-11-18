package com.numina.common.exceptions

import io.ktor.http.*

/**
 * Base exception for all API errors with HTTP status code and error details
 */
sealed class ApiException(
    val statusCode: HttpStatusCode,
    override val message: String,
    val errorCode: String,
    val details: Map<String, Any>? = null
) : Exception(message)

/**
 * 400 Bad Request - Client sent invalid data
 */
class BadRequestException(
    message: String,
    errorCode: String = "BAD_REQUEST",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.BadRequest, message, errorCode, details)

/**
 * 401 Unauthorized - Authentication required or failed
 */
class UnauthorizedException(
    message: String = "Authentication required",
    errorCode: String = "UNAUTHORIZED",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.Unauthorized, message, errorCode, details)

/**
 * 403 Forbidden - User doesn't have permission
 */
class ForbiddenException(
    message: String = "You don't have permission to access this resource",
    errorCode: String = "FORBIDDEN",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.Forbidden, message, errorCode, details)

/**
 * 404 Not Found - Resource doesn't exist
 */
class NotFoundException(
    message: String,
    errorCode: String = "NOT_FOUND",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.NotFound, message, errorCode, details)

/**
 * 409 Conflict - Resource already exists or conflict with current state
 */
class ConflictException(
    message: String,
    errorCode: String = "CONFLICT",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.Conflict, message, errorCode, details)

/**
 * 422 Unprocessable Entity - Validation failed
 */
class ValidationException(
    message: String = "Validation failed",
    errorCode: String = "VALIDATION_ERROR",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.UnprocessableEntity, message, errorCode, details)

/**
 * 500 Internal Server Error - Unexpected server error
 */
class InternalServerException(
    message: String = "An unexpected error occurred",
    errorCode: String = "INTERNAL_ERROR",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.InternalServerError, message, errorCode, details)

/**
 * 503 Service Unavailable - External service or database unavailable
 */
class ServiceUnavailableException(
    message: String = "Service temporarily unavailable",
    errorCode: String = "SERVICE_UNAVAILABLE",
    details: Map<String, Any>? = null
) : ApiException(HttpStatusCode.ServiceUnavailable, message, errorCode, details)
