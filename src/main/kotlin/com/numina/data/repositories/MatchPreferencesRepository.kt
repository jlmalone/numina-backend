package com.numina.data.repositories

import com.numina.data.tables.MatchPreferences
import com.numina.domain.MatchPreferences as MatchPreferencesDomain
import com.numina.domain.UpdateMatchPreferencesRequest
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

interface MatchPreferencesRepository {
    suspend fun getPreferences(userId: Int): MatchPreferencesDomain
    suspend fun updatePreferences(userId: Int, request: UpdateMatchPreferencesRequest): MatchPreferencesDomain
}

class MatchPreferencesRepositoryImpl : MatchPreferencesRepository {
    private fun resultRowToPreferences(row: ResultRow): MatchPreferencesDomain {
        return MatchPreferencesDomain(
            userId = row[MatchPreferences.userId],
            maxDistanceKm = row[MatchPreferences.maxDistanceKm],
            minFitnessLevel = row[MatchPreferences.minFitnessLevel],
            maxFitnessLevel = row[MatchPreferences.maxFitnessLevel],
            preferredAgeMin = row[MatchPreferences.preferredAgeMin],
            preferredAgeMax = row[MatchPreferences.preferredAgeMax]
        )
    }

    override suspend fun getPreferences(userId: Int): MatchPreferencesDomain = transaction {
        MatchPreferences.select { MatchPreferences.userId eq userId }
            .map { resultRowToPreferences(it) }
            .singleOrNull()
            ?: run {
                // Create default preferences if they don't exist
                MatchPreferences.insert {
                    it[MatchPreferences.userId] = userId
                    it[maxDistanceKm] = 10.0f
                }
                MatchPreferences.select { MatchPreferences.userId eq userId }
                    .map { resultRowToPreferences(it) }
                    .single()
            }
    }

    override suspend fun updatePreferences(userId: Int, request: UpdateMatchPreferencesRequest): MatchPreferencesDomain = transaction {
        // Ensure preferences exist
        val existing = MatchPreferences.select { MatchPreferences.userId eq userId }.count()
        if (existing == 0L) {
            MatchPreferences.insert {
                it[MatchPreferences.userId] = userId
                it[maxDistanceKm] = 10.0f
            }
        }

        // Update preferences
        MatchPreferences.update({ MatchPreferences.userId eq userId }) {
            request.maxDistanceKm?.let { dist -> it[maxDistanceKm] = dist }
            request.minFitnessLevel?.let { min -> it[minFitnessLevel] = min }
            request.maxFitnessLevel?.let { max -> it[maxFitnessLevel] = max }
            request.preferredAgeMin?.let { min -> it[preferredAgeMin] = min }
            request.preferredAgeMax?.let { max -> it[preferredAgeMax] = max }
        }

        MatchPreferences.select { MatchPreferences.userId eq userId }
            .map { resultRowToPreferences(it) }
            .single()
    }
}
