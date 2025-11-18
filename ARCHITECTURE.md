# Numina Backend Architecture

## Overview

The Numina Backend is built using a modern, layered architecture following clean code principles and industry best practices. This document outlines the architectural decisions, patterns, and structure of the application.

## Technology Stack

- **Language**: Kotlin 1.9.22 (JVM)
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL (production) / H2 (development)
- **ORM**: Exposed 0.46.0
- **Dependency Injection**: Koin 3.5.3
- **Serialization**: kotlinx.serialization
- **Authentication**: JWT (JSON Web Tokens)
- **Logging**: SLF4J with Logback

## Architecture Layers

### 1. Presentation Layer (Routes)

**Location**: `src/main/kotlin/com/numina/routes/`

The presentation layer handles HTTP requests and responses. It is responsible for:
- Request validation (basic)
- HTTP status code management
- Delegating business logic to the service layer
- Response formatting

**Key Files**:
- `AuthRoutes.kt` - Authentication endpoints
- `UserRoutes.kt` - User profile management
- `ClassRoutes.kt` - Fitness class catalog

**Design Principles**:
- Thin controllers - minimal logic
- Dependency injection via Koin
- Exception handling delegated to StatusPages plugin
- All routes use services (not repositories directly)

### 2. Service Layer

**Location**: `src/main/kotlin/com/numina/services/`

The service layer contains business logic and orchestrates operations. It is responsible for:
- Business logic implementation
- Input validation (comprehensive)
- Transaction orchestration
- Error handling with custom exceptions

**Key Files**:
- `AuthService.kt` - Authentication and authorization logic
- `UserService.kt` - User profile management logic
- `ClassService.kt` - Class catalog and filtering logic

**Design Principles**:
- Single Responsibility Principle
- Separation of concerns
- Comprehensive validation
- Structured logging

### 3. Data Layer (Repositories)

**Location**: `src/main/kotlin/com/numina/data/repositories/`

The data layer handles database operations using Exposed ORM. It is responsible for:
- CRUD operations
- Database queries
- Data mapping (ResultRow to Domain models)

**Key Files**:
- `UserRepository.kt` - User data access
- `UserProfileRepository.kt` - User profile data access
- `ClassRepository.kt` - Class data access
- `RefreshTokenRepository.kt` - Token management

**Design Principles**:
- Repository pattern
- Data mapper pattern
- Transaction management
- Separation from business logic

### 4. Domain Layer

**Location**: `src/main/kotlin/com/numina/domain/`

The domain layer contains domain models and DTOs. It is responsible for:
- Data transfer objects (DTOs)
- Request/Response models
- Domain entities

**Key Files**:
- `User.kt` - User entity and DTOs
- `UserProfile.kt` - Profile entity and DTOs
- `FitnessClass.kt` - Class entity and DTOs

### 5. Common Layer

**Location**: `src/main/kotlin/com/numina/common/`

The common layer contains shared utilities and cross-cutting concerns.

**Subdirectories**:
- `exceptions/` - Custom exception hierarchy
- `models/` - Shared models (API responses, pagination)
- `utils/` - Utility classes (validation, correlation IDs)

## Database Schema

### Tables

1. **Users**
   - `id` (PK, auto-increment)
   - `email` (unique)
   - `password_hash`
   - `created_at`
   - `updated_at`

2. **UserProfiles**
   - `user_id` (PK, FK to Users)
   - `name`
   - `bio`
   - `location_lat`, `location_long`
   - `fitness_interests` (JSONB)
   - `fitness_level`
   - `availability` (JSONB)
   - `photo_url`
   - `privacy_settings` (JSONB)

3. **Classes**
   - `id` (PK, auto-increment)
   - `name`
   - `description`
   - `datetime`
   - `location_lat`, `location_long`
   - `trainer`
   - `intensity`
   - `price`
   - `external_booking_url`
   - `provider_id`
   - `capacity`
   - `tags` (JSONB)
   - `created_at`

4. **RefreshTokens**
   - `id` (PK, auto-increment)
   - `user_id` (FK to Users)
   - `token` (unique)
   - `expires_at`
   - `created_at`
   - `is_revoked`

## Error Handling

### Exception Hierarchy

All custom exceptions extend `ApiException` which includes:
- HTTP status code
- Error message
- Error code (for client identification)
- Optional details map

**Exception Types**:
- `BadRequestException` (400)
- `UnauthorizedException` (401)
- `ForbiddenException` (403)
- `NotFoundException` (404)
- `ConflictException` (409)
- `ValidationException` (422)
- `InternalServerException` (500)
- `ServiceUnavailableException` (503)

### Error Response Format

```json
{
  "success": false,
  "error": {
    "message": "Human-readable error message",
    "errorCode": "MACHINE_READABLE_CODE",
    "details": {
      "field": "specific error details"
    },
    "timestamp": 1640000000000
  }
}
```

## Authentication & Authorization

### JWT Authentication Flow

1. **Registration/Login**:
   - User provides credentials
   - Server validates and creates user
   - Server generates JWT access token (24h expiry)
   - Server generates refresh token (30d expiry)
   - Both tokens returned to client

2. **Authenticated Requests**:
   - Client includes JWT in `Authorization: Bearer <token>` header
   - Server validates JWT signature and expiry
   - Server extracts user ID from JWT claims
   - Request proceeds with user context

3. **Token Refresh**:
   - Client sends refresh token
   - Server validates refresh token (not expired, not revoked)
   - Server revokes old refresh token
   - Server generates new JWT and refresh token pair

4. **Logout**:
   - Server revokes all user's refresh tokens
   - Client discards tokens

### JWT Claims

