package com.numina.data.repositories

import com.numina.data.tables.GroupActivities
import com.numina.data.tables.ActivityRSVPs
import com.numina.domain.groups.GroupActivity
import com.numina.domain.groups.ActivityRSVP
import com.numina.domain.groups.RSVPStats
import com.numina.domain.groups.UpdateActivityRequest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface GroupActivityRepository {
    suspend fun createActivity(
        groupId: String,
        classId: Int?,
        title: String,
        description: String?,
        scheduledAt: Instant,
        location: String?,
        latitude: Double?,
        longitude: Double?,
        isRecurring: Boolean,
        recurrenceRule: String?,
        createdById: Int
    ): GroupActivity?

    suspend fun getActivityById(activityId: String): GroupActivity?
    suspend fun getGroupActivities(groupId: String, upcomingOnly: Boolean = false): List<GroupActivity>
    suspend fun updateActivity(activityId: String, request: UpdateActivityRequest): GroupActivity?
    suspend fun cancelActivity(activityId: String): Boolean
    suspend fun deleteActivity(activityId: String): Boolean

    // RSVP methods
    suspend fun createOrUpdateRSVP(activityId: String, userId: Int, status: String): ActivityRSVP?
    suspend fun getRSVP(activityId: String, userId: Int): ActivityRSVP?
    suspend fun getActivityRSVPs(activityId: String): List<ActivityRSVP>
    suspend fun getRSVPStats(activityId: String): RSVPStats
}

class GroupActivityRepositoryImpl : GroupActivityRepository {

    private fun resultRowToGroupActivity(row: ResultRow, rsvpStats: RSVPStats? = null): GroupActivity {
        return GroupActivity(
            id = row[GroupActivities.id],
            groupId = row[GroupActivities.groupId],
            classId = row[GroupActivities.classId],
            title = row[GroupActivities.title],
            description = row[GroupActivities.description],
            scheduledAt = row[GroupActivities.scheduledAt],
            location = row[GroupActivities.location],
            latitude = row[GroupActivities.latitude]?.toDouble(),
            longitude = row[GroupActivities.longitude]?.toDouble(),
            isRecurring = row[GroupActivities.isRecurring],
            recurrenceRule = row[GroupActivities.recurrenceRule],
            createdById = row[GroupActivities.createdById],
            createdAt = row[GroupActivities.createdAt],
            cancelled = row[GroupActivities.cancelled],
            rsvpStats = rsvpStats
        )
    }

    private fun resultRowToActivityRSVP(row: ResultRow): ActivityRSVP {
        return ActivityRSVP(
            id = row[ActivityRSVPs.id],
            activityId = row[ActivityRSVPs.activityId],
            userId = row[ActivityRSVPs.userId],
            status = row[ActivityRSVPs.status],
            createdAt = row[ActivityRSVPs.createdAt],
            updatedAt = row[ActivityRSVPs.updatedAt]
        )
    }

    override suspend fun createActivity(
        groupId: String,
        classId: Int?,
        title: String,
        description: String?,
        scheduledAt: Instant,
        location: String?,
        latitude: Double?,
        longitude: Double?,
        isRecurring: Boolean,
        recurrenceRule: String?,
        createdById: Int
    ): GroupActivity? = transaction {
        val now = Clock.System.now()
        val activityId = UUID.randomUUID().toString()

        GroupActivities.insert {
            it[id] = activityId
            it[GroupActivities.groupId] = groupId
            it[GroupActivities.classId] = classId
            it[GroupActivities.title] = title
            it[GroupActivities.description] = description
            it[GroupActivities.scheduledAt] = scheduledAt
            it[GroupActivities.location] = location
            it[GroupActivities.latitude] = latitude?.toBigDecimal()
            it[GroupActivities.longitude] = longitude?.toBigDecimal()
            it[GroupActivities.isRecurring] = isRecurring
            it[GroupActivities.recurrenceRule] = recurrenceRule
            it[GroupActivities.createdById] = createdById
            it[createdAt] = now
            it[cancelled] = false
        }

        getActivityById(activityId)
    }

