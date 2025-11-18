package com.numina.data.repositories

import com.numina.data.tables.RatingAggregates
import com.numina.domain.EntityType
import com.numina.domain.RatingAggregate
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface RatingAggregateRepository {
    suspend fun getAggregate(entityType: EntityType, entityId: String): RatingAggregate?
    suspend fun updateAggregate(
        entityType: EntityType,
        entityId: String,
        averageRating: Double,
        totalReviews: Int,
        ratingDistribution: Map<Int, Int>
    ): RatingAggregate?
    suspend fun deleteAggregate(entityType: EntityType, entityId: String): Boolean
}

class RatingAggregateRepositoryImpl : RatingAggregateRepository {

    override suspend fun getAggregate(entityType: EntityType, entityId: String): RatingAggregate? = transaction {
        RatingAggregates.select {
            (RatingAggregates.entityType eq entityType.name.lowercase()) and
            (RatingAggregates.entityId eq entityId)
        }.firstOrNull()?.let { row ->
            val distributionJson = row[RatingAggregates.ratingDistribution]
            val distribution = Json.decodeFromString<Map<String, Int>>(distributionJson)
                .mapKeys { it.key.toInt() }

            RatingAggregate(
                entityType = EntityType.valueOf(row[RatingAggregates.entityType].uppercase()),
                entityId = row[RatingAggregates.entityId],
                averageRating = row[RatingAggregates.averageRating],
                totalReviews = row[RatingAggregates.totalReviews],
                ratingDistribution = distribution,
                lastUpdated = row[RatingAggregates.lastUpdated]
            )
        }
    }

    override suspend fun updateAggregate(
        entityType: EntityType,
        entityId: String,
        averageRating: Double,
        totalReviews: Int,
        ratingDistribution: Map<Int, Int>
    ): RatingAggregate? = transaction {
        val now = Clock.System.now()
        val distributionJson = Json.encodeToString(
            ratingDistribution.mapKeys { it.key.toString() }
        )

        val existing = RatingAggregates.select {
            (RatingAggregates.entityType eq entityType.name.lowercase()) and
            (RatingAggregates.entityId eq entityId)
        }.count() > 0

        if (existing) {
            RatingAggregates.update({
                (RatingAggregates.entityType eq entityType.name.lowercase()) and
                (RatingAggregates.entityId eq entityId)
            }) {
                it[RatingAggregates.averageRating] = averageRating
                it[RatingAggregates.totalReviews] = totalReviews
                it[RatingAggregates.ratingDistribution] = distributionJson
                it[lastUpdated] = now
            }
        } else {
            RatingAggregates.insert {
                it[RatingAggregates.entityType] = entityType.name.lowercase()
                it[RatingAggregates.entityId] = entityId
                it[RatingAggregates.averageRating] = averageRating
                it[RatingAggregates.totalReviews] = totalReviews
                it[RatingAggregates.ratingDistribution] = distributionJson
                it[lastUpdated] = now
            }
        }

        getAggregate(entityType, entityId)
    }

    override suspend fun deleteAggregate(entityType: EntityType, entityId: String): Boolean = transaction {
        RatingAggregates.deleteWhere {
            (RatingAggregates.entityType eq entityType.name.lowercase()) and
            (RatingAggregates.entityId eq entityId)
        } > 0
    }
}
