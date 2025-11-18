package com.numina.data.repositories

import com.numina.data.tables.FeatureFlags
import com.numina.domain.FeatureFlag
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface FeatureFlagRepository {
    suspend fun createFlag(name: String, enabled: Boolean, description: String?, rolloutPercentage: Int): FeatureFlag
    suspend fun getFlagByName(name: String): FeatureFlag?
    suspend fun getFlagById(id: UUID): FeatureFlag?
    suspend fun getAllFlags(): List<FeatureFlag>
    suspend fun updateFlag(id: UUID, enabled: Boolean?, description: String?, rolloutPercentage: Int?): FeatureFlag?
    suspend fun deleteFlag(id: UUID): Boolean
}

class FeatureFlagRepositoryImpl : FeatureFlagRepository {
    override suspend fun createFlag(name: String, enabled: Boolean, description: String?, rolloutPercentage: Int): FeatureFlag = transaction {
        val now = kotlinx.datetime.Clock.System.now().toJavaInstant()
        val id = FeatureFlags.insertAndGetId {
            it[FeatureFlags.name] = name
            it[FeatureFlags.enabled] = enabled
            it[FeatureFlags.description] = description
            it[FeatureFlags.rolloutPercentage] = rolloutPercentage
            it[createdAt] = now
            it[updatedAt] = now
        }

        FeatureFlags.select { FeatureFlags.id eq id }.map { rowToFeatureFlag(it) }.single()
    }

    override suspend fun getFlagByName(name: String): FeatureFlag? = transaction {
        FeatureFlags.select { FeatureFlags.name eq name }
            .map { rowToFeatureFlag(it) }
            .singleOrNull()
    }

    override suspend fun getFlagById(id: UUID): FeatureFlag? = transaction {
        FeatureFlags.select { FeatureFlags.id eq id }
            .map { rowToFeatureFlag(it) }
            .singleOrNull()
    }

    override suspend fun getAllFlags(): List<FeatureFlag> = transaction {
        FeatureFlags.selectAll().map { rowToFeatureFlag(it) }
    }

    override suspend fun updateFlag(id: UUID, enabled: Boolean?, description: String?, rolloutPercentage: Int?): FeatureFlag? = transaction {
        FeatureFlags.update({ FeatureFlags.id eq id }) {
            enabled?.let { value -> it[FeatureFlags.enabled] = value }
            description?.let { value -> it[FeatureFlags.description] = value }
            rolloutPercentage?.let { value -> it[FeatureFlags.rolloutPercentage] = value }
            it[updatedAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
        }
        getFlagById(id)
    }

    override suspend fun deleteFlag(id: UUID): Boolean = transaction {
        FeatureFlags.deleteWhere { FeatureFlags.id eq id } > 0
    }

    private fun rowToFeatureFlag(row: ResultRow): FeatureFlag {
        return FeatureFlag(
            id = row[FeatureFlags.id].value.toString(),
            name = row[FeatureFlags.name],
            enabled = row[FeatureFlags.enabled],
            description = row[FeatureFlags.description],
            rolloutPercentage = row[FeatureFlags.rolloutPercentage],
            createdAt = row[FeatureFlags.createdAt].toKotlinInstant(),
            updatedAt = row[FeatureFlags.updatedAt].toKotlinInstant()
        )
    }
}
