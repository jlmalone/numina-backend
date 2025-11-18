# TASK: Add Messaging System to Numina Backend

> **IMPORTANT**: Before starting, check if `.task-messaging-completed` file exists in the root directory.
> If it exists, respond: "✅ Messaging system has already been implemented. See README.md for details."
> **When finished**, create `.task-messaging-completed` file with timestamp and summary.

## 🎯 OBJECTIVE

Implement a complete direct messaging system allowing users to communicate with potential workout partners and group members.

## 📋 REQUIREMENTS

### Core Features

1. **One-on-One Messaging**
   - Users can send/receive text messages
   - Message history persistence
   - Real-time delivery status (sent, delivered, read)
   - Support for 5000+ character messages
   - Pagination for message history

2. **Conversations Management**
   - List all conversations for a user
   - Unread message counts
   - Last message preview
   - Conversation search/filter
   - Archive/delete conversations

3. **Real-Time Support**
   - WebSocket or SSE for real-time message delivery
   - Typing indicators
   - Online/offline status
   - Last seen timestamps

4. **Safety & Moderation**
   - Block/unblock users
   - Report inappropriate messages
   - Profanity filter (optional)
   - Message flagging system

### API Endpoints to Implement

```
POST   /api/v1/messages/send                 # Send a message
GET    /api/v1/messages/conversations         # List user's conversations
GET    /api/v1/messages/conversations/{id}    # Get messages in conversation
POST   /api/v1/messages/conversations/{id}/mark-read  # Mark messages as read
DELETE /api/v1/messages/{id}                  # Delete a message
POST   /api/v1/messages/block/{userId}        # Block a user
POST   /api/v1/messages/report/{messageId}    # Report a message
GET    /api/v1/messages/unread-count          # Get unread message count

# WebSocket endpoint
WS     /api/v1/ws/messages                    # WebSocket for real-time messaging
```

### Database Schema

Create the following tables:

**messages**:
- id (UUID, primary key)
- conversation_id (UUID, foreign key)
- sender_id (UUID, foreign key to users)
- content (TEXT)
- sent_at (TIMESTAMP)
- delivered_at (TIMESTAMP, nullable)
- read_at (TIMESTAMP, nullable)
- deleted (BOOLEAN, default false)
- created_at (TIMESTAMP)

**conversations**:
- id (UUID, primary key)
- participant_1_id (UUID, foreign key to users)
- participant_2_id (UUID, foreign key to users)
- last_message_at (TIMESTAMP)
- created_at (TIMESTAMP)
- archived_by_user_1 (BOOLEAN, default false)
- archived_by_user_2 (BOOLEAN, default false)

**blocked_users**:
- id (UUID, primary key)
- blocker_id (UUID, foreign key to users)
- blocked_id (UUID, foreign key to users)
- created_at (TIMESTAMP)

**message_reports**:
- id (UUID, primary key)
- message_id (UUID, foreign key to messages)
- reporter_id (UUID, foreign key to users)
- reason (VARCHAR)
- status (ENUM: pending, reviewed, resolved)
- created_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION GUIDE

### Step 1: Data Models
Create Kotlin data classes for:
- Message
- Conversation
- BlockedUser
- MessageReport
- DTOs for requests/responses

### Step 2: Database Layer
- Create Exposed table definitions
- Implement migration scripts
- Create repository layer with CRUD operations

### Step 3: Service Layer
Create `MessagingService.kt` with methods:
- `sendMessage(senderId, recipientId, content): Message`
- `getConversations(userId, page, pageSize): List<Conversation>`
- `getMessages(conversationId, page, pageSize): List<Message>`
- `markAsRead(conversationId, userId)`
- `blockUser(blockerId, blockedId)`
- `reportMessage(messageId, reporterId, reason)`
- `getUnreadCount(userId): Int`

### Step 4: WebSocket Implementation
- Implement WebSocket endpoint using Ktor WebSockets plugin
- Handle real-time message delivery
- Implement connection management
- Add authentication to WebSocket connections

### Step 5: REST API Routes
- Implement all REST endpoints listed above
- Add JWT authentication middleware
- Validate user permissions (can only read own messages)
- Add rate limiting for message sending

### Step 6: Testing
Write tests for:
- Message sending and receiving
- Conversation management
- Blocking functionality
- WebSocket connections
- Permission checks

## 📦 DEPENDENCIES TO ADD

Add to `build.gradle.kts`:
```kotlin
implementation("io.ktor:ktor-server-websockets:$ktor_version")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
```

## ✅ ACCEPTANCE CRITERIA

- [ ] All 8 REST endpoints are implemented and working
- [ ] WebSocket endpoint delivers messages in real-time
- [ ] Users can send/receive messages successfully
- [ ] Message history is paginated and performant
- [ ] Blocking functionality prevents unwanted communication
- [ ] Unread counts are accurate
- [ ] All database migrations are created
- [ ] Unit tests cover core messaging logic
- [ ] Integration tests verify API endpoints
- [ ] API documentation is updated
- [ ] README.md includes messaging setup instructions

## 📝 DELIVERABLES

1. **Source Code**:
   - `src/main/kotlin/com/numina/messaging/` directory with all code
   - `src/main/kotlin/com/numina/routes/MessagingRoutes.kt`
   - Database migration files in `src/main/resources/db/migration/`

2. **Tests**:
   - `src/test/kotlin/com/numina/messaging/MessagingServiceTest.kt`
   - `src/test/kotlin/com/numina/routes/MessagingRoutesTest.kt`

3. **Documentation**:
   - Update README.md with messaging feature description
   - Update API documentation with new endpoints
   - Add WebSocket connection examples

4. **Configuration**:
   - Add WebSocket configuration to `application.conf`
   - Add any new environment variables to `.env.example`

## 🚀 WHEN COMPLETE

1. Run all tests: `./gradlew test`
2. Verify build: `./gradlew build`
3. Update README.md with messaging feature description
4. Create `.task-messaging-completed` file with completion summary
5. Commit all changes with message: "Add messaging system with real-time WebSocket support"
6. Push to new branch: `git push -u origin claude/add-messaging-system`

## 💡 TECHNICAL NOTES

- Use Ktor WebSockets plugin for real-time features
- Consider using coroutines for async message delivery
- Implement proper connection cleanup for WebSockets
- Add indexes on conversation_id and sender_id for performance
- Consider message retention policies (e.g., auto-delete after 90 days)
- Implement proper error handling for WebSocket disconnections

---

**Estimated Completion Time**: 60-75 minutes
**Priority**: HIGH - Core social feature
