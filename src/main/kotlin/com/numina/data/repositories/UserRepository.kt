package com.numina.data.repositories

import com.numina.data.tables.Users
import com.numina.domain.User
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

interface UserRepository {
    suspend fun createUser(email: String, password: String): User?
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun verifyPassword(email: String, password: String): User?
}

class UserRepositoryImpl : UserRepository {
    private fun resultRowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            createdAt = row[Users.createdAt],
            updatedAt = row[Users.updatedAt]
        )
    }

    override suspend fun createUser(email: String, password: String): User? = transaction {
        val existingUser = Users.select { Users.email eq email }.singleOrNull()
        if (existingUser != null) {
            return@transaction null
        }

        val now = Clock.System.now()
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        val userId = Users.insertAndGetId {
            it[Users.email] = email
            it[passwordHash] = hashedPassword
            it[createdAt] = now
            it[updatedAt] = now
        }

        Users.select { Users.id eq userId }.map { resultRowToUser(it) }.singleOrNull()
    }

    override suspend fun getUserById(id: Int): User? = transaction {
        Users.select { Users.id eq id }
            .map { resultRowToUser(it) }
            .singleOrNull()
    }

    override suspend fun getUserByEmail(email: String): User? = transaction {
        Users.select { Users.email eq email }
            .map { resultRowToUser(it) }
            .singleOrNull()
    }

    override suspend fun verifyPassword(email: String, password: String): User? = transaction {
        val row = Users.select { Users.email eq email }.singleOrNull()
        if (row != null && BCrypt.checkpw(password, row[Users.passwordHash])) {
            resultRowToUser(row)
        } else {
            null
        }
    }
}
