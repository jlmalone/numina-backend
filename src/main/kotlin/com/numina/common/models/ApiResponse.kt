package com.numina.common.models

import kotlinx.serialization.Serializable

/**
 * Standard API response wrapper for consistent response format
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val meta: MetaData? = null
) {
    companion object {
        fun <T> success(data: T, meta: MetaData? = null) = ApiResponse(
            success = true,
            data = data,
            meta = meta
        )

        fun error(
            message: String,
            errorCode: String,
            details: Map<String, Any>? = null
        ) = ApiResponse<Nothing>(
            success = false,
            error = ErrorResponse(message, errorCode, details)
        )
    }
}

/**
 * Error response details
 */
@Serializable
data class ErrorResponse(
    val message: String,
    val errorCode: String,
    val details: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Response metadata (pagination, timestamps, etc.)
 */
@Serializable
data class MetaData(
    val pagination: PaginationMeta? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pagination metadata
 */
@Serializable
data class PaginationMeta(
    val page: Int,
    val pageSize: Int,
    val totalItems: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun from(page: Int, pageSize: Int, totalItems: Long): PaginationMeta {
            val totalPages = ((totalItems + pageSize - 1) / pageSize).toInt()
            return PaginationMeta(
                page = page,
                pageSize = pageSize,
                totalItems = totalItems,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrevious = page > 1
            )
        }
    }
}

/**
 * Paginated list response
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: PaginationMeta
)
