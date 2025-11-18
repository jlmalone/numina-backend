# TASK: Add Groups & Coordination System to Numina Backend

> **IMPORTANT**: Before starting, check if `.task-groups-completed` file exists in the root directory.
> If it exists, respond: "✅ Groups system has already been implemented. See README.md for details."
> **When finished**, create `.task-groups-completed` file with timestamp and summary.

## 🎯 OBJECTIVE

Implement a complete groups and workout coordination system allowing users to create, join, and manage fitness groups for classes and workouts.

## 📋 REQUIREMENTS

### Core Features

1. **Group Management**
   - Create public/private groups
   - Group descriptions, photos, and metadata
   - Group member limits (e.g., 2-50 members)
   - Group categories (running club, yoga group, HIIT crew, etc.)
   - Group location/area
   - Group tags/interests

2. **Membership**
   - Join public groups instantly
   - Request to join private groups (approval workflow)
   - Leave groups
   - Member roles: Owner, Admin, Member
   - Invite users to groups via link or direct invite
   - Remove members (owner/admin only)

3. **Group Activities**
   - Schedule group workouts/classes
   - Link group activities to specific fitness classes
   - RSVP system (going, maybe, not going)
   - Activity comments and updates
   - Recurring activities (weekly runs, etc.)

4. **Discovery**
   - Browse public groups
   - Search groups by name, category, location
   - Filter by member count, activity level
   - Recommended groups based on user interests

### API Endpoints to Implement

```
# Group Management
POST   /api/v1/groups                         # Create a group
GET    /api/v1/groups                         # List/search groups
GET    /api/v1/groups/{id}                    # Get group details
PUT    /api/v1/groups/{id}                    # Update group
DELETE /api/v1/groups/{id}                    # Delete group
GET    /api/v1/groups/{id}/members            # List group members
POST   /api/v1/groups/{id}/join               # Join a group
POST   /api/v1/groups/{id}/leave              # Leave a group
POST   /api/v1/groups/{id}/invite             # Invite user to group
POST   /api/v1/groups/{id}/kick/{userId}      # Remove member
GET    /api/v1/groups/my-groups               # User's groups
GET    /api/v1/groups/discover                # Discover groups

# Membership Requests
GET    /api/v1/groups/{id}/requests           # Pending join requests
POST   /api/v1/groups/{id}/requests/approve/{userId}   # Approve request
POST   /api/v1/groups/{id}/requests/reject/{userId}    # Reject request

# Group Activities
POST   /api/v1/groups/{id}/activities         # Create group activity
GET    /api/v1/groups/{id}/activities         # List group activities
GET    /api/v1/groups/{id}/activities/{activityId}  # Get activity details
PUT    /api/v1/groups/{id}/activities/{activityId}  # Update activity
DELETE /api/v1/groups/{id}/activities/{activityId}  # Cancel activity
POST   /api/v1/groups/{id}/activities/{activityId}/rsvp  # RSVP to activity
```

### Database Schema

**groups**:
- id (UUID, primary key)
- name (VARCHAR, not null)
- description (TEXT)
- photo_url (VARCHAR, nullable)
- category (VARCHAR: running, yoga, hiit, cycling, general, etc.)
- is_private (BOOLEAN, default false)
- max_members (INT, default 50)
- owner_id (UUID, foreign key to users)
- location (VARCHAR, nullable)
- latitude (DECIMAL, nullable)
- longitude (DECIMAL, nullable)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**group_members**:
- id (UUID, primary key)
- group_id (UUID, foreign key to groups)
- user_id (UUID, foreign key to users)
- role (ENUM: owner, admin, member)
- joined_at (TIMESTAMP)
- status (ENUM: active, pending, removed)

**group_activities**:
- id (UUID, primary key)
- group_id (UUID, foreign key to groups)
- class_id (UUID, foreign key to fitness_classes, nullable)
- title (VARCHAR)
- description (TEXT)
- scheduled_at (TIMESTAMP)
- location (VARCHAR)
- latitude (DECIMAL, nullable)
- longitude (DECIMAL, nullable)
- is_recurring (BOOLEAN, default false)
- recurrence_rule (VARCHAR, nullable)
- created_by_id (UUID, foreign key to users)
- created_at (TIMESTAMP)
- cancelled (BOOLEAN, default false)

