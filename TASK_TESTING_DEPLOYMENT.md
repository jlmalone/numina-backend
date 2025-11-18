# Task: Add Testing & Deployment Setup

> **IMPORTANT**: Check for `.task-testing-deployment-completed` before starting.
> If it exists, respond: "✅ This task has already been implemented."
> **When finished**, create `.task-testing-deployment-completed` file.

## Overview
Add comprehensive E2E testing suite, Docker containerization, and production deployment configuration to the Numina backend.

## Requirements

### 1. E2E Testing Suite

Create `src/test/kotlin/com/numina/e2e/` directory with:

#### Test Infrastructure
**File**: `src/test/kotlin/com/numina/e2e/E2ETestBase.kt`
```kotlin
package com.numina.e2e

import com.numina.Application
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class E2ETestBase {
    protected lateinit var client: HttpClient
    protected var authToken: String? = null

    @BeforeEach
    fun setup() = testApplication {
        application { Application().module() }

        client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    @AfterEach
    fun teardown() {
        client.close()
    }

    protected suspend fun login(email: String, password: String): String {
        // Implementation for login and token retrieval
    }

    protected fun HttpRequestBuilder.withAuth() {
        authToken?.let { header("Authorization", "Bearer $it") }
    }
}
```

#### Test Suites
**File**: `src/test/kotlin/com/numina/e2e/AuthFlowTest.kt`
```kotlin
package com.numina.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthFlowTest : E2ETestBase() {
    @Test
    fun `complete authentication flow`() = runBlocking {
        // Register
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"test123","name":"Test User"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        // Login
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"test123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        // Get profile
        authToken = extractToken(loginResponse.bodyAsText())
        val profileResponse = client.get("/api/users/me") {
            withAuth()
        }
        assertEquals(HttpStatusCode.OK, profileResponse.status)
    }

    @Test
    fun `invalid credentials fail`() = runBlocking {
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"wrong"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
```

**File**: `src/test/kotlin/com/numina/e2e/ClassesFlowTest.kt`
```kotlin
package com.numina.e2e

import org.junit.jupiter.api.Test

class ClassesFlowTest : E2ETestBase() {
    @Test
    fun `search and view classes flow`() {
        // Test class search, filtering, details
    }

    @Test
    fun `bookmark class flow`() {
        // Test bookmarking and retrieving bookmarks
    }
}
```

**File**: `src/test/kotlin/com/numina/e2e/MessagingFlowTest.kt`
```kotlin
package com.numina.e2e

import org.junit.jupiter.api.Test

class MessagingFlowTest : E2ETestBase() {
    @Test
    fun `complete messaging flow`() {
        // Test creating conversation, sending messages, real-time delivery
    }
}
```

**File**: `src/test/kotlin/com/numina/e2e/GroupsFlowTest.kt`
```kotlin
package com.numina.e2e

import org.junit.jupiter.api.Test

class GroupsFlowTest : E2ETestBase() {
    @Test
    fun `create group and activity flow`() {
        // Test group creation, joining, creating activities, RSVP
    }
}
```

**File**: `src/test/kotlin/com/numina/e2e/SocialFlowTest.kt`
```kotlin
package com.numina.e2e

import org.junit.jupiter.api.Test

class SocialFlowTest : E2ETestBase() {
    @Test
    fun `follow user and view feed flow`() {
        // Test following, unfollowing, viewing feed
    }
}
```

### 2. Docker Configuration

**File**: `Dockerfile`
```dockerfile
FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle buildFatJar --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV DATABASE_URL=""
ENV JWT_SECRET=""
ENV JWT_ISSUER="numina-backend"
ENV JWT_AUDIENCE="numina-clients"
ENV JWT_REALM="numina"
ENV REDIS_HOST="localhost"
ENV REDIS_PORT="6379"
ENV FCM_CREDENTIALS_PATH=""
ENV ENVIRONMENT="production"

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**File**: `docker-compose.yml`
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: numina
      POSTGRES_USER: numina
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U numina"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  backend:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: "jdbc:postgresql://postgres:5432/numina"
      DB_USER: numina
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      FCM_CREDENTIALS_PATH: /app/config/firebase-credentials.json
      ENVIRONMENT: production
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./config:/app/config
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 3s
      retries: 3
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

**File**: `.dockerignore`
```
.gradle
build
.idea
*.iml
.env
.env.local
*.log
```

### 3. Production Configuration

**File**: `src/main/resources/application-production.conf`
```hocon
ktor {
    deployment {
        port = 8080
        watch = []
    }
    application {
        modules = [ com.numina.ApplicationKt.module ]
    }
}

