# Numina Backend

Kotlin/Ktor backend server for the Numina group fitness social platform.

## Overview

Numina is a group fitness social platform that combines elements of ClassPass, Meetup, and community-focused fitness. Think of it as a connection-oriented platform (not a dating app!) that matches people to each other and to fitness classes/events, fostering community, accountability, and shared fitness journeys.

This backend provides:

- User authentication and profile management
- Fitness class catalog and discovery
- Groups and workout coordination system
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

### Groups & Coordination Endpoints

#### List/Search Groups (Public)

```bash
GET /api/v1/groups

# Optional query parameters:
# - category: Filter by category (RUNNING, YOGA, HIIT, etc.)
# - isPrivate: Filter by privacy (true/false)
# - location: Location text filter
# - lat, long, radius: Location-based filtering (km)
# - minMembers, maxMembers: Member count range
# - search: Search in name and description
# - page, pageSize: Pagination

# Response: 200 OK (paginated)
{
  "items": [
    {
      "id": "uuid",
      "name": "Morning Runners NYC",
      "description": "Daily morning runs in Central Park",
      "photoUrl": "https://...",
      "category": "RUNNING",
      "isPrivate": false,
      "maxMembers": 50,
      "ownerId": 1,
      "location": "New York, NY",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "memberCount": 25,
      "createdAt": "2025-01-15T10:00:00Z",
      "updatedAt": "2025-01-15T10:00:00Z"
    }
  ],
  "pagination": { ... }
}
```

#### Discover Groups (Public Groups Only)

```bash
GET /api/v1/groups/discover
# Same query parameters as /groups
# Returns only public groups
```

#### Get Group by ID

```bash
GET /api/v1/groups/{id}

# Response: 200 OK (single group)
```

#### Create Group (Requires Auth)

```bash
POST /api/v1/groups
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Yoga Enthusiasts",
  "description": "Weekly yoga sessions for all levels",
  "photoUrl": "https://...",
  "category": "YOGA",
  "isPrivate": false,
  "maxMembers": 30,
  "location": "Brooklyn, NY",
  "latitude": 40.6782,
  "longitude": -73.9442
}

# Response: 201 Created
```

#### Update Group (Owner/Admin Only)

```bash
PUT /api/v1/groups/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "Updated description",
  "maxMembers": 40
}

# Response: 200 OK
```

#### Delete Group (Owner Only)

```bash
DELETE /api/v1/groups/{id}
Authorization: Bearer <token>

# Response: 200 OK
```

#### Get My Groups

```bash
GET /api/v1/groups/my-groups
Authorization: Bearer <token>

# Response: 200 OK (list of user's groups)
```

#### Get Group Members

```bash
GET /api/v1/groups/{id}/members
Authorization: Bearer <token>

# Response: 200 OK
[
  {
    "id": "member-uuid",
    "groupId": "group-uuid",
    "userId": 1,
    "userEmail": "user@example.com",
    "userName": "John Doe",
    "role": "OWNER",
    "status": "ACTIVE",
    "joinedAt": "2025-01-15T10:00:00Z"
  }
]
```

#### Join Group

```bash
POST /api/v1/groups/{id}/join
Authorization: Bearer <token>

# For public groups: instant join
# For private groups: creates pending request

# Response: 200 OK
```

#### Leave Group

```bash
POST /api/v1/groups/{id}/leave
Authorization: Bearer <token>

# Response: 200 OK
# Note: Owner cannot leave (must delete group or transfer ownership)
```

#### Invite User to Group

```bash
POST /api/v1/groups/{id}/invite
Authorization: Bearer <token>
Content-Type: application/json

# Direct invite to user:
{
  "userId": 5
}

# Or generate invite link:
{
  "generateLink": true
}

# Response for link: 201 Created
{
  "inviteCode": "abc123...",
  "expiresAt": "2025-01-22T10:00:00Z"
}
```

#### Remove Member (Admin/Owner Only)

```bash
DELETE /api/v1/groups/{id}/kick/{userId}
Authorization: Bearer <token>

# Response: 200 OK
```

#### Get Pending Join Requests (Admin/Owner Only)

```bash
GET /api/v1/groups/{id}/requests
Authorization: Bearer <token>

# Response: 200 OK (list of pending members)
```

#### Approve Join Request (Admin/Owner Only)

```bash
POST /api/v1/groups/{id}/requests/approve/{userId}
Authorization: Bearer <token>

# Response: 200 OK
```

#### Reject Join Request (Admin/Owner Only)

```bash
POST /api/v1/groups/{id}/requests/reject/{userId}
Authorization: Bearer <token>

# Response: 200 OK
```

### Group Activities Endpoints

#### Create Group Activity