**activity_rsvps**:
- id (UUID, primary key)
- activity_id (UUID, foreign key to group_activities)
- user_id (UUID, foreign key to users)
- status (ENUM: going, maybe, not_going)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**group_invites**:
- id (UUID, primary key)
- group_id (UUID, foreign key to groups)
- inviter_id (UUID, foreign key to users)
- invitee_id (UUID, foreign key to users, nullable)
- invite_code (VARCHAR, unique, nullable) # For shareable links
- status (ENUM: pending, accepted, declined, expired)
- created_at (TIMESTAMP)
- expires_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION GUIDE

### Step 1: Data Models
Create Kotlin data classes for:
- Group
- GroupMember
- GroupActivity
- ActivityRSVP
- GroupInvite
- DTOs for all requests/responses

### Step 2: Database Layer
- Create Exposed table definitions for all schemas
- Implement migration scripts
- Create repositories:
  - GroupRepository
  - GroupMemberRepository
  - GroupActivityRepository

### Step 3: Service Layer
Create `GroupService.kt` with methods:
- `createGroup(ownerId, groupData): Group`
- `updateGroup(groupId, ownerId, groupData): Group`
- `deleteGroup(groupId, ownerId)`
- `joinGroup(groupId, userId)`
- `leaveGroup(groupId, userId)`
- `inviteToGroup(groupId, inviterId, inviteeId)`
- `kickMember(groupId, adminId, memberId)`
- `getGroupMembers(groupId): List<User>`
- `getUserGroups(userId): List<Group>`
- `discoverGroups(filters, page, pageSize): List<Group>`

Create `GroupActivityService.kt`:
- `createActivity(groupId, creatorId, activityData): GroupActivity`
- `updateActivity(activityId, userId, activityData): GroupActivity`
- `cancelActivity(activityId, userId)`
- `rsvpToActivity(activityId, userId, status)`
- `getActivityRSVPs(activityId): List<RSVP>`
- `getGroupActivities(groupId, upcoming): List<GroupActivity>`

### Step 4: REST API Routes
- Implement all endpoints listed above
- Add JWT authentication
- Validate permissions (owners/admins for management actions)
- Add pagination for listings

### Step 5: Business Logic
- Enforce max member limits
- Validate group owner permissions
- Handle join request approval workflow
- Generate unique invite codes
- Implement activity reminders (if time permits)

### Step 6: Testing
Write tests for:
- Group creation and management
- Membership workflows
- Activity scheduling and RSVPs
- Permission checks
- Discovery and search

## ✅ ACCEPTANCE CRITERIA

- [ ] All 20+ endpoints are implemented and working
- [ ] Users can create and manage groups
- [ ] Join/leave functionality works correctly
- [ ] Private groups require approval
- [ ] Group activities can be scheduled and linked to classes
- [ ] RSVP system tracks attendance
- [ ] Discovery shows relevant groups based on location/interests
- [ ] Permission system enforces owner/admin privileges
- [ ] All database migrations are created
- [ ] Unit tests cover service layer
- [ ] Integration tests verify API endpoints
- [ ] API documentation is updated

## 📝 DELIVERABLES

1. **Source Code**:
   - `src/main/kotlin/com/numina/groups/` directory
   - `src/main/kotlin/com/numina/routes/GroupRoutes.kt`
   - Database migration files

2. **Tests**:
   - `src/test/kotlin/com/numina/groups/GroupServiceTest.kt`
   - `src/test/kotlin/com/numina/groups/GroupActivityServiceTest.kt`
   - `src/test/kotlin/com/numina/routes/GroupRoutesTest.kt`

3. **Documentation**:
   - Update README.md
   - Update API documentation
   - Add group management examples

## 🚀 WHEN COMPLETE

1. Run tests: `./gradlew test`
2. Build: `./gradlew build`
3. Update README.md
4. Create `.task-groups-completed` file
5. Commit: "Add groups and workout coordination system"
6. Push to branch: `git push -u origin claude/add-groups-system`

---

**Estimated Completion Time**: 75-90 minutes
**Priority**: HIGH - Core coordination feature
