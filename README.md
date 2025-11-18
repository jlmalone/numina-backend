# Numina Backend

Kotlin/Ktor backend server for the Numina group fitness social platform.

## Overview

Numina is a group fitness social platform that combines elements of ClassPass, Meetup, and community-focused fitness. Think of it as a connection-oriented platform (not a dating app!) that matches people to each other and to fitness classes/events, fostering community, accountability, and shared fitness journeys.

This backend provides:

- User authentication and profile management
- Fitness class catalog and discovery
- User matching algorithms (coming soon)
- Messaging and coordination features (coming soon)
- Ratings and feedback system (coming soon)

## Technology Stack

- **Language**: Kotlin JVM 2.0+
- **Framework**: Ktor 3.0+
- **Database**: PostgreSQL for production, H2 in-memory for development
- **DI**: Koin 4.0
- **Authentication**: JWT tokens with refresh token support
- **ORM**: Exposed with kotlinx.datetime
- **Serialization**: kotlinx.serialization
- **Testing**: Ktor test framework
- **Containerization**: Docker & Docker Compose

## Quick Start

### Prerequisites

- JDK 17 or higher
- Gradle 8.5+ (or use included wrapper)
- Docker & Docker Compose (optional, for PostgreSQL)

### Running with H2 (In-Memory Database)

```bash
# Clone the repository
git clone https://github.com/yourusername/numina-backend.git
cd numina-backend

# Run the application
./gradlew run

# The server will start on http://localhost:8080
```

### Running with Docker Compose (PostgreSQL)

```bash
# Start PostgreSQL and backend
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Configuration

Configuration is managed via `application.yaml` in `src/main/resources/`.

### Environment Variables

- `JWT_SECRET`: Secret key for JWT token signing (default: "default-secret-change-in-production")

### Database Configuration

For PostgreSQL, update `application.yaml`:

```yaml
database:
  type: postgresql
  jdbcUrl: jdbc:postgresql://localhost:5432/numina
  user: postgres
  password: postgres
```

## API Documentation

Base URL: `http://localhost:8080/api/v1`

### Health Check

```bash
GET /health
```

### Authentication Endpoints

#### Register

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123",
  "name": "John Doe"
}

# Response: 201 Created
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "a1b2c3d4-...",
  "user": {
    "id": 1,
    "email": "user@example.com",
    "createdAt": "2025-01-15T10:00:00Z",
    "updatedAt": "2025-01-15T10:00:00Z"
  }
}
```

#### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123"
}

# Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "a1b2c3d4-...",
  "user": { ... }
}
```

#### Refresh Token

```bash
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "a1b2c3d4-..."
}

# Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "e5f6g7h8-..."
}
```

#### Logout

```bash
POST /api/v1/auth/logout
Authorization: Bearer <token>

# Response: 200 OK
{
  "message": "Logged out successfully"
}
```

### User Profile Endpoints

All user profile endpoints require authentication (Bearer token).

#### Get My Profile

```bash
GET /api/v1/users/me
Authorization: Bearer <token>

# Response: 200 OK
{
  "userId": 1,
  "name": "John Doe",
  "bio": "Fitness enthusiast",
  "locationLat": 40.7128,
  "locationLong": -74.0060,
  "fitnessInterests": ["yoga", "running", "cycling"],
  "fitnessLevel": 7,
  "availability": {
    "Monday": ["morning", "evening"],
    "Wednesday": ["evening"]
  },
  "photoUrl": "https://example.com/photo.jpg",
  "privacySettings": {
    "bioPublic": true,
    "locationPublic": true,
    "fitnessInterestsPublic": true,
    "fitnessLevelPublic": true,
    "availabilityPublic": false
  }
}
```

#### Update My Profile

```bash
PUT /api/v1/users/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "bio": "Passionate about yoga and outdoor running",
  "fitnessLevel": 8,
  "fitnessInterests": ["yoga", "running", "hiking"],
  "locationLat": 40.7128,
  "locationLong": -74.0060,
  "availability": {
    "Monday": ["morning"],
    "Wednesday": ["evening"],
    "Saturday": ["morning", "afternoon"]
  }
}

# Response: 200 OK (returns updated profile)
```

#### Get Public Profile