```bash
POST /api/v1/groups/{id}/activities
Authorization: Bearer <token>
Content-Type: application/json

{
  "classId": 1,  # Optional: link to a fitness class
  "title": "Saturday Morning Run",
  "description": "5k run around Central Park",
  "scheduledAt": "2025-01-20T08:00:00Z",
  "location": "Central Park, NYC",
  "latitude": 40.785091,
  "longitude": -73.968285,
  "isRecurring": true,
  "recurrenceRule": "WEEKLY"
}

# Response: 201 Created
```

#### Get Group Activities

```bash
GET /api/v1/groups/{id}/activities
Authorization: Bearer <token>

# Optional query parameter:
# - upcoming=true: Only show future activities

# Response: 200 OK (list of activities with RSVP stats)
```

#### Get Activity Details

```bash
GET /api/v1/groups/{id}/activities/{activityId}
Authorization: Bearer <token>

# Response: 200 OK (single activity)
```

#### Update Activity (Creator/Admin Only)

```bash
PUT /api/v1/groups/{id}/activities/{activityId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Updated title",
  "scheduledAt": "2025-01-20T09:00:00Z"
}

# Response: 200 OK
```

#### Cancel Activity (Creator/Admin Only)

```bash
DELETE /api/v1/groups/{id}/activities/{activityId}
Authorization: Bearer <token>

# Response: 200 OK
```

#### RSVP to Activity

```bash
POST /api/v1/groups/{id}/activities/{activityId}/rsvp
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "GOING"  # or "MAYBE" or "NOT_GOING"
}

# Response: 200 OK
```

#### Get Activity RSVPs

```bash
GET /api/v1/groups/{id}/activities/{activityId}/rsvps
Authorization: Bearer <token>

# Response: 200 OK (list of RSVPs)
```

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

### Groups Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| name | VARCHAR(255) | Group name |
| description | TEXT | Group description |
| photo_url | VARCHAR(512) | Group photo URL |
| category | VARCHAR(50) | Group category |
| is_private | BOOLEAN | Private group flag |
| max_members | INTEGER | Maximum members |
| owner_id | INTEGER | Foreign key to Users |
| location | VARCHAR(255) | Location text |
| latitude | DECIMAL(10,8) | Latitude |
| longitude | DECIMAL(11,8) | Longitude |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

### GroupMembers Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| group_id | VARCHAR(36) | Foreign key to Groups |
| user_id | INTEGER | Foreign key to Users |
| role | VARCHAR(20) | OWNER, ADMIN, MEMBER |
| status | VARCHAR(20) | ACTIVE, PENDING, REMOVED |
| joined_at | TIMESTAMP | Join time |

### GroupActivities Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| group_id | VARCHAR(36) | Foreign key to Groups |
| class_id | INTEGER | Foreign key to Classes (nullable) |
| title | VARCHAR(255) | Activity title |
| description | TEXT | Activity description |
| scheduled_at | TIMESTAMP | Scheduled time |
| location | VARCHAR(255) | Location text |
| latitude | DECIMAL(10,8) | Latitude |
| longitude | DECIMAL(11,8) | Longitude |
| is_recurring | BOOLEAN | Recurring flag |
| recurrence_rule | VARCHAR(255) | Recurrence pattern |
| created_by_id | INTEGER | Foreign key to Users |
| created_at | TIMESTAMP | Creation time |
| cancelled | BOOLEAN | Cancellation flag |

### ActivityRSVPs Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| activity_id | VARCHAR(36) | Foreign key to GroupActivities |
| user_id | INTEGER | Foreign key to Users |
| status | VARCHAR(20) | GOING, MAYBE, NOT_GOING |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update time |

### GroupInvites Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| group_id | VARCHAR(36) | Foreign key to Groups |
| inviter_id | INTEGER | Foreign key to Users |
| invitee_id | INTEGER | Foreign key to Users (nullable) |
| invite_code | VARCHAR(64) | Invite code (nullable, unique) |
| status | VARCHAR(20) | PENDING, ACCEPTED, DECLINED, EXPIRED |
| created_at | TIMESTAMP | Creation time |
| expires_at | TIMESTAMP | Expiration time |

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
│   │   ├── FitnessClass.kt
│   │   └── groups/             # Groups domain models
│   │       └── Group.kt
│   ├── data/                   # Database layer
│   │   ├── tables/             # Exposed table definitions
│   │   │   ├── Users.kt
│   │   │   ├── Classes.kt
│   │   │   └── Groups.kt       # Groups tables
│   │   └── repositories/       # Data access repositories
│   │       ├── UserRepository.kt
│   │       ├── ClassRepository.kt
│   │       ├── GroupRepository.kt
│   │       ├── GroupMemberRepository.kt
│   │       └── GroupActivityRepository.kt
│   ├── services/               # Business logic
│   │   ├── AuthService.kt
│   │   ├── UserService.kt
│   │   ├── ClassService.kt
│   │   └── groups/             # Groups services
│   │       ├── GroupService.kt
│   │       └── GroupActivityService.kt
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   ├── ClassRoutes.kt
│   │   └── GroupRoutes.kt
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