database {
    url = ${DATABASE_URL}
    user = ${DB_USER}
    password = ${DB_PASSWORD}
    driver = "org.postgresql.Driver"
    maxPoolSize = 20
}

jwt {
    secret = ${JWT_SECRET}
    issuer = ${JWT_ISSUER}
    audience = ${JWT_AUDIENCE}
    realm = ${JWT_REALM}
    expirationMs = 86400000
}

redis {
    host = ${REDIS_HOST}
    port = ${REDIS_PORT}
}

cors {
    allowedHosts = ${?ALLOWED_HOSTS}
}
```

**File**: `src/main/kotlin/com/numina/plugins/Monitoring.kt`
```kotlin
package com.numina.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Application.configureMonitoring() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "healthy",
                "timestamp" to System.currentTimeMillis()
            ))
        }

        get("/ready") {
            // Check database, redis, etc.
            val isReady = checkDatabaseConnection() && checkRedisConnection()
            if (isReady) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "not ready"))
            }
        }
    }
}
```

### 4. Environment Configuration

**File**: `.env.example`
```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/numina
DB_USER=numina
DB_PASSWORD=your_password_here

# JWT
JWT_SECRET=your_jwt_secret_here
JWT_ISSUER=numina-backend
JWT_AUDIENCE=numina-clients
JWT_REALM=numina

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Firebase
FCM_CREDENTIALS_PATH=/path/to/firebase-credentials.json

# CORS
ALLOWED_HOSTS=http://localhost:3000,https://numina.app

# Environment
ENVIRONMENT=production
```

### 5. Deployment Scripts

**File**: `scripts/deploy.sh`
```bash
#!/bin/bash
set -e

echo "🚀 Deploying Numina Backend..."

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

# Build Docker image
echo "📦 Building Docker image..."
docker-compose build

# Run database migrations
echo "🗄️  Running migrations..."
docker-compose run --rm backend gradle flywayMigrate

# Start services
echo "▶️  Starting services..."
docker-compose up -d

# Wait for health check
echo "🏥 Waiting for health check..."
timeout 60 bash -c 'until curl -f http://localhost:8080/health; do sleep 2; done'

echo "✅ Deployment complete!"
```

**File**: `scripts/backup-db.sh`
```bash
#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/numina_$TIMESTAMP.sql"

mkdir -p $BACKUP_DIR

echo "📦 Creating database backup..."
docker-compose exec -T postgres pg_dump -U numina numina > $BACKUP_FILE

echo "✅ Backup created: $BACKUP_FILE"
```

### 6. Update build.gradle.kts

Add testing dependencies:
```kotlin
dependencies {
    // Existing dependencies...

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}
```

### 7. Documentation

**File**: `DEPLOYMENT.md`
```markdown
# Numina Backend Deployment Guide

## Prerequisites
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Firebase project with FCM enabled

## Quick Start

1. **Clone and configure**:
   ```bash
   git clone <repo>
   cd numina-backend
   cp .env.example .env
   # Edit .env with your values
   ```

2. **Deploy with Docker**:
   ```bash
   ./scripts/deploy.sh
   ```

3. **Verify deployment**:
   ```bash
   curl http://localhost:8080/health
   ```

## Manual Deployment

1. **Build**:
   ```bash
   ./gradlew buildFatJar
   ```

2. **Run migrations**:
   ```bash
   ./gradlew flywayMigrate
   ```

3. **Start server**:
   ```bash
   java -jar build/libs/numina-backend-all.jar
   ```

## Production Checklist
- [ ] Set strong JWT_SECRET
- [ ] Configure CORS allowed hosts
- [ ] Set up SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure log aggregation
- [ ] Set up monitoring/alerts
- [ ] Review rate limiting settings

## Monitoring
- Health: `GET /health`
- Readiness: `GET /ready`

## Backup
```bash
./scripts/backup-db.sh
```
```

## Completion Checklist
- [ ] All E2E test files created and passing
- [ ] Docker configuration complete
- [ ] Production config files created
- [ ] Deployment scripts working
- [ ] Documentation complete
- [ ] Health check endpoints working
- [ ] `.task-testing-deployment-completed` file created

## Testing
Run all tests:
```bash
./gradlew test
```

Run E2E tests only:
```bash
./gradlew test --tests "com.numina.e2e.*"
```

## Success Criteria
1. ✅ Comprehensive E2E test suite with >80% coverage
2. ✅ Docker setup working with single command deployment
3. ✅ Production configuration secure and complete
4. ✅ Health check and monitoring endpoints functional
5. ✅ Documentation clear and comprehensive
