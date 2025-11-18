package com.numina.data.repositories

import com.numina.data.tables.AdminUsers
import com.numina.domain.AdminRole
import com.numina.domain.AdminUser
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface AdminUserRepository {
    suspend fun createAdmin(userId: Int, role: AdminRole, permissions: Map<String, Boolean>): AdminUser
    suspend fun getAdminByUserId(userId: Int): AdminUser?
    suspend fun getAdminById(id: UUID): AdminUser?
    suspend fun updatePermissions(id: UUID, permissions: Map<String, Boolean>): AdminUser?
    suspend fun deleteAdmin(id: UUID): Boolean
}

class AdminUserRepositoryImpl : AdminUserRepository {
    override suspend fun createAdmin(userId: Int, role: AdminRole, permissions: Map<String, Boolean>): AdminUser = transaction {
        val id = AdminUsers.insertAndGetId {
            it[AdminUsers.userId] = userId
            it[AdminUsers.role] = role.name
            it[AdminUsers.permissions] = Json.encodeToString(kotlinx.serialization.serializer(), permissions)
            it[createdAt] = kotlinx.datetime.Clock.System.now().toJavaInstant()
        }

        AdminUsers.select { AdminUsers.id eq id }.map { rowToAdminUser(it) }.single()
    }

    override suspend fun getAdminByUserId(userId: Int): AdminUser? = transaction {
        AdminUsers.select { AdminUsers.userId eq userId }
            .map { rowToAdminUser(it) }
            .singleOrNull()
    }

    override suspend fun getAdminById(id: UUID): AdminUser? = transaction {
        AdminUsers.select { AdminUsers.id eq id }
            .map { rowToAdminUser(it) }
            .singleOrNull()
    }

    override suspend fun updatePermissions(id: UUID, permissions: Map<String, Boolean>): AdminUser? = transaction {
        AdminUsers.update({ AdminUsers.id eq id }) {
            it[AdminUsers.permissions] = Json.encodeToString(kotlinx.serialization.serializer(), permissions)
        }
        getAdminById(id)
    }

    override suspend fun deleteAdmin(id: UUID): Boolean = transaction {
        AdminUsers.deleteWhere { AdminUsers.id eq id } > 0
    }

    private fun rowToAdminUser(row: ResultRow): AdminUser {
        val permissionsJson = row[AdminUsers.permissions]
        val permissions = Json.decodeFromString<Map<String, Boolean>>(permissionsJson)

        return AdminUser(
            id = row[AdminUsers.id].value.toString(),
            userId = row[AdminUsers.userId],
            role = AdminRole.valueOf(row[AdminUsers.role]),
            permissions = permissions,
            createdAt = row[AdminUsers.createdAt].toKotlinInstant()
        )
    }
}
