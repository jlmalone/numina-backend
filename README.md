# Numina Backend

Kotlin/Ktor backend server for the Numina group fitness social platform.

## Overview

Numina is a group fitness social platform that combines elements of ClassPass, Meetup, and community-focused fitness. Think of it as a connection-oriented platform (not a dating app!) that matches people to each other and to fitness classes/events, fostering community, accountability, and shared fitness journeys.

This backend provides:

- User authentication and profile management
- Fitness class catalog and discovery
- **Real-time messaging system with WebSocket support**
- User matching algorithms (coming soon)
- Ratings and feedback system (coming soon)

## Technology Stack

- **Language**: Kotlin JVM 2.0+
- **Framework**: Ktor 3.0+
- **Database**: PostgreSQL for production, H2 in-memory for development
- **DI**: Koin 4.0
- **Authentication**: JWT tokens with refresh token support
- **ORM**: Exposed with kotlinx.datetime
- **Serialization**: kotlinx.serialization
- **Real-time**: WebSockets for live messaging
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

### Messaging Endpoints

All messaging endpoints require authentication (Bearer token).

#### Send Message

```bash
POST /api/v1/messages/send
Authorization: Bearer <token>
Content-Type: application/json

{
  "recipientId": 2,
  "content": "Hey! Want to join me for yoga tomorrow?"
}

# Response: 201 Created
{
  "message": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "conversationId": "conv-123",
    "senderId": 1,
    "content": "Hey! Want to join me for yoga tomorrow?",
    "sentAt": "2025-01-15T10:30:00Z",
    "deliveredAt": null,
    "readAt": null,
    "deleted": false
  },
  "conversationId": "conv-123"
}
```

#### Get Conversations

```bash
GET /api/v1/messages/conversations?page=1&pageSize=20
Authorization: Bearer <token>

# Response: 200 OK
{
  "conversations": [
    {
      "id": "conv-123",
      "participant1Id": 1,
      "participant2Id": 2,
      "lastMessageAt": "2025-01-15T10:30:00Z",
      "lastMessage": "Hey! Want to join me for yoga tomorrow?",
      "unreadCount": 3,
      "otherParticipant": {
        "id": 2,
        "name": "Jane Smith",
        "email": "jane@example.com"
      }
    }
  ],
  "total": 5,
  "page": 1,
  "pageSize": 20
}
```

#### Get Messages in Conversation

```bash
GET /api/v1/messages/conversations/{conversationId}?page=1&pageSize=50
Authorization: Bearer <token>

# Response: 200 OK
{
  "messages": [
    {
      "id": "msg-1",
      "conversationId": "conv-123",
      "senderId": 1,
      "content": "See you at 9am!",
      "sentAt": "2025-01-15T10:45:00Z",
      "deliveredAt": "2025-01-15T10:45:01Z",
      "readAt": "2025-01-15T10:46:00Z",
      "deleted": false
    }
  ],
  "total": 15,
  "page": 1,
  "pageSize": 50
}
```

#### Mark Conversation as Read

```bash
POST /api/v1/messages/conversations/{conversationId}/mark-read
Authorization: Bearer <token>

# Response: 200 OK
{
  "success": true,
  "conversationId": "conv-123"
}
```

#### Delete Message

```bash
DELETE /api/v1/messages/{messageId}
Authorization: Bearer <token>

# Response: 200 OK
{
  "success": true,
  "messageId": "msg-1"
}
```

#### Block User

```bash
POST /api/v1/messages/block/{userId}
Authorization: Bearer <token>

# Response: 200 OK
{
  "blockedUserId": 2,
  "success": true
}
```

#### Unblock User

```bash
DELETE /api/v1/messages/block/{userId}
Authorization: Bearer <token>

# Response: 200 OK
{
  "success": true,
  "unblockedUserId": 2
}
```

#### Report Message

```bash
POST /api/v1/messages/report/{messageId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Inappropriate content"
}

# Response: 201 Created
{
  "reportId": "report-123",
  "status": "PENDING"
}
```

#### Get Unread Message Count

```bash
GET /api/v1/messages/unread-count
Authorization: Bearer <token>

# Response: 200 OK
{
  "count": 7
}
```

### WebSocket Connection (Real-Time Messaging)

Connect to the WebSocket endpoint for real-time message delivery:

```javascript
// Example WebSocket connection
const token = "your-jwt-token";
const ws = new WebSocket(`ws://localhost:8080/api/v1/ws/messages?token=${token}`);