    override suspend fun getActivityById(activityId: String): GroupActivity? = transaction {
        val row = GroupActivities.select { GroupActivities.id eq activityId }.singleOrNull()
        row?.let {
            val stats = getRSVPStats(activityId)
            resultRowToGroupActivity(it, stats)
        }
    }

    override suspend fun getGroupActivities(groupId: String, upcomingOnly: Boolean): List<GroupActivity> = transaction {
        val query = GroupActivities.select {
            (GroupActivities.groupId eq groupId) and (GroupActivities.cancelled eq false)
        }

        if (upcomingOnly) {
            val now = Clock.System.now()
            query.andWhere { GroupActivities.scheduledAt greaterEq now }
        }

        query.orderBy(GroupActivities.scheduledAt to SortOrder.ASC)
            .map { row ->
                val stats = getRSVPStats(row[GroupActivities.id])
                resultRowToGroupActivity(row, stats)
            }
    }

    override suspend fun updateActivity(activityId: String, request: UpdateActivityRequest): GroupActivity? = transaction {
        var updateCount = 0

        GroupActivities.update({ GroupActivities.id eq activityId }) { stmt ->
            request.title?.let {
                stmt[title] = it
                updateCount++
            }
            request.description?.let {
                stmt[description] = it
                updateCount++
            }
            request.scheduledAt?.let {
                stmt[scheduledAt] = it
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
            request.isRecurring?.let {
                stmt[isRecurring] = it
                updateCount++
            }
            request.recurrenceRule?.let {
                stmt[recurrenceRule] = it
                updateCount++
            }
        }

        if (updateCount > 0) getActivityById(activityId) else null
    }

    override suspend fun cancelActivity(activityId: String): Boolean = transaction {
        GroupActivities.update({ GroupActivities.id eq activityId }) {
            it[cancelled] = true
        } > 0
    }

    override suspend fun deleteActivity(activityId: String): Boolean = transaction {
        GroupActivities.deleteWhere { GroupActivities.id eq activityId } > 0
    }

    override suspend fun createOrUpdateRSVP(activityId: String, userId: Int, status: String): ActivityRSVP? = transaction {
        val now = Clock.System.now()

        // Check if RSVP exists
        val existing = ActivityRSVPs.select {
            (ActivityRSVPs.activityId eq activityId) and (ActivityRSVPs.userId eq userId)
        }.singleOrNull()

        if (existing != null) {
            // Update existing RSVP
            ActivityRSVPs.update({
                (ActivityRSVPs.activityId eq activityId) and (ActivityRSVPs.userId eq userId)
            }) {
                it[ActivityRSVPs.status] = status
                it[updatedAt] = now
            }
        } else {
            // Create new RSVP
            val rsvpId = UUID.randomUUID().toString()
            ActivityRSVPs.insert {
                it[id] = rsvpId
                it[ActivityRSVPs.activityId] = activityId
                it[ActivityRSVPs.userId] = userId
                it[ActivityRSVPs.status] = status
                it[createdAt] = now
                it[updatedAt] = now
            }
        }

        getRSVP(activityId, userId)
    }

    override suspend fun getRSVP(activityId: String, userId: Int): ActivityRSVP? = transaction {
        ActivityRSVPs.select {
            (ActivityRSVPs.activityId eq activityId) and (ActivityRSVPs.userId eq userId)
        }.map { resultRowToActivityRSVP(it) }.singleOrNull()
    }

    override suspend fun getActivityRSVPs(activityId: String): List<ActivityRSVP> = transaction {
        ActivityRSVPs.select { ActivityRSVPs.activityId eq activityId }
            .map { resultRowToActivityRSVP(it) }
    }

    override suspend fun getRSVPStats(activityId: String): RSVPStats = transaction {
        val rsvps = getActivityRSVPs(activityId)
        RSVPStats(
            going = rsvps.count { it.status == "GOING" },
            maybe = rsvps.count { it.status == "MAYBE" },
            notGoing = rsvps.count { it.status == "NOT_GOING" }
        )
    }
}
