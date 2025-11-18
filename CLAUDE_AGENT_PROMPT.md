# 🤖 CLAUDE CODE WEB AGENT PROMPT

> **IMPORTANT**: Before starting, check if `.agent-completed` file exists in the root directory.
> If it exists, respond: "✅ This task has already been completed. See README.md for details."
> **When finished**, create `.agent-completed` file with timestamp and summary.

---

# TASK: Scaffold Numina Backend API Server

## Repository Purpose
You are working on `numina-backend`, the Kotlin/Ktor backend server for Numina - a group fitness social platform that matches people to each other and to fitness classes/events. Think ClassPass + Meetup with a focus on connections, community, and accountability (not a dating app, but connection-oriented).

## Current State
Repository initialized with basic README. Starting from scratch.

## Your Task
Create a production-ready Kotlin/Ktor backend with the following features:

### Core Requirements

1. **Project Structure**
   - Gradle build (Kotlin DSL)
   - Ktor 3.0+ with modern architecture
   - Koin for dependency injection
   - Exposed ORM with PostgreSQL (or SQLite for dev)
   - JWT-based authentication
   - CORS configuration for web/mobile clients

2. **Authentication Module**
   - POST `/api/v1/auth/register` - User registration
   - POST `/api/v1/auth/login` - Login with JWT token
   - POST `/api/v1/auth/refresh` - Token refresh
   - POST `/api/v1/auth/logout` - Logout
   - Bcrypt password hashing
   - JWT token generation and validation

3. **User Profile Module**
   - GET `/api/v1/users/me` - Get current user profile
   - PUT `/api/v1/users/me` - Update profile
   - GET `/api/v1/users/{id}` - Get public profile
   - Profile fields: name, bio, location (lat/long), fitness interests, fitness level (1-10), availability schedule, profile photo URL
   - Privacy settings (public/friends-only fields)

4. **Class Catalog Module**
   - GET `/api/v1/classes` - List classes (with filters: location, date, type, price range)
   - GET `/api/v1/classes/{id}` - Get class details
   - POST `/api/v1/classes` - Admin: Create class entry
   - PUT `/api/v1/classes/{id}` - Admin: Update class
   - Class fields: name, description, datetime, location (lat/long), trainer, intensity (1-10), price, external_booking_url, provider_id, capacity, tags

5. **Database Schema**
   - Users table: id, email, password_hash, created_at, updated_at
   - UserProfiles table: user_id (FK), name, bio, location_lat, location_long, fitness_interests (JSON), fitness_level, availability (JSON), photo_url
   - Classes table: id, name, description, datetime, location_lat, location_long, trainer, intensity, price, booking_url, provider_id, capacity, tags (JSON), created_at
   - (Future: Matches, Messages, Ratings tables - add TODO comments)

### Technical Constraints

- **Language**: Kotlin JVM
- **Framework**: Ktor 3.0+
- **Database**: PostgreSQL for production, SQLite for local dev (configurable)
- **DI**: Koin
- **Serialization**: kotlinx.serialization
- **Testing**: Ktor test framework with in-memory database

### File Structure
```
numina-backend/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/kotlin/com/numina/
│   ├── Application.kt          # Main entry point
│   ├── plugins/                # Ktor plugins config
│   │   ├── Routing.kt
│   │   ├── Security.kt
│   │   ├── Serialization.kt
│   │   └── Database.kt
│   ├── domain/                 # Domain models
│   │   ├── User.kt
│   │   ├── UserProfile.kt
│   │   └── FitnessClass.kt
│   ├── data/                   # Database tables & repos
│   │   ├── tables/
│   │   └── repositories/
│   ├── routes/                 # API route handlers
│   │   ├── AuthRoutes.kt
│   │   ├── UserRoutes.kt
│   │   └── ClassRoutes.kt
│   └── auth/                   # JWT and auth logic
│       └── JwtConfig.kt
├── src/main/resources/
│   └── application.conf
├── src/test/kotlin/
└── README.md
```

### Acceptance Criteria

1. ✅ Server starts successfully on port 8080 (HTTP) with configurable HTTPS
2. ✅ All authentication endpoints work with proper JWT flow
3. ✅ User can register, login, and update their profile
4. ✅ Admin can create/update classes via API
5. ✅ Clients can query classes with filters (location radius, date range, type)
6. ✅ Database migrations set up properly
7. ✅ Basic integration tests for each module
8. ✅ README with setup instructions, API documentation, and example requests
9. ✅ Environment-based configuration (dev/prod)
10. ✅ Proper error handling and validation

### Deliverables

- Complete working Ktor backend
- Database schema with migrations
- API documentation in README
- Integration tests for critical paths
- Docker Compose file for local dev (PostgreSQL + backend)
- TODO.md file listing next steps: matching engine, messaging, ratings
- `.gitignore` file (exclude build/, .gradle/, *.db, config.properties, etc.)

### How to Report Back

1. **Update README.md** with:
   - Quick start instructions
   - API endpoint documentation with curl examples
   - Database schema diagram (ASCII or markdown table)
   - Configuration guide
   - Testing instructions
   - Next steps and known limitations

2. **Create TODO.md** with prioritized next features

3. **Create `.agent-completed` file** with content:
   ```
   Completed: [timestamp]
   Summary: Numina backend scaffolded successfully
   Features: Auth, User Profiles, Class Catalog
   Status: All acceptance criteria met
   Next: See TODO.md
   ```

4. **Commit and push** all changes with message:
   ```
   feat: Complete backend scaffolding with auth and class catalog

   🤖 Generated with [Claude Code](https://claude.com/claude-code)

   Co-Authored-By: Claude <noreply@anthropic.com>
   ```
