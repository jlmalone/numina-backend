package com.numina.data.repositories

import com.numina.data.tables.RefreshTokens
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.time.Duration.Companion.days

interface RefreshTokenRepository {
    suspend fun createRefreshToken(userId: Int): String
    suspend fun validateRefreshToken(token: String): Int?
    suspend fun revokeRefreshToken(token: String): Boolean
    suspend fun revokeAllUserTokens(userId: Int): Boolean
}

class RefreshTokenRepositoryImpl : RefreshTokenRepository {
    override suspend fun createRefreshToken(userId: Int): String = transaction {
        val token = UUID.randomUUID().toString()
        val now = Clock.System.now()
        val expiresAt = now.plus(30.days)

        RefreshTokens.insert {
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.token] = token
            it[RefreshTokens.expiresAt] = expiresAt
            it[createdAt] = now
            it[isRevoked] = false
        }

        token
    }

    override suspend fun validateRefreshToken(token: String): Int? = transaction {
        val now = Clock.System.now()
        val row = RefreshTokens.select {
            (RefreshTokens.token eq token) and
            (RefreshTokens.isRevoked eq false) and
            (RefreshTokens.expiresAt greater now)
        }.singleOrNull()

        row?.get(RefreshTokens.userId)
    }

    override suspend fun revokeRefreshToken(token: String): Boolean = transaction {
        RefreshTokens.update({ RefreshTokens.token eq token }) {
            it[isRevoked] = true
        } > 0
    }

    override suspend fun revokeAllUserTokens(userId: Int): Boolean = transaction {
        RefreshTokens.update({ RefreshTokens.userId eq userId }) {
            it[isRevoked] = true
        } > 0
    }
}
