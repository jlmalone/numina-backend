# Numina Backend

Kotlin/Ktor backend server for the Numina group fitness social platform.

## Overview

Numina is a group fitness social platform that combines elements of ClassPass, Meetup, and community-focused fitness. Think of it as a connection-oriented platform (not a dating app!) that matches people to each other and to fitness classes/events, fostering community, accountability, and shared fitness journeys.

This backend provides:

- User authentication and profile management
- Fitness class catalog and discovery
- User matching algorithms (user-to-user and user-to-class)
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

#### Get Partner Matches

Find workout partners based on compatibility scoring.

```bash
GET /api/v1/matches/partners
Authorization: Bearer <token>

# Optional query parameters:
# - limit: Max results (default: 20)
# - minScore: Minimum match score 0-100 (default: 60)
# - radiusKm: Search radius in kilometers (default: 10.0)

# Example:
GET /api/v1/matches/partners?limit=10&minScore=70&radiusKm=15

# Response: 200 OK
[
  {
    "userId": 42,
    "profile": {
      "userId": 42,
      "name": "Sarah Johnson",
      "bio": "Love morning runs and yoga",
      "fitnessInterests": ["running", "yoga"],
      "fitnessLevel": 7,
      "photoUrl": "https://example.com/sarah.jpg"
    },
    "matchScore": 85,
    "matchReasons": [
      "Similar fitness level (7 vs 7)",
      "2 shared interests: running, yoga",
      "3.2 km away"
    ],
    "sharedInterests": ["running", "yoga"],
    "distanceKm": 3.2
  }
]
```

**Matching Algorithm (User-to-User):**

The partner matching algorithm uses weighted scoring across 5 factors:

- **Fitness Level Similarity (20%)**: Matches users with similar fitness levels to ensure compatible workout intensity
- **Shared Interests (30%)**: Jaccard similarity of fitness interests (yoga, running, cycling, etc.)
- **Geographic Proximity (25%)**: Calculated using Haversine formula for accurate distance
- **Schedule Compatibility (20%)**: Overlapping availability in weekly schedules
- **Past Interactions (5%)**: Historical match actions to improve recommendations over time

Match scores range from 0-100, with 60+ considered a good match and 80+ considered excellent.

#### Get Class Recommendations

Find fitness classes that match user preferences and profile.

```bash
GET /api/v1/matches/classes
Authorization: Bearer <token>

# Optional query parameters:
# - limit: Max results (default: 20)
# - minScore: Minimum match score 0-100 (default: 50)
# - startDate: Filter classes after this date (ISO 8601)
# - endDate: Filter classes before this date (ISO 8601)

# Example:
GET /api/v1/matches/classes?limit=10&minScore=60&startDate=2025-01-20T00:00:00Z

# Response: 200 OK
[
  {
    "classId": 15,
    "classDetails": {
      "id": 15,
      "name": "Vinyasa Flow Yoga",
      "description": "Dynamic yoga flow",
      "datetime": "2025-01-20T09:00:00Z",
      "locationLat": 40.7128,
      "locationLong": -74.0060,
      "trainer": "Maya Patel",
      "intensity": 6,
      "price": 28.0,
      "capacity": 15,
      "tags": ["yoga", "intermediate", "morning"]
    },
    "matchScore": 78,
    "matchReasons": [
      "Matches interest: yoga",
      "Appropriate intensity level",
      "Fits your morning schedule",
      "Only 2.1 km away"
    ],
    "estimatedFit": "good"
  }
]
```

**Matching Algorithm (User-to-Class):**

The class matching algorithm uses weighted scoring across 5 factors:

- **Fitness Interests Match (35%)**: How well class type aligns with user's stated interests
- **Appropriate Intensity (25%)**: Classes within ±2 levels of user's fitness level
- **Schedule Fit (20%)**: Class time matches user's availability preferences
- **Location Convenience (15%)**: Geographic proximity using Haversine distance
- **Price Range (5%)**: Affordability based on historical user preferences

Match scores range from 0-100:
- **80-100 (perfect)**: Highly recommended, strong match across all factors
- **60-79 (good)**: Solid match, worth considering
- **50-59 (okay)**: Moderate match, may have some tradeoffs
- **<50**: Not shown unless minScore lowered

#### Get Mutual Matches

List all mutual matches where both users have liked each other.

