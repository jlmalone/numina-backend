# Numina Backend

Kotlin/Ktor backend server for the Numina group fitness social platform.

## Overview

Numina is a group fitness social platform that combines elements of ClassPass, Meetup, and community-focused fitness. Think of it as a connection-oriented platform (not a dating app!) that matches people to each other and to fitness classes/events, fostering community, accountability, and shared fitness journeys.

This backend provides:

- User authentication and profile management
- Fitness class catalog and discovery
- **User matching algorithms** (user-to-user and user-to-class recommendations)
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

### Matching Endpoints

All matching endpoints require authentication (Bearer token).

#### Get Potential Workout Partners

```bash
GET /api/v1/matches/partners
Authorization: Bearer <token>

# Optional query parameters:
# - limit: Maximum results (default: 20, max: 100)
# - minScore: Minimum match score 0-100 (default: 60)
# - radius: Maximum distance in km (default: 10.0)

# Example:
GET /api/v1/matches/partners?limit=10&minScore=70&radius=15.0

# Response: 200 OK
[
  {
    "userId": 5,
    "profile": {
      "userId": 5,
      "name": "Sarah Johnson",
      "bio": "Marathon runner and yoga enthusiast",
      "fitnessInterests": ["running", "yoga"],
      "fitnessLevel": 8,
      "photoUrl": "https://example.com/sarah.jpg"
    },
    "matchScore": 85,
    "matchReasons": [
      "Similar fitness levels (7 vs 8)",
      "2 shared interests: running, yoga",
      "Very close (3.2km away)",
      "Good schedule compatibility"
    ],
    "sharedInterests": ["running", "yoga"],
    "distanceKm": 3.2
  }
]
```

#### Get Recommended Classes

```bash
GET /api/v1/matches/classes
Authorization: Bearer <token>

# Optional query parameters:
# - limit: Maximum results (default: 20, max: 100)
# - minScore: Minimum match score 0-100 (default: 50)
# - startDate: Start of date range (ISO 8601, default: now)
# - endDate: End of date range (ISO 8601, default: +7 days)

# Example:
GET /api/v1/matches/classes?limit=15&minScore=60&startDate=2025-01-20T00:00:00Z

# Response: 200 OK
[
  {
    "classId": 12,
    "classDetails": {
      "id": 12,
      "name": "Power Yoga Flow",
      "description": "Dynamic vinyasa flow",
      "datetime": "2025-01-20T18:00:00Z",
      "locationLat": 40.7580,
      "locationLong": -73.9855,
      "trainer": "Emma Williams",
      "intensity": 7,
      "price": 28.0,
      "capacity": 18,
      "tags": ["yoga", "vinyasa", "intermediate"],
      "createdAt": "2025-01-15T10:00:00Z"
    },
    "matchScore": 82,
    "matchReasons": [
      "Matches your interests: yoga",
      "Great intensity level for you",
      "Very convenient location (2.1km)"
    ],
    "estimatedFit": "perfect"
  }
]
```

#### Get Mutual Matches

```bash
GET /api/v1/matches/mutual
Authorization: Bearer <token>

# Response: 200 OK
[
  {
    "userId": 7,
    "profile": {
      "userId": 7,
      "name": "Mike Chen",
      "fitnessInterests": ["cycling", "running"],
      "fitnessLevel": 6
    },
    "matchScore": 78,
    "matchedAt": "2025-01-15T14:30:00Z"
  }
]
```

#### Record Match Action

```bash
POST /api/v1/matches/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetUserId": 5,
  "action": "LIKE"  // Options: "LIKE", "PASS", "SUPER_LIKE"
}

# Response: 200 OK (if one-sided like)
{
  "mutual": false,
  "match": null
}

# Response: 200 OK (if mutual match created)
{
  "mutual": true,
  "match": {
    "userId": 5,
    "profile": { ... },
    "matchScore": 85,
    "matchReasons": [...],
    "sharedInterests": ["running", "yoga"],
    "distanceKm": 3.2
  }
}
```

### Matching Algorithm Details

#### User-to-User Matching

The algorithm uses weighted scoring (0-100 scale) based on:

