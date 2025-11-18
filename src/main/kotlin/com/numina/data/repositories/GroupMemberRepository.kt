package com.numina.data.repositories

import com.numina.data.tables.GroupMembers
import com.numina.data.tables.GroupInvites
import com.numina.data.tables.Users
import com.numina.data.tables.UserProfiles
import com.numina.domain.groups.GroupMember
import com.numina.domain.groups.GroupMemberWithUser
import com.numina.domain.groups.GroupInvite
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface GroupMemberRepository {
    suspend fun addMember(groupId: String, userId: Int, role: String, status: String): GroupMember?
    suspend fun getMember(groupId: String, userId: Int): GroupMember?
    suspend fun getMembers(groupId: String, status: String? = null): List<GroupMemberWithUser>
    suspend fun updateMemberRole(groupId: String, userId: Int, role: String): GroupMember?
    suspend fun updateMemberStatus(groupId: String, userId: Int, status: String): GroupMember?
    suspend fun removeMember(groupId: String, userId: Int): Boolean
    suspend fun getMemberCount(groupId: String, status: String = "ACTIVE"): Int

    // Invite methods
    suspend fun createInvite(
        groupId: String,
        inviterId: Int,
        inviteeId: Int?,
        inviteCode: String?,
        expiresAt: kotlinx.datetime.Instant
    ): GroupInvite?
    suspend fun getInviteByCode(inviteCode: String): GroupInvite?
    suspend fun getInviteById(inviteId: String): GroupInvite?
    suspend fun updateInviteStatus(inviteId: String, status: String): GroupInvite?
    suspend fun getPendingRequests(groupId: String): List<GroupMemberWithUser>
}

class GroupMemberRepositoryImpl : GroupMemberRepository {

    private fun resultRowToGroupMember(row: ResultRow): GroupMember {
        return GroupMember(
            id = row[GroupMembers.id],
            groupId = row[GroupMembers.groupId],
            userId = row[GroupMembers.userId],
            role = row[GroupMembers.role],
            status = row[GroupMembers.status],
            joinedAt = row[GroupMembers.joinedAt]
        )
    }

    private fun resultRowToGroupMemberWithUser(row: ResultRow): GroupMemberWithUser {
        return GroupMemberWithUser(
            id = row[GroupMembers.id],
            groupId = row[GroupMembers.groupId],
            userId = row[GroupMembers.userId],
            userEmail = row[Users.email],
            userName = row[UserProfiles.name],
            role = row[GroupMembers.role],
            status = row[GroupMembers.status],
            joinedAt = row[GroupMembers.joinedAt]
        )
    }

    private fun resultRowToGroupInvite(row: ResultRow): GroupInvite {
        return GroupInvite(
            id = row[GroupInvites.id],
            groupId = row[GroupInvites.groupId],
            inviterId = row[GroupInvites.inviterId],
            inviteeId = row[GroupInvites.inviteeId],
            inviteCode = row[GroupInvites.inviteCode],
            status = row[GroupInvites.status],
            createdAt = row[GroupInvites.createdAt],
            expiresAt = row[GroupInvites.expiresAt]
        )
    }

    override suspend fun addMember(
        groupId: String,
        userId: Int,
        role: String,
        status: String
    ): GroupMember? = transaction {
        val now = Clock.System.now()
        val memberId = UUID.randomUUID().toString()

        // Check if member already exists
        val existing = GroupMembers.select {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }.singleOrNull()

        if (existing != null) {
            return@transaction null
        }

        GroupMembers.insert {
            it[id] = memberId
            it[GroupMembers.groupId] = groupId
            it[GroupMembers.userId] = userId
            it[GroupMembers.role] = role
            it[GroupMembers.status] = status
            it[joinedAt] = now
        }

        GroupMembers.select { GroupMembers.id eq memberId }
            .map { resultRowToGroupMember(it) }
            .singleOrNull()
    }

    override suspend fun getMember(groupId: String, userId: Int): GroupMember? = transaction {
        GroupMembers.select {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }.map { resultRowToGroupMember(it) }.singleOrNull()
    }

    override suspend fun getMembers(groupId: String, status: String?): List<GroupMemberWithUser> = transaction {
        val query = GroupMembers
            .join(Users, JoinType.INNER, GroupMembers.userId, Users.id)
            .join(UserProfiles, JoinType.LEFT, Users.id, UserProfiles.userId)
            .select { GroupMembers.groupId eq groupId }

        status?.let {
            query.andWhere { GroupMembers.status eq it }
        }

        query.map { resultRowToGroupMemberWithUser(it) }
    }

    override suspend fun updateMemberRole(groupId: String, userId: Int, role: String): GroupMember? = transaction {
        GroupMembers.update({
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }) {
            it[GroupMembers.role] = role
        }

        getMember(groupId, userId)
    }

    override suspend fun updateMemberStatus(groupId: String, userId: Int, status: String): GroupMember? = transaction {
        GroupMembers.update({
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        }) {
            it[GroupMembers.status] = status
        }

        getMember(groupId, userId)
    }

    override suspend fun removeMember(groupId: String, userId: Int): Boolean = transaction {
        GroupMembers.deleteWhere {
            (GroupMembers.groupId eq groupId) and (GroupMembers.userId eq userId)
        } > 0
    }

    override suspend fun getMemberCount(groupId: String, status: String): Int = transaction {
        GroupMembers.select {
            (GroupMembers.groupId eq groupId) and (GroupMembers.status eq status)
        }.count().toInt()
    }

    override suspend fun createInvite(
        groupId: String,
        inviterId: Int,
        inviteeId: Int?,
        inviteCode: String?,
        expiresAt: kotlinx.datetime.Instant
    ): GroupInvite? = transaction {
        val now = Clock.System.now()
        val inviteId = UUID.randomUUID().toString()

        GroupInvites.insert {
            it[id] = inviteId
            it[GroupInvites.groupId] = groupId
            it[GroupInvites.inviterId] = inviterId
            it[GroupInvites.inviteeId] = inviteeId
            it[GroupInvites.inviteCode] = inviteCode
            it[status] = "PENDING"
            it[createdAt] = now
            it[GroupInvites.expiresAt] = expiresAt
        }

        GroupInvites.select { GroupInvites.id eq inviteId }
            .map { resultRowToGroupInvite(it) }
            .singleOrNull()
    }

    override suspend fun getInviteByCode(inviteCode: String): GroupInvite? = transaction {
        GroupInvites.select { GroupInvites.inviteCode eq inviteCode }
            .map { resultRowToGroupInvite(it) }
            .singleOrNull()
    }

    override suspend fun getInviteById(inviteId: String): GroupInvite? = transaction {
        GroupInvites.select { GroupInvites.id eq inviteId }
            .map { resultRowToGroupInvite(it) }
            .singleOrNull()
    }

    override suspend fun updateInviteStatus(inviteId: String, status: String): GroupInvite? = transaction {
        GroupInvites.update({ GroupInvites.id eq inviteId }) {
            it[GroupInvites.status] = status
        }

        getInviteById(inviteId)
    }

    override suspend fun getPendingRequests(groupId: String): List<GroupMemberWithUser> = transaction {
        getMembers(groupId, "PENDING")
    }
}