```bash
GET /api/v1/matches/mutual
Authorization: Bearer <token>

# Response: 200 OK
[
  {
    "userId": 89,
    "profile": {
      "userId": 89,
      "name": "Mike Chen",
      "bio": "CrossFit enthusiast",
      "fitnessInterests": ["crossfit", "weightlifting"],
      "fitnessLevel": 8,
      "photoUrl": "https://example.com/mike.jpg"
    },
    "matchScore": 76,
    "matchedAt": "2025-01-18T14:30:00Z"
  }
]
```

#### Record Match Action

Like, pass, or super-like another user. Creates mutual match if both users have liked each other.

```bash
POST /api/v1/matches/action
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetUserId": 42,
  "action": "LIKE"  # Options: "LIKE", "PASS", "SUPER_LIKE"
}

# Response: 200 OK
{
  "mutual": true,
  "match": {
    "userId": 42,
    "profile": {
      "userId": 42,
      "name": "Sarah Johnson",
      "bio": "Love morning runs and yoga",
      "fitnessInterests": ["running", "yoga"],
      "fitnessLevel": 7,
      "photoUrl": "https://example.com/sarah.jpg"
    },
    "matchScore": 85,
    "matchReasons": [
      "Similar fitness level",
      "2 shared interests",
      "3.2 km away"
    ],
    "sharedInterests": ["running", "yoga"],
    "distanceKm": 3.2
  }
}
```

Match actions are recorded and used to:
- Detect mutual matches for connection
- Improve future recommendations through behavioral learning
- Filter out previously passed users from future recommendations

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
| id | UUID | Primary key |
| user_id | INTEGER | Foreign key to Users (initiating user) |
| target_user_id | INTEGER | Foreign key to Users (target user) |
| action | VARCHAR(20) | LIKE, PASS, or SUPER_LIKE |
| created_at | TIMESTAMP | Action timestamp |

Unique constraint on (user_id, target_user_id).

### MutualMatches Table
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| user1_id | INTEGER | Foreign key to Users (smaller ID) |
| user2_id | INTEGER | Foreign key to Users (larger ID) |
| match_score | INTEGER | Compatibility score (0-100) |
| matched_at | TIMESTAMP | When mutual match occurred |

Canonical ordering ensures user1_id < user2_id to prevent duplicates.

### MatchPreferences Table
| Column | Type | Description |
|--------|------|-------------|
| user_id | INTEGER | Foreign key to Users |
| max_distance_km | DOUBLE | Maximum search radius |
| min_fitness_level | INTEGER | Minimum partner fitness level |
| max_fitness_level | INTEGER | Maximum partner fitness level |
| updated_at | TIMESTAMP | Last update time |

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
│   │   ├── Koin.kt
│   │   ├── ErrorHandling.kt
│   │   ├── Monitoring.kt
│   │   └── Health.kt
│   ├── domain/                 # Domain models
│   │   ├── User.kt
│   │   ├── UserProfile.kt
│   │   ├── FitnessClass.kt
│   │   └── Matching.kt         # Match models
│   ├── data/                   # Database layer
│   │   ├── tables/             # Exposed table definitions
│   │   │   ├── UserTables.kt
│   │   │   ├── ClassTables.kt
│   │   │   └── MatchTables.kt  # Matching tables
│   │   └── repositories/       # Data access repositories
│   │       ├── UserRepository.kt
│   │       ├── UserProfileRepository.kt
│   │       ├── ClassRepository.kt
│   │       ├── RefreshTokenRepository.kt
│   │       └── MatchRepository.kt
│   ├── services/               # Business logic layer
│   │   ├── AuthService.kt
│   │   ├── UserService.kt
│   │   ├── ClassService.kt
│   │   ├── MatchingService.kt
│   │   └── matching/           # Matching algorithms
│   │       ├── UserMatcher.kt
│   │       └── ClassMatcher.kt
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   ├── ClassRoutes.kt
│   │   └── MatchRoutes.kt
│   ├── common/                 # Shared utilities
│   │   ├── exceptions/
│   │   │   └── ApiExceptions.kt
│   │   └── utils/
│   │       ├── ValidationUtils.kt
│   │       └── ScoreCalculator.kt
│   └── auth/                   # JWT and auth logic
│       └── JwtConfig.kt
├── src/main/resources/
│   ├── application.yaml        # Application config
│   └── logback.xml             # Logging config
├── src/test/kotlin/
│   └── com/numina/             # Integration tests
│       ├── AuthRoutesTest.kt
│       ├── UserProfileRoutesTest.kt
│       ├── ClassRoutesTest.kt
│       └── MatchingRoutesTest.kt
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
- `MatchingRoutesTest`: Partner matching, class matching, mutual matches, match actions

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