```bash
GET /api/v1/users/{id}
Authorization: Bearer <token>

# Response: 200 OK
{
  "userId": 2,
  "name": "Jane Smith",
  "bio": "Yoga instructor",
  "fitnessInterests": ["yoga", "pilates"],
  "fitnessLevel": 9,
  "photoUrl": "https://example.com/jane.jpg"
}
# Note: Only public fields are returned based on privacy settings
```

### Class Catalog Endpoints

#### List Classes (Public)

```bash
GET /api/v1/classes

# Optional query parameters:
# - lat, long, radius (km): Location-based filtering
# - startDate, endDate: Date range (ISO 8601 format)
# - minPrice, maxPrice: Price range
# - minIntensity, maxIntensity: Intensity range (1-10)
# - tags: Filter by tags (can specify multiple)

# Example:
GET /api/v1/classes?lat=40.7128&long=-74.0060&radius=5&minIntensity=5

# Response: 200 OK
[
  {
    "id": 1,
    "name": "Morning Yoga",
    "description": "Relaxing yoga session",
    "datetime": "2025-01-16T08:00:00Z",
    "locationLat": 40.7128,
    "locationLong": -74.0060,
    "trainer": "Jane Doe",
    "intensity": 5,
    "price": 25.0,
    "externalBookingUrl": "https://example.com/book/123",
    "providerId": "classpass-123",
    "capacity": 20,
    "tags": ["yoga", "morning", "beginner"],
    "createdAt": "2025-01-15T10:00:00Z"
  }
]
```

#### Get Class by ID (Public)

```bash
GET /api/v1/classes/{id}

# Response: 200 OK (returns single class)
```

#### Create Class (Admin - Requires Auth)

```bash
POST /api/v1/classes
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "HIIT Bootcamp",
  "description": "High intensity interval training",
  "datetime": "2025-01-17T18:00:00Z",
  "locationLat": 40.7128,
  "locationLong": -74.0060,
  "trainer": "Mike Johnson",
  "intensity": 9,
  "price": 35.0,
  "capacity": 15,
  "tags": ["hiit", "cardio", "advanced"]
}

# Response: 201 Created (returns created class)
```

#### Update Class (Admin - Requires Auth)

```bash
PUT /api/v1/classes/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "price": 30.0,
  "capacity": 18
}

# Response: 200 OK (returns updated class)
```

### Push Notifications (FCM)

The backend supports push notifications via Firebase Cloud Messaging for Android, iOS, and Web platforms.

#### Firebase Setup

