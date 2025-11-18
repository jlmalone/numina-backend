# TASK: Add Admin API to Numina Backend

> **IMPORTANT**: Check for `.task-admin-completed` before starting.
> **When finished**, create `.task-admin-completed` file.

## 🎯 OBJECTIVE

Build admin API endpoints for platform management, moderation, and analytics.

## 📋 CORE FEATURES

1. **User Management**
   - List/search users
   - View user details and activity
   - Suspend/ban users
   - Reset passwords
   - Impersonate users (for support)

2. **Content Moderation**
   - Review flagged messages/reviews
   - Approve/reject reviews
   - Remove inappropriate content
   - Ban list management

3. **Provider/Class Management**
   - Manually add/edit providers
   - Manage class listings
   - Override scraper data
   - Feature classes/providers

4. **Analytics & Reports**
   - User growth metrics
   - Class booking trends
   - Provider popularity
   - Revenue reports (if applicable)
   - Export CSV/JSON

5. **System Admin**
   - Broadcast notifications
   - System settings
   - Feature flags
   - Background job management

### API Endpoints

```
# User Management
GET    /api/v1/admin/users                    # List users
GET    /api/v1/admin/users/{id}               # User details
POST   /api/v1/admin/users/{id}/suspend       # Suspend user
POST   /api/v1/admin/users/{id}/unsuspend     # Unsuspend user
POST   /api/v1/admin/users/{id}/reset-password # Reset password

# Content Moderation
GET    /api/v1/admin/moderation/queue         # Flagged content queue
POST   /api/v1/admin/moderation/reviews/{id}/approve
POST   /api/v1/admin/moderation/reviews/{id}/reject
POST   /api/v1/admin/moderation/messages/{id}/delete

# Provider/Class Management
POST   /api/v1/admin/providers                # Create provider
PUT    /api/v1/admin/providers/{id}           # Update provider
DELETE /api/v1/admin/providers/{id}           # Delete provider
POST   /api/v1/admin/classes                  # Create class
PUT    /api/v1/admin/classes/{id}             # Update class
DELETE /api/v1/admin/classes/{id}             # Delete class
POST   /api/v1/admin/classes/{id}/feature     # Feature class

# Analytics
GET    /api/v1/admin/analytics/users          # User metrics
GET    /api/v1/admin/analytics/classes        # Class metrics
GET    /api/v1/admin/analytics/engagement     # Engagement metrics
GET    /api/v1/admin/analytics/export         # Export data

# System
POST   /api/v1/admin/notifications/broadcast  # Send broadcast
GET    /api/v1/admin/settings                 # System settings
PUT    /api/v1/admin/settings                 # Update settings
GET    /api/v1/admin/jobs                     # Background jobs status
```

### Database Schema

**admin_users**:
- user_id (UUID, FK to users, PK)
- role (ENUM: super_admin, admin, moderator)
- permissions (JSONB) # Fine-grained permissions
- created_at (TIMESTAMP)

**admin_audit_log**:
- id (UUID, PK)
- admin_user_id (UUID, FK to users)
- action (VARCHAR)
- entity_type (VARCHAR)
- entity_id (UUID)
- changes (JSONB)
- ip_address (VARCHAR)
- created_at (TIMESTAMP)

**feature_flags**:
- id (UUID, PK)
- name (VARCHAR, unique)
- enabled (BOOLEAN, default false)
- description (TEXT)
- rollout_percentage (INT, default 100)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**system_settings**:
- key (VARCHAR, PK)
- value (TEXT)
- type (ENUM: string, int, boolean, json)
- description (TEXT)
- updated_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION

### Services
- `AdminUserService.kt`: User management
- `ModerationService.kt`: Content moderation
- `AdminAnalyticsService.kt`: Generate reports
- `FeatureFlagService.kt`: Feature flag management
- `AuditLogService.kt`: Track admin actions

### Security
- Separate admin authentication (admin JWT)
- Role-based access control (RBAC)
- All admin actions logged to audit trail
- IP allowlist for admin endpoints (optional)

### Middleware
- Admin authentication middleware
- Permission check middleware
- Audit logging middleware

### Testing
- Admin permission checks
- Moderation workflows
- Analytics calculations
- Audit log creation

## ✅ ACCEPTANCE CRITERIA

- [ ] Admin authentication working
- [ ] User management endpoints functional
- [ ] Content moderation queue operational
- [ ] Analytics generate accurate metrics
- [ ] All admin actions logged
- [ ] RBAC enforced correctly
- [ ] Feature flags toggle functionality
- [ ] CSV export working

## 📝 DELIVERABLES

- Admin services implementation
- Admin routes with auth
- Audit logging system
- Analytics queries
- Database migrations
- Tests
- Admin API documentation

## 🚀 COMPLETION

1. Test: `./gradlew test`
2. Build: `./gradlew build`
3. Create `.task-admin-completed`
4. Commit: "Add admin API for platform management"
5. Push: `git push -u origin claude/add-admin-api`

---

**Estimated Time**: 75-90 minutes
**Priority**: MEDIUM
