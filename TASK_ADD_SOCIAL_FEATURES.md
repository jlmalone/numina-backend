# TASK: Add Social Features to Numina Backend

> **IMPORTANT**: Check for `.task-social-completed` before starting.
> **When finished**, create `.task-social-completed` file.

## 🎯 OBJECTIVE

Implement social networking features: following/followers, activity feeds, user discovery, and social interactions.

## 📋 CORE FEATURES

1. **Following System**
   - Follow/unfollow users
   - Followers/following lists
   - Follow suggestions based on interests
   - Block users from following

2. **Activity Feed**
   - User's feed shows activity from followed users
   - Activity types: completed workouts, joined groups, new reviews, achieved milestones
   - Like/react to activity items
   - Comment on activities
   - Feed pagination and real-time updates

3. **User Discovery**
   - Find users by name, location, fitness interests
   - Suggested users (similar fitness level, nearby, shared interests)
   - Filter by activity level, fitness goals

4. **Profile Interactions**
   - View public user profiles
   - See mutual connections
   - See shared group memberships
   - See workout history (if public)

### API Endpoints

```
# Following
POST   /api/v1/social/follow/{userId}         # Follow a user
DELETE /api/v1/social/unfollow/{userId}       # Unfollow a user
GET    /api/v1/social/following               # Users I follow
GET    /api/v1/social/followers               # My followers
GET    /api/v1/social/suggestions             # Follow suggestions

# Activity Feed
GET    /api/v1/social/feed                    # My activity feed
POST   /api/v1/social/activity                # Create activity post
GET    /api/v1/social/activity/{id}           # Get activity details
DELETE /api/v1/social/activity/{id}           # Delete own activity
POST   /api/v1/social/activity/{id}/like      # Like activity
DELETE /api/v1/social/activity/{id}/like      # Unlike activity
POST   /api/v1/social/activity/{id}/comment   # Comment on activity
GET    /api/v1/social/activity/{id}/comments  # Get comments

# User Discovery
GET    /api/v1/social/discover-users          # Discover users
GET    /api/v1/social/users/{id}/profile      # View user profile
GET    /api/v1/social/users/{id}/activities   # User's public activities
GET    /api/v1/social/mutual-connections/{id} # Mutual connections
```

### Database Schema

**follows**:
- id (UUID, PK)
- follower_id (UUID, FK to users)
- following_id (UUID, FK to users)
- created_at (TIMESTAMP)
- UNIQUE(follower_id, following_id)

**activity_feed**:
- id (UUID, PK)
- user_id (UUID, FK to users)
- activity_type (ENUM: workout_completed, group_joined, review_posted, milestone_achieved, class_attended)
- content (TEXT)
- metadata (JSONB) # Class info, group info, etc.
- visibility (ENUM: public, followers, private)
- created_at (TIMESTAMP)

**activity_likes**:
- id (UUID, PK)
- activity_id (UUID, FK to activity_feed)
- user_id (UUID, FK to users)
- created_at (TIMESTAMP)
- UNIQUE(activity_id, user_id)

**activity_comments**:
- id (UUID, PK)
- activity_id (UUID, FK to activity_feed)
- user_id (UUID, FK to users)
- content (TEXT)
- created_at (TIMESTAMP)

**user_stats** (denormalized for performance):
- user_id (UUID, FK to users, PK)
- followers_count (INT, default 0)
- following_count (INT, default 0)
- activities_count (INT, default 0)
- workouts_count (INT, default 0)
- updated_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION

### Services
- `SocialService.kt`: follow/unfollow, discovery, suggestions
- `ActivityFeedService.kt`: feed generation, activity creation, likes/comments
- Background job to update user stats

### Testing
- Follow/unfollow workflows
- Feed generation performance
- Privacy settings enforcement
- Activity visibility logic

## ✅ ACCEPTANCE CRITERIA

- [ ] Follow/unfollow system working
- [ ] Activity feed shows relevant content
- [ ] Like and comment functionality working
- [ ] User discovery with filters
- [ ] Privacy settings enforced
- [ ] Performance optimized (feed generation < 500ms)
- [ ] All endpoints tested

## 📝 DELIVERABLES

- Social service implementation
- Activity feed service
- API routes
- Database migrations
- Tests
- Updated documentation

## 🚀 COMPLETION

1. Test: `./gradlew test`
2. Build: `./gradlew build`
3. Create `.task-social-completed`
4. Commit: "Add social features (following, activity feed, discovery)"
5. Push: `git push -u origin claude/add-social-features`

---

**Estimated Time**: 60-75 minutes
**Priority**: MEDIUM-HIGH
