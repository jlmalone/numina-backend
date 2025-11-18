package com.numina.services

import com.numina.common.exceptions.NotFoundException
import com.numina.common.exceptions.ValidationException
import com.numina.common.models.PaginatedResponse
import com.numina.common.models.PaginationMeta
import com.numina.common.utils.ValidationUtils
import com.numina.data.repositories.ClassRepository
import com.numina.domain.ClassFilters
import com.numina.domain.CreateClassRequest
import com.numina.domain.FitnessClass
import com.numina.domain.UpdateClassRequest
import org.slf4j.LoggerFactory

interface ClassService {
    suspend fun createClass(request: CreateClassRequest, createdBy: Int): FitnessClass
    suspend fun getClassById(id: Int): FitnessClass
    suspend fun getClasses(filters: ClassFilters, page: Int = 1, pageSize: Int = 20): PaginatedResponse<FitnessClass>
    suspend fun updateClass(id: Int, request: UpdateClassRequest, updatedBy: Int): FitnessClass
    suspend fun deleteClass(id: Int, deletedBy: Int): Boolean
}

class ClassServiceImpl(
    private val classRepository: ClassRepository
) : ClassService {
    private val logger = LoggerFactory.getLogger(ClassServiceImpl::class.java)

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val DEFAULT_PAGE_SIZE = 20
    }

    override suspend fun createClass(request: CreateClassRequest, createdBy: Int): FitnessClass {
        logger.info("Creating new class: ${request.name} (createdBy=$createdBy)")

        // Validate input
        ValidationUtils.validateRequired(request.name, "name")
        ValidationUtils.validateRequired(request.description, "description")
        ValidationUtils.validateRange(request.intensity, 1, 10, "intensity")
        ValidationUtils.validatePositive(request.capacity, "capacity")
        ValidationUtils.validatePositive(request.price, "price")
        ValidationUtils.validateLatitude(request.locationLat)
        ValidationUtils.validateLongitude(request.locationLong)

        val fitnessClass = classRepository.createClass(request)
            ?: throw ValidationException(
                message = "Failed to create class",
                errorCode = "CLASS_CREATION_FAILED"
            )

        logger.info("Class created successfully: classId=${fitnessClass.id}")
        return fitnessClass
    }

    override suspend fun getClassById(id: Int): FitnessClass {
        logger.debug("Fetching class by id: $id")

        return classRepository.getClassById(id)
            ?: throw NotFoundException(
                message = "Class not found",
                errorCode = "CLASS_NOT_FOUND",
                details = mapOf("classId" to id.toString())
            )
    }

    override suspend fun getClasses(
        filters: ClassFilters,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<FitnessClass> {
        logger.debug("Fetching classes with filters: $filters (page=$page, pageSize=$pageSize)")

        // Validate pagination params
        if (page < 1) {
            throw ValidationException(
                message = "Validation failed",
                details = mapOf("page" to "Page number must be >= 1")
            )
        }

        val effectivePageSize = when {
            pageSize < 1 -> DEFAULT_PAGE_SIZE
            pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> pageSize
        }

        // Validate location filters
        filters.locationLat?.let { ValidationUtils.validateLatitude(it) }
        filters.locationLong?.let { ValidationUtils.validateLongitude(it) }

        // Validate intensity filters
        filters.minIntensity?.let {
            if (it < 1 || it > 10) {
                throw ValidationException(
                    message = "Validation failed",
                    details = mapOf("minIntensity" to "Must be between 1 and 10")
                )
            }
        }
        filters.maxIntensity?.let {
            if (it < 1 || it > 10) {
                throw ValidationException(
                    message = "Validation failed",
                    details = mapOf("maxIntensity" to "Must be between 1 and 10")
                )
            }
        }

        // Get all matching classes (for now - would optimize with database pagination in production)
        val allClasses = classRepository.getClasses(filters)

        // Apply pagination
        val totalItems = allClasses.size.toLong()
        val skip = (page - 1) * effectivePageSize
        val paginatedClasses = allClasses.drop(skip).take(effectivePageSize)

        val paginationMeta = PaginationMeta.from(page, effectivePageSize, totalItems)

        logger.debug("Returning ${paginatedClasses.size} classes out of $totalItems total")

        return PaginatedResponse(
            items = paginatedClasses,
            pagination = paginationMeta
        )
    }

    override suspend fun updateClass(id: Int, request: UpdateClassRequest, updatedBy: Int): FitnessClass {
        logger.info("Updating class: id=$id (updatedBy=$updatedBy)")

        // Validate optional fields
        request.intensity?.let { ValidationUtils.validateRange(it, 1, 10, "intensity") }
        request.capacity?.let { ValidationUtils.validatePositive(it, "capacity") }
        request.price?.let { ValidationUtils.validatePositive(it, "price") }
        request.locationLat?.let { ValidationUtils.validateLatitude(it) }
        request.locationLong?.let { ValidationUtils.validateLongitude(it) }

        val updatedClass = classRepository.updateClass(id, request)
            ?: throw NotFoundException(
                message = "Class not found",
                errorCode = "CLASS_NOT_FOUND",
                details = mapOf("classId" to id.toString())
            )

        logger.info("Class updated successfully: classId=$id")
        return updatedClass
    }

    override suspend fun deleteClass(id: Int, deletedBy: Int): Boolean {
        logger.info("Deleting class: id=$id (deletedBy=$deletedBy)")

        val deleted = classRepository.deleteClass(id)
        if (!deleted) {
            throw NotFoundException(
                message = "Class not found",
                errorCode = "CLASS_NOT_FOUND",
                details = mapOf("classId" to id.toString())
            )
        }

        logger.info("Class deleted successfully: classId=$id")
        return true
    }
}
