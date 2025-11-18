package com.numina.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object Groups : Table("groups") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val photoUrl = varchar("photo_url", 512).nullable()
    val category = varchar("category", 50)
    val isPrivate = bool("is_private").default(false)
    val maxMembers = integer("max_members").default(50)
    val ownerId = integer("owner_id").references(Users.id)
    val location = varchar("location", 255).nullable()
    val latitude = decimal("latitude", 10, 8).nullable()
    val longitude = decimal("longitude", 11, 8).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object GroupMembers : Table("group_members") {
    val id = varchar("id", 36)
    val groupId = varchar("group_id", 36).references(Groups.id)
    val userId = integer("user_id").references(Users.id)
    val role = varchar("role", 20)
    val status = varchar("status", 20)
    val joinedAt = timestamp("joined_at")

    override val primaryKey = PrimaryKey(id)
}

object GroupActivities : Table("group_activities") {
    val id = varchar("id", 36)
    val groupId = varchar("group_id", 36).references(Groups.id)
    val classId = integer("class_id").references(Classes.id).nullable()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val scheduledAt = timestamp("scheduled_at")
    val location = varchar("location", 255).nullable()
    val latitude = decimal("latitude", 10, 8).nullable()
    val longitude = decimal("longitude", 11, 8).nullable()
    val isRecurring = bool("is_recurring").default(false)
    val recurrenceRule = varchar("recurrence_rule", 255).nullable()
    val createdById = integer("created_by_id").references(Users.id)
    val createdAt = timestamp("created_at")
    val cancelled = bool("cancelled").default(false)

    override val primaryKey = PrimaryKey(id)
}

object ActivityRSVPs : Table("activity_rsvps") {
    val id = varchar("id", 36)
    val activityId = varchar("activity_id", 36).references(GroupActivities.id)
    val userId = integer("user_id").references(Users.id)
    val status = varchar("status", 20)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object GroupInvites : Table("group_invites") {
    val id = varchar("id", 36)
    val groupId = varchar("group_id", 36).references(Groups.id)
    val inviterId = integer("inviter_id").references(Users.id)
    val inviteeId = integer("invitee_id").references(Users.id).nullable()
    val inviteCode = varchar("invite_code", 64).nullable().uniqueIndex()
    val status = varchar("status", 20)
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")

    override val primaryKey = PrimaryKey(id)
}
