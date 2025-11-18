# TASK: Add Push Notifications System to Numina Backend

> **IMPORTANT**: Before starting, check if `.task-notifications-completed` file exists.
> If it exists, respond: "✅ Notifications system has already been implemented."
> **When finished**, create `.task-notifications-completed` file with timestamp and summary.

## 🎯 OBJECTIVE

Implement a comprehensive push notifications system for Android, iOS, and web platforms using Firebase Cloud Messaging (FCM).

## 📋 REQUIREMENTS

### Core Features

1. **Device Token Management**
   - Register device tokens for push notifications
   - Support multiple devices per user
   - Update/refresh expired tokens
   - Remove tokens on logout

2. **Notification Types**
   - New message notifications
   - Match notifications (new workout partner matches)
   - Group invitations and activity updates
   - Class reminders (upcoming classes user is interested in)
   - Social notifications (follows, likes, comments)
   - System announcements

3. **Notification Preferences**
   - Per-type notification toggle (messages, matches, groups, etc.)
   - Quiet hours (do not disturb schedule)
   - Email notification fallback
   - Push/in-app/email delivery preferences

4. **Notification Delivery**
   - Queue system for batch notifications
   - Priority levels (urgent, high, normal, low)
   - Delivery tracking (sent, delivered, clicked)
   - Retry logic for failed deliveries

### API Endpoints to Implement

```
# Device Registration
POST   /api/v1/notifications/register-device    # Register FCM token
DELETE /api/v1/notifications/device/{tokenId}   # Remove device token

# Preferences
GET    /api/v1/notifications/preferences        # Get notification preferences
PUT    /api/v1/notifications/preferences        # Update preferences

# Notification History
GET    /api/v1/notifications/history            # User's notification history
POST   /api/v1/notifications/{id}/mark-read     # Mark as read
DELETE /api/v1/notifications/{id}               # Delete notification

# Admin/System (Internal Use)
POST   /api/v1/admin/notifications/send         # Send notification to user(s)
POST   /api/v1/admin/notifications/broadcast    # Broadcast to all users
```

### Database Schema

**device_tokens**:
- id (UUID, primary key)
- user_id (UUID, foreign key to users)
- platform (ENUM: android, ios, web)
- token (VARCHAR, not null, unique)
- active (BOOLEAN, default true)
- created_at (TIMESTAMP)
- last_used_at (TIMESTAMP)

**notification_preferences**:
- id (UUID, primary key)
- user_id (UUID, foreign key to users, unique)
- messages_enabled (BOOLEAN, default true)
- matches_enabled (BOOLEAN, default true)
- groups_enabled (BOOLEAN, default true)
- class_reminders_enabled (BOOLEAN, default true)
- social_enabled (BOOLEAN, default true)
- email_fallback (BOOLEAN, default true)
- quiet_hours_start (TIME, nullable)
- quiet_hours_end (TIME, nullable)
- updated_at (TIMESTAMP)

**notifications**:
- id (UUID, primary key)
- user_id (UUID, foreign key to users)
- type (ENUM: message, match, group, reminder, social, system)
- title (VARCHAR)
- body (TEXT)
- data (JSONB) # Additional payload data
- priority (ENUM: urgent, high, normal, low)
- read (BOOLEAN, default false)
- clicked (BOOLEAN, default false)
- sent_at (TIMESTAMP)
- delivered_at (TIMESTAMP, nullable)
- read_at (TIMESTAMP, nullable)
- created_at (TIMESTAMP)

**notification_delivery_log**:
- id (UUID, primary key)
- notification_id (UUID, foreign key to notifications)
- device_token_id (UUID, foreign key to device_tokens)
- status (ENUM: pending, sent, delivered, failed, clicked)
- error_message (TEXT, nullable)
- attempted_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION GUIDE

### Step 1: Add FCM Dependency
```kotlin
// build.gradle.kts
implementation("com.google.firebase:firebase-admin:9.2.0")
```

### Step 2: Firebase Admin Setup
- Initialize Firebase Admin SDK with service account
- Create `FirebaseService.kt` for FCM operations
- Add `firebase-service-account.json` to resources (gitignored)

### Step 3: Data Models
Create:
- DeviceToken
- NotificationPreferences
- Notification
- NotificationDeliveryLog
- DTOs for requests/responses

### Step 4: Service Layer
Create `NotificationService.kt`:
- `registerDevice(userId, platform, token)`
- `removeDevice(tokenId, userId)`
- `sendNotification(userId, type, title, body, data)`
- `sendToMultipleUsers(userIds, type, title, body, data)`
- `broadcastNotification(type, title, body, data)`
- `getUserPreferences(userId): NotificationPreferences`
- `updatePreferences(userId, preferences)`
- `getNotificationHistory(userId, page, pageSize)`

Create `FCMService.kt`:
- `sendPushNotification(tokens, notification)`
- `sendToTopic(topic, notification)`
- Handle FCM responses and token invalidation

### Step 5: Background Job System
- Implement notification queue processor
- Batch notifications for efficiency
- Respect user quiet hours
- Retry failed deliveries

### Step 6: Integration Points
Trigger notifications when:
- New message received → message notification
- New match found → match notification
- Group invitation received → group notification
- Class scheduled in 1 hour → reminder notification
- User followed → social notification

### Step 7: REST API Routes
Implement all endpoints with JWT auth

### Step 8: Testing
- Unit tests for notification service
- Integration tests for FCM sending
- Test quiet hours logic
- Test preference filtering

## ✅ ACCEPTANCE CRITERIA

- [ ] FCM integration working for Android, iOS, and web
- [ ] Device tokens registered and managed correctly
- [ ] All notification types can be sent
- [ ] User preferences control notification delivery
- [ ] Quiet hours respected
- [ ] Notification history available
- [ ] Failed deliveries are retried
- [ ] Invalid tokens are marked inactive
- [ ] All endpoints tested and documented

## 📝 DELIVERABLES

1. **Source Code**:
   - `src/main/kotlin/com/numina/notifications/`
   - `src/main/kotlin/com/numina/routes/NotificationRoutes.kt`
   - Database migrations

2. **Configuration**:
   - Firebase Admin SDK setup instructions
   - Environment variables for FCM
   - `.env.example` updated

3. **Tests**:
   - Service layer tests
   - API endpoint tests

4. **Documentation**:
   - README.md updated
   - FCM setup guide
   - Notification payload examples

## 🚀 WHEN COMPLETE

1. Test: `./gradlew test`
2. Build: `./gradlew build`
3. Update README.md
4. Create `.task-notifications-completed`
5. Commit: "Add push notifications system with FCM"
6. Push: `git push -u origin claude/add-notifications`

---

**Estimated Time**: 60-75 minutes
**Priority**: HIGH