1. Create a Firebase project at https://console.firebase.google.com
2. Go to Project Settings > Service Accounts
3. Click "Generate New Private Key" to download `firebase-service-account.json`
4. Place the file in `src/main/resources/` (it's already in `.gitignore`)
5. Update `application.yaml`:

```yaml
firebase:
  serviceAccountPath: "src/main/resources/firebase-service-account.json"
```

**IMPORTANT**: Never commit Firebase credentials to version control!

#### Device Registration

```bash
POST /api/v1/notifications/register-device
Authorization: Bearer <token>
Content-Type: application/json

{
  "platform": "ANDROID",  # or "IOS", "WEB"
  "token": "fcm-device-token-here"
}

# Response: 201 Created
{
  "id": "uuid",
  "userId": 1,
  "platform": "ANDROID",
  "token": "fcm-device-token-here",
  "active": true,
  "createdAt": "2025-01-15T10:00:00Z",
  "lastUsedAt": "2025-01-15T10:00:00Z"
}
```

#### Remove Device

```bash
DELETE /api/v1/notifications/device/{tokenId}
Authorization: Bearer <token>

# Response: 200 OK
```

#### Get Notification Preferences

```bash
GET /api/v1/notifications/preferences
Authorization: Bearer <token>

# Response: 200 OK
{
  "id": "uuid",
  "userId": 1,
  "messagesEnabled": true,
  "matchesEnabled": true,
  "groupsEnabled": true,
  "classRemindersEnabled": true,
  "socialEnabled": true,
  "emailFallback": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00",
  "updatedAt": "2025-01-15T10:00:00Z"
}
```

#### Update Notification Preferences

```bash
PUT /api/v1/notifications/preferences
Authorization: Bearer <token>
Content-Type: application/json

{
  "messagesEnabled": false,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00"
}

# Response: 200 OK (returns updated preferences)
```

#### Get Notification History

```bash
GET /api/v1/notifications/history?page=1&pageSize=20
Authorization: Bearer <token>

# Response: 200 OK
{
  "notifications": [
    {
      "id": "uuid",
      "userId": 1,
      "type": "MESSAGE",  # MESSAGE, MATCH, GROUP, REMINDER, SOCIAL, SYSTEM
      "title": "New Message",
      "body": "You have a new message from Jane",
      "data": { "senderId": "123", "conversationId": "456" },
      "priority": "NORMAL",  # URGENT, HIGH, NORMAL, LOW
      "read": false,
      "clicked": false,
      "sentAt": "2025-01-15T10:00:00Z",
      "createdAt": "2025-01-15T10:00:00Z"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 42
}
```

#### Mark Notification as Read

```bash
POST /api/v1/notifications/{id}/mark-read
Authorization: Bearer <token>

# Response: 200 OK
```

#### Delete Notification

```bash
DELETE /api/v1/notifications/{id}
Authorization: Bearer <token>

# Response: 200 OK
```

#### Send Notification (Admin)

```bash
POST /api/v1/admin/notifications/send
Authorization: Bearer <token>
Content-Type: application/json

# Send to single user
{
  "userId": 123,
  "type": "SYSTEM",
  "title": "System Update",
  "body": "The app will be updated tonight",
  "data": { "updateVersion": "2.0" },
  "priority": "HIGH"
}

# OR send to multiple users
{
  "userIds": [123, 456, 789],
  "type": "MATCH",
  "title": "New Match!",
  "body": "You've been matched with a workout buddy",
  "priority": "NORMAL"
}

# Response: 201 Created
```

#### Broadcast Notification (Admin)

```bash
POST /api/v1/admin/notifications/broadcast
Authorization: Bearer <token>
Content-Type: application/json

{
  "type": "SYSTEM",
  "title": "System Announcement",
  "body": "Join us for the community fitness challenge!",
  "priority": "HIGH"
}

# Response: 201 Created
```

#### Notification Types

- **MESSAGE**: New chat messages
- **MATCH**: New workout partner matches
- **GROUP**: Group invitations and updates
- **REMINDER**: Upcoming class reminders
- **SOCIAL**: Follows, likes, comments
- **SYSTEM**: System announcements

#### Quiet Hours

Users can set quiet hours (e.g., 22:00 to 08:00) during which push notifications will not be sent. Notifications are still created and stored, but FCM push is suppressed.

## Database Schema

### Users Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| email | VARCHAR(255) | Unique email address |
| password_hash | VARCHAR(255) | Bcrypt hashed password |
| created_at | TIMESTAMP | Account creation time |
| updated_at | TIMESTAMP | Last update time |

### UserProfiles Table
| Column | Type | Description |
|--------|------|-------------|
| user_id | INTEGER | Foreign key to Users |
| name | VARCHAR(255) | User's display name |
| bio | TEXT | User biography |
| location_lat | DOUBLE | Latitude |
| location_long | DOUBLE | Longitude |
| fitness_interests | JSONB | Array of interests |
| fitness_level | INTEGER | Skill level (1-10) |
| availability | JSONB | Schedule map |
| photo_url | VARCHAR(500) | Profile photo URL |
| privacy_settings | JSONB | Privacy preferences |

### Classes Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| name | VARCHAR(255) | Class name |
| description | TEXT | Class description |
| datetime | TIMESTAMP | Class date/time |
| location_lat | DOUBLE | Latitude |
| location_long | DOUBLE | Longitude |
| trainer | VARCHAR(255) | Trainer name |
| intensity | INTEGER | Intensity (1-10) |
| price | DOUBLE | Class price |
| external_booking_url | VARCHAR(500) | Booking link |
| provider_id | VARCHAR(100) | External provider ID |
| capacity | INTEGER | Max participants |
| tags | JSONB | Class tags |
| created_at | TIMESTAMP | Record creation time |

### RefreshTokens Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| user_id | INTEGER | Foreign key to Users |
| token | VARCHAR(500) | Refresh token (UUID) |
| expires_at | TIMESTAMP | Expiration time |
| created_at | TIMESTAMP | Token creation time |
| is_revoked | BOOLEAN | Revocation status |

### DeviceTokens Table
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | INTEGER | Foreign key to Users |
| platform | VARCHAR(50) | Platform (android/ios/web) |
| token | VARCHAR(500) | FCM device token (unique) |
| active | BOOLEAN | Token active status |
| created_at | TIMESTAMP | Token creation time |
| last_used_at | TIMESTAMP | Last usage time |

### NotificationPreferences Table
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | INTEGER | Foreign key to Users (unique) |
| messages_enabled | BOOLEAN | Message notifications |
| matches_enabled | BOOLEAN | Match notifications |
| groups_enabled | BOOLEAN | Group notifications |
| class_reminders_enabled | BOOLEAN | Class reminder notifications |
| social_enabled | BOOLEAN | Social notifications |
| email_fallback | BOOLEAN | Email fallback enabled |
| quiet_hours_start | TIME | Quiet hours start (nullable) |
| quiet_hours_end | TIME | Quiet hours end (nullable) |
| updated_at | TIMESTAMP | Last update time |

### Notifications Table
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user_id | INTEGER | Foreign key to Users |
| type | VARCHAR(50) | Notification type |
| title | VARCHAR(255) | Notification title |
| body | TEXT | Notification body |
| data | JSONB | Additional payload data |
| priority | VARCHAR(50) | Priority level |
| read | BOOLEAN | Read status |
| clicked | BOOLEAN | Clicked status |
| sent_at | TIMESTAMP | Sent time |
| delivered_at | TIMESTAMP | Delivery time (nullable) |
| read_at | TIMESTAMP | Read time (nullable) |
| created_at | TIMESTAMP | Creation time |

### NotificationDeliveryLog Table
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| notification_id | UUID | Foreign key to Notifications |
| device_token_id | UUID | Foreign key to DeviceTokens |
| status | VARCHAR(50) | Delivery status |
| error_message | TEXT | Error details (nullable) |
| attempted_at | TIMESTAMP | Attempt time |

## Project Structure

```
numina-backend/
├── src/main/kotlin/com/numina/
│   ├── Application.kt          # Main entry point
│   ├── plugins/                # Ktor plugins config
│   │   ├── Routing.kt
│   │   ├── Security.kt
│   │   ├── Serialization.kt
│   │   ├── Database.kt
│   │   └── Koin.kt
│   ├── domain/                 # Domain models
│   │   ├── User.kt
│   │   ├── UserProfile.kt
│   │   └── FitnessClass.kt
│   ├── data/                   # Database layer
│   │   ├── tables/             # Exposed table definitions
│   │   └── repositories/       # Data access repositories
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   └── ClassRoutes.kt
│   └── auth/                   # JWT and auth logic
│       └── JwtConfig.kt
├── src/main/resources/
│   ├── application.yaml        # Application config
│   └── logback.xml             # Logging config
├── src/test/kotlin/
│   └── com/numina/             # Integration tests
├── build.gradle.kts
├── docker-compose.yml
└── Dockerfile
```

## Security Considerations

- **JWT Secret**: Always use a strong, randomly generated secret in production
- **Password Hashing**: Bcrypt with automatic salt generation
- **CORS**: Currently set to `anyHost()` for development - restrict in production
- **HTTPS**: Configure SSL/TLS certificates for production deployments
- **Environment Variables**: Never commit secrets to version control

## Testing

The project includes integration tests for all major endpoints:

- `AuthRoutesTest`: Registration, login, token refresh
- `UserProfileRoutesTest`: Profile retrieval and updates
- `ClassRoutesTest`: Class creation, listing, and filtering

Tests use an in-memory H2 database for isolation.

## Known Limitations

- No role-based access control (RBAC) - all authenticated users can create classes
- Location-based filtering uses Haversine formula (accurate but not optimized for large datasets)
- No rate limiting or request throttling
- File upload for profile photos not implemented (URL-based only)
- No email verification for registration

## Next Steps

See [TODO.md](TODO.md) for planned features and enhancements.

## Development

This repository was scaffolded with Claude Code assistance.

## License

Proprietary - All rights reserved

---

**Numina** - Where fitness meets community
