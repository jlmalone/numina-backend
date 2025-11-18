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
    suspend fun getAllUsers(limit: Int = 100, offset: Int = 0): List<User>
    suspend fun searchUsers(query: String, limit: Int = 100): List<User>
    suspend fun suspendUser(id: Int, reason: String): Boolean
    suspend fun unsuspendUser(id: Int): Boolean
    suspend fun resetPassword(id: Int, newPassword: String): Boolean
    suspend fun getUserCount(): Int
    suspend fun isSuspended(id: Int): Boolean
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

    override suspend fun getAllUsers(limit: Int, offset: Int): List<User> = transaction {
        Users.selectAll()
            .limit(limit, offset.toLong())
            .orderBy(Users.createdAt to SortOrder.DESC)
            .map { resultRowToUser(it) }
    }

    override suspend fun searchUsers(query: String, limit: Int): List<User> = transaction {
        Users.select { Users.email like "%$query%" }
            .limit(limit)
            .orderBy(Users.createdAt to SortOrder.DESC)
            .map { resultRowToUser(it) }
    }

    override suspend fun suspendUser(id: Int, reason: String): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[isSuspended] = true
            it[suspensionReason] = reason
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun unsuspendUser(id: Int): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[isSuspended] = false
            it[suspensionReason] = null
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun resetPassword(id: Int, newPassword: String): Boolean = transaction {
        val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        Users.update({ Users.id eq id }) {
            it[passwordHash] = hashedPassword
            it[updatedAt] = Clock.System.now()
        } > 0
    }

    override suspend fun getUserCount(): Int = transaction {
        Users.selectAll().count().toInt()
    }

    override suspend fun isSuspended(id: Int): Boolean = transaction {
        Users.select { Users.id eq id }
            .singleOrNull()
            ?.get(Users.isSuspended) ?: false
    }
}
