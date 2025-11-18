package com.numina.common.utils

import com.numina.common.exceptions.ValidationException

object ValidationUtils {
    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateEmail(email: String, fieldName: String = "email") {
        if (email.isBlank()) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Email is required")
            )
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Invalid email format")
            )
        }
    }

    fun validatePassword(password: String, fieldName: String = "password") {
        if (password.isBlank()) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Password is required")
            )
        }
        if (password.length < 8) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Password must be at least 8 characters long")
            )
        }
        if (!password.any { it.isDigit() }) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Password must contain at least one digit")
            )
        }
        if (!password.any { it.isLetter() }) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "Password must contain at least one letter")
            )
        }
    }

    fun validateRequired(value: String?, fieldName: String) {
        if (value.isNullOrBlank()) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "$fieldName is required")
            )
        }
    }

    fun validateRange(value: Int, min: Int, max: Int, fieldName: String) {
        if (value < min || value > max) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "$fieldName must be between $min and $max")
            )
        }
    }

    fun validatePositive(value: Double, fieldName: String) {
        if (value <= 0) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "$fieldName must be positive")
            )
        }
    }

    fun validatePositive(value: Int, fieldName: String) {
        if (value <= 0) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf(fieldName to "$fieldName must be positive")
            )
        }
    }

    fun validateLatitude(lat: Double) {
        if (lat < -90 || lat > 90) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("latitude" to "Latitude must be between -90 and 90")
            )
        }
    }

    fun validateLongitude(long: Double) {
        if (long < -180 || long > 180) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("longitude" to "Longitude must be between -180 and 180")
            )
        }
    }
}