- `userId` - User ID
- `email` - User email
- `aud` - Audience (numina-users)
- `iss` - Issuer (numina-backend)
- `exp` - Expiration timestamp

## Logging & Monitoring

### Correlation IDs

Every request is assigned a correlation ID for distributed tracing:
- `X-Correlation-ID` - Passed from client or generated
- `X-Request-ID` - Unique ID for each request

These IDs are:
- Added to all log messages via MDC
- Included in error responses
- Returned in response headers

### Log Levels

- `ERROR` - Application errors, exceptions
- `WARN` - Warnings, potential issues
- `INFO` - Request/response logging, business events
- `DEBUG` - Detailed debugging information

### Health Checks

Three health check endpoints:
- `/health` - Overall health with dependency checks
- `/health/ready` - Readiness probe (Kubernetes)
- `/health/live` - Liveness probe (Kubernetes)

Health checks include:
- Database connectivity
- Memory usage
- Response time metrics

## Pagination

List endpoints support pagination via query parameters:
- `page` - Page number (default: 1)
- `pageSize` - Items per page (default: 20, max: 100)

### Pagination Response

```json
{
  "items": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Validation

### Input Validation

Multi-layered validation approach:
1. **Route level** - Basic type checking, parameter parsing
2. **Service level** - Business logic validation
3. **Repository level** - Database constraints

### Validation Utilities

`ValidationUtils` provides reusable validation functions:
- Email format validation
- Password strength validation
- Range validation (e.g., fitness level 1-10)
- Required field validation
- Latitude/longitude validation

## Configuration

### Environment-Based Config

Configuration managed via `application.yaml`:
- Development settings (H2 database)
- Production settings (PostgreSQL)
- Environment variables for secrets

### Key Configuration Points

- Database connection
- JWT secret
- Server port
- CORS settings
- Log levels

## Dependency Injection

### Koin Configuration

All dependencies registered in `appModule`:
- Repositories (singletons)
- Services (singletons)
- Routes use constructor injection

**Benefits**:
- Testability (easy mocking)
- Loose coupling
- Centralized configuration
- Lifecycle management

## Security Considerations

### Implemented

- ✅ JWT-based authentication
- ✅ Bcrypt password hashing
- ✅ Refresh token rotation
- ✅ CORS configuration
- ✅ Input validation
- ✅ SQL injection prevention (ORM)
- ✅ Privacy controls (user profiles)

### To Implement (see TODO.md)

- ⏳ Role-Based Access Control (RBAC)
- ⏳ Rate limiting
- ⏳ Security headers (CSP, HSTS, etc.)
- ⏳ Request throttling
- ⏳ IP-based restrictions
- ⏳ 2FA support

## Testing Strategy

### Current Tests

- Integration tests for all major endpoints
- In-memory H2 database for test isolation
- Ktor test framework

### Future Testing Needs

- Unit tests for service layer
- Repository layer tests
- Performance tests
- Contract tests
- E2E tests

## Performance Considerations

### Current Implementation

- Connection pooling (Exposed)
- Efficient JSON serialization
- In-memory H2 for development

### Future Optimizations

- Redis caching layer
- Database query optimization
- Database pagination (vs in-memory)
- CDN for static assets
- Horizontal scaling with load balancer

## Deployment

### Local Development

```bash
./gradlew run
```

### Docker Deployment

```bash
docker-compose up
```

### Production Considerations

- Use PostgreSQL (not H2)
- Set strong JWT_SECRET environment variable
- Enable HTTPS/TLS
- Configure CORS for specific domains
- Set up monitoring and alerting
- Implement backup strategy
- Configure log aggregation

## Future Architecture Enhancements

1. **Event-Driven Architecture**
   - Message queue (RabbitMQ/Kafka)
   - Async event processing
   - Notification service

2. **Microservices**
   - Split into domain-specific services
   - API Gateway
   - Service mesh

3. **Caching Layer**
   - Redis for session management
   - Cache frequently accessed data
   - Cache invalidation strategy

4. **Search Engine**
   - Elasticsearch for full-text search
   - Advanced filtering
   - Geospatial queries

5. **File Storage**
   - S3-compatible storage
   - CDN integration
   - Image processing pipeline

## Architecture Decision Records (ADRs)

### ADR-001: Service Layer Pattern

**Decision**: Introduce a service layer between routes and repositories.

**Rationale**:
- Separation of concerns
- Business logic isolation
- Improved testability
- Better code organization

**Consequences**:
- More files and layers
- Slight increase in complexity
- Significant improvement in maintainability

### ADR-002: Custom Exception Hierarchy

**Decision**: Create typed exceptions extending ApiException.

**Rationale**:
- Type-safe error handling
- Consistent error responses
- Better debugging
- Clear error contracts

**Consequences**:
- More exception classes
- Easier error handling in routes
- Consistent API responses

### ADR-003: Correlation ID Tracking

**Decision**: Implement correlation IDs for all requests.

**Rationale**:
- Distributed tracing
- Debugging complex flows
- Log aggregation
- Request tracking

**Consequences**:
- Additional header processing
- Improved observability
- Better debugging

### ADR-004: Pagination at Service Layer

**Decision**: Implement pagination in service layer, not database.

**Rationale**:
- Simpler initial implementation
- Location filtering requires in-memory processing
- Sufficient for initial scale

**Consequences**:
- Performance limitations at scale
- Future migration to database pagination needed
- Trade-off: simplicity vs performance

## References

- [Ktor Documentation](https://ktor.io/)
- [Exposed Documentation](https://github.com/JetBrains/Exposed)
- [Koin Documentation](https://insert-koin.io/)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
