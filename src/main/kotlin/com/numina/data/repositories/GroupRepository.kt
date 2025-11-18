package com.numina.data.repositories

import com.numina.data.tables.Groups
import com.numina.data.tables.GroupMembers
import com.numina.domain.groups.Group
import com.numina.domain.groups.GroupFilters
import com.numina.domain.groups.UpdateGroupRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface GroupRepository {
    suspend fun createGroup(
        name: String,
        description: String?,
        photoUrl: String?,
        category: String,
        isPrivate: Boolean,
        maxMembers: Int,
        ownerId: Int,
        location: String?,
        latitude: Double?,
        longitude: Double?
    ): Group?

    suspend fun getGroupById(id: String): Group?
    suspend fun getGroups(filters: GroupFilters): List<Group>
    suspend fun updateGroup(id: String, request: UpdateGroupRequest): Group?
    suspend fun deleteGroup(id: String): Boolean
    suspend fun getUserGroups(userId: Int): List<Group>
}

class GroupRepositoryImpl : GroupRepository {

    private fun resultRowToGroup(row: ResultRow, memberCount: Int = 0): Group {
        return Group(
            id = row[Groups.id],
            name = row[Groups.name],
            description = row[Groups.description],
            photoUrl = row[Groups.photoUrl],
            category = row[Groups.category],
            isPrivate = row[Groups.isPrivate],
            maxMembers = row[Groups.maxMembers],
            ownerId = row[Groups.ownerId],
            location = row[Groups.location],
            latitude = row[Groups.latitude]?.toDouble(),
            longitude = row[Groups.longitude]?.toDouble(),
            memberCount = memberCount,
            createdAt = row[Groups.createdAt],
            updatedAt = row[Groups.updatedAt]
        )
    }

    override suspend fun createGroup(
        name: String,
        description: String?,
        photoUrl: String?,
        category: String,
        isPrivate: Boolean,
        maxMembers: Int,
        ownerId: Int,
        location: String?,
        latitude: Double?,
        longitude: Double?
    ): Group? = transaction {
        val now = Clock.System.now()
        val groupId = UUID.randomUUID().toString()

        Groups.insert {
            it[id] = groupId
            it[Groups.name] = name
            it[Groups.description] = description
            it[Groups.photoUrl] = photoUrl
            it[Groups.category] = category
            it[Groups.isPrivate] = isPrivate
            it[Groups.maxMembers] = maxMembers
            it[Groups.ownerId] = ownerId
            it[Groups.location] = location
            it[Groups.latitude] = latitude?.toBigDecimal()
            it[Groups.longitude] = longitude?.toBigDecimal()
            it[createdAt] = now
            it[updatedAt] = now
        }

        Groups.select { Groups.id eq groupId }
            .map { resultRowToGroup(it, 0) }
            .singleOrNull()
    }

    override suspend fun getGroupById(id: String): Group? = transaction {
        val memberCount = GroupMembers.select {
            (GroupMembers.groupId eq id) and (GroupMembers.status eq "ACTIVE")
        }.count().toInt()

        Groups.select { Groups.id eq id }
            .map { resultRowToGroup(it, memberCount) }
            .singleOrNull()
    }

    override suspend fun getGroups(filters: GroupFilters): List<Group> = transaction {
        val query = Groups.selectAll()

        filters.category?.let { cat ->
            query.andWhere { Groups.category eq cat }
        }

        filters.isPrivate?.let { private ->
            query.andWhere { Groups.isPrivate eq private }
        }

        filters.search?.let { search ->
            query.andWhere {
                (Groups.name like "%$search%") or (Groups.description like "%$search%")
            }
        }

        filters.location?.let { loc ->
            query.andWhere { Groups.location like "%$loc%" }
        }

        val groups = query.map { row ->
            val memberCount = GroupMembers.select {
                (GroupMembers.groupId eq row[Groups.id]) and (GroupMembers.status eq "ACTIVE")
            }.count().toInt()
            resultRowToGroup(row, memberCount)
        }

        // Filter by member count if specified
        val filteredGroups = groups.filter { group ->
            val minMembersMatch = filters.minMembers?.let { group.memberCount >= it } ?: true
            val maxMembersMatch = filters.maxMembers?.let { group.memberCount <= it } ?: true
            minMembersMatch && maxMembersMatch
        }

        // Filter by location radius if specified
        if (filters.latitude != null && filters.longitude != null && filters.radiusKm != null) {
            filteredGroups.filter { group ->
                if (group.latitude == null || group.longitude == null) {
                    false
                } else {
                    val distance = calculateDistance(
                        filters.latitude, filters.longitude,
                        group.latitude, group.longitude
                    )
                    distance <= filters.radiusKm
                }
            }
        } else {
            filteredGroups
        }
    }

    override suspend fun updateGroup(id: String, request: UpdateGroupRequest): Group? = transaction {
        val now = Clock.System.now()
        var updateCount = 0

        Groups.update({ Groups.id eq id }) { stmt ->
            request.name?.let {
                stmt[name] = it
                updateCount++
            }
            request.description?.let {
                stmt[description] = it
                updateCount++
            }
            request.photoUrl?.let {
                stmt[photoUrl] = it
                updateCount++
            }
            request.category?.let {
                stmt[category] = it
                updateCount++
            }
            request.isPrivate?.let {
                stmt[isPrivate] = it
                updateCount++
            }
            request.maxMembers?.let {
                stmt[maxMembers] = it
                updateCount++
            }
            request.location?.let {
                stmt[location] = it
                updateCount++
            }
            request.latitude?.let {
                stmt[latitude] = it.toBigDecimal()
                updateCount++
            }
            request.longitude?.let {
                stmt[longitude] = it.toBigDecimal()
                updateCount++
            }
            if (updateCount > 0) {
                stmt[updatedAt] = now
            }
        }

        if (updateCount > 0) getGroupById(id) else null
    }

    override suspend fun deleteGroup(id: String): Boolean = transaction {
        Groups.deleteWhere { Groups.id eq id } > 0
    }

    override suspend fun getUserGroups(userId: Int): List<Group> = transaction {
        val groupIds = GroupMembers.select {
            (GroupMembers.userId eq userId) and (GroupMembers.status eq "ACTIVE")
        }.map { it[GroupMembers.groupId] }

        Groups.select { Groups.id inList groupIds }
            .map { row ->
                val memberCount = GroupMembers.select {
                    (GroupMembers.groupId eq row[Groups.id]) and (GroupMembers.status eq "ACTIVE")
                }.count().toInt()
                resultRowToGroup(row, memberCount)
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusKm * c
    }
}