ws.onopen = () => {
  console.log('Connected to messaging WebSocket');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);

  // Handle different message types
  switch (message.type) {
    case 'NewMessage':
      console.log('New message received:', message.message);
      break;
    case 'MessageDelivered':
      console.log('Message delivered:', message.messageId);
      break;
    case 'MessageRead':
      console.log('Message read:', message.messageId);
      break;
    case 'TypingIndicator':
      console.log('User typing:', message.userId, message.typing);
      break;
    case 'UserOnlineStatus':
      console.log('User status:', message.userId, message.online);
      break;
  }
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = () => {
  console.log('WebSocket connection closed');
};
```

**WebSocket Message Types:**
- `NewMessage`: Real-time notification when a new message arrives
- `MessageDelivered`: Confirmation that a message was delivered
- `MessageRead`: Notification that a message was read
- `TypingIndicator`: Shows when another user is typing
- `UserOnlineStatus`: Indicates when users go online/offline

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

### Messages Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| conversation_id | VARCHAR(36) | Foreign key to Conversations |
| sender_id | INTEGER | Foreign key to Users |
| content | TEXT | Message content (max 5000 chars) |
| sent_at | TIMESTAMP | Message sent timestamp |
| delivered_at | TIMESTAMP | Delivery timestamp (nullable) |
| read_at | TIMESTAMP | Read timestamp (nullable) |
| deleted | BOOLEAN | Soft delete flag |
| created_at | TIMESTAMP | Record creation time |

### Conversations Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| participant_1_id | INTEGER | Foreign key to Users |
| participant_2_id | INTEGER | Foreign key to Users |
| last_message_at | TIMESTAMP | Timestamp of last message |
| created_at | TIMESTAMP | Conversation creation time |
| archived_by_user_1 | BOOLEAN | Archived flag for participant 1 |
| archived_by_user_2 | BOOLEAN | Archived flag for participant 2 |

### BlockedUsers Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| blocker_id | INTEGER | Foreign key to Users (who blocked) |
| blocked_id | INTEGER | Foreign key to Users (who was blocked) |
| created_at | TIMESTAMP | Block creation time |

### MessageReports Table
| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(36) | Primary key (UUID) |
| message_id | VARCHAR(36) | Foreign key to Messages |
| reporter_id | INTEGER | Foreign key to Users |
| reason | VARCHAR(500) | Report reason |
| status | VARCHAR(20) | Report status (PENDING, REVIEWED, RESOLVED) |
| created_at | TIMESTAMP | Report creation time |

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
│   │   ├── WebSockets.kt      # WebSocket configuration
│   │   └── Koin.kt
│   ├── domain/                 # Domain models
│   │   ├── User.kt
│   │   ├── UserProfile.kt
│   │   └── FitnessClass.kt
│   ├── messaging/              # Messaging feature
│   │   ├── Models.kt           # Messaging domain models & DTOs
│   │   ├── MessagingService.kt # Business logic
│   │   └── WebSocketManager.kt # WebSocket connection manager
│   ├── data/                   # Database layer
│   │   ├── tables/             # Exposed table definitions
│   │   │   ├── Users.kt
│   │   │   ├── MessagingTables.kt
│   │   │   └── ...
│   │   └── repositories/       # Data access repositories
│   │       ├── MessageRepository.kt
│   │       ├── ConversationRepository.kt
│   │       ├── BlockedUserRepository.kt
│   │       └── ...
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   ├── ClassRoutes.kt
│   │   └── MessagingRoutes.kt  # Messaging endpoints
│   └── auth/                   # JWT and auth logic
│       └── JwtConfig.kt
├── src/main/resources/
│   ├── application.yaml        # Application config
│   └── logback.xml             # Logging config
├── src/test/kotlin/
│   └── com/numina/             # Integration tests
│       ├── messaging/
│       │   └── MessagingServiceTest.kt
│       └── routes/
│           └── MessagingRoutesTest.kt
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

The project includes comprehensive tests for all major features:

**Unit Tests:**
- `MessagingServiceTest`: Message sending, blocking, reporting

**Integration Tests:**
- `AuthRoutesTest`: Registration, login, token refresh
- `UserProfileRoutesTest`: Profile retrieval and updates
- `ClassRoutesTest`: Class creation, listing, and filtering
- `MessagingRoutesTest`: Messaging endpoints, conversations, WebSocket

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