- **Fitness Level Similarity (20%)**: ±2 levels is considered ideal
  - Same level: 100 points
  - ±1 level: 75 points
  - ±2 levels: 50 points
  - Further apart: Lower scores

- **Shared Fitness Interests (30%)**: Overlap in interests (yoga, HIIT, running, etc.)
  - 3+ shared interests: 100 points
  - 2 shared interests: 50 points
  - 1 shared interest: 25 points

- **Geographic Proximity (25%)**: Distance between users
  - ≤2 km: 100 points
  - ≤5 km: 80 points
  - ≤10 km: 60 points
  - ≤20 km: 40 points
  - >20 km: 20 points

- **Schedule Compatibility (20%)**: Overlapping availability
  - 5+ overlapping time slots: 100 points
  - 3-4 slots: 75 points
  - 1-2 slots: 50 points

- **Past Interactions (5%)**: Previous positive partnerships
  - Bonus points for users who've worked out together before

**Match Score Interpretation:**
- 80-100: Excellent match
- 65-79: Good match
- 50-64: Okay match
- Below 50: Not recommended

#### User-to-Class Matching

The algorithm uses weighted scoring (0-100 scale) based on:

- **Fitness Interests Match (35%)**: Class type matches user interests
- **Appropriate Intensity (25%)**: Class intensity aligns with user's fitness level
- **Schedule Fit (20%)**: Class time matches user availability
- **Location Convenience (15%)**: Class within user's preferred radius
- **Price Range (5%)**: Within user's budget preferences

**Estimated Fit:**
- "perfect": Score ≥80
- "good": Score 65-79
- "okay": Score 50-64

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

### MatchActions Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| user_id | INTEGER | Foreign key to Users |
| target_user_id | INTEGER | Foreign key to Users |
| action | VARCHAR(20) | Action type: LIKE, PASS, SUPER_LIKE |
| created_at | TIMESTAMP | Action timestamp |

**Indexes:**
- Unique constraint on (user_id, target_user_id)
- Index on user_id
- Index on target_user_id

### MutualMatches Table
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| user1_id | INTEGER | Foreign key to Users (smaller ID) |
| user2_id | INTEGER | Foreign key to Users (larger ID) |
| match_score | INTEGER | Match score (0-100) |
| matched_at | TIMESTAMP | When mutual match was created |

**Indexes:**
- Unique constraint on (user1_id, user2_id)
- Index on user1_id
- Index on user2_id
- Constraint: user1_id < user2_id (canonical ordering)

### MatchPreferences Table
| Column | Type | Description |
|--------|------|-------------|
| user_id | INTEGER | Primary key, foreign key to Users |
| max_distance_km | FLOAT | Maximum distance for matches (default: 10.0) |
| min_fitness_level | INTEGER | Minimum fitness level preference |
| max_fitness_level | INTEGER | Maximum fitness level preference |
| preferred_age_min | INTEGER | Minimum age preference |
| preferred_age_max | INTEGER | Maximum age preference |

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
│   │   └── Matching.kt         # Match models
│   ├── data/                   # Database layer
│   │   ├── tables/             # Exposed table definitions
│   │   │   ├── Users.kt
│   │   │   ├── UserProfiles.kt
│   │   │   ├── Classes.kt
│   │   │   ├── MatchActions.kt
│   │   │   ├── MutualMatches.kt
│   │   │   └── MatchPreferences.kt
│   │   └── repositories/       # Data access repositories
│   │       ├── UserRepository.kt
│   │       ├── ClassRepository.kt
│   │       ├── MatchActionRepository.kt
│   │       └── MutualMatchRepository.kt
│   ├── services/               # Business logic
│   │   ├── AuthService.kt
│   │   ├── UserService.kt
│   │   ├── ClassService.kt
│   │   ├── MatchingService.kt
│   │   ├── UserMatcher.kt
│   │   ├── ClassMatcher.kt
│   │   └── ScoreCalculator.kt
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   ├── ClassRoutes.kt
│   │   └── MatchingRoutes.kt
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
- `MatchingRoutesTest`: Partner matching, class recommendations, match actions

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
