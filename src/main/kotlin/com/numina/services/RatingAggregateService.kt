package com.numina.services

import com.numina.data.repositories.RatingAggregateRepository
import org.slf4j.LoggerFactory

interface RatingAggregateService {
    suspend fun updateAllAggregates()
    suspend fun updateAggregateForEntity(entityType: String, entityId: String)
}

class RatingAggregateServiceImpl(
    private val ratingAggregateRepository: RatingAggregateRepository
) : RatingAggregateService {

    private val logger = LoggerFactory.getLogger(RatingAggregateServiceImpl::class.java)

    override suspend fun updateAllAggregates() {
        logger.info("Updating all rating aggregates...")
        // In a production system, this would:
        // 1. Query all entities that have new reviews
        // 2. Recalculate aggregates for each
        // 3. Update the rating_aggregates table
        // For now, this is a placeholder for the background job
        logger.info("Rating aggregates update completed")
    }

    override suspend fun updateAggregateForEntity(entityType: String, entityId: String) {
        logger.info("Updating rating aggregate for $entityType:$entityId")
        // Calculate and update aggregate for a specific entity
        ratingAggregateRepository.updateAggregate(entityType, entityId)
    }
}
