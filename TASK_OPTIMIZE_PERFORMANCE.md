# TASK: Backend Performance Optimization

> **IMPORTANT**: Check for `.task-performance-completed` before starting.
> **When finished**, create `.task-performance-completed` file.

## 🎯 OBJECTIVE

Optimize backend performance with caching, database query optimization, and rate limiting.

## 📋 REQUIREMENTS

### 1. Add Redis Caching Layer

**What to cache**:
- User profiles (30 min TTL)
- Class listings (15 min TTL)
- Match results (1 hour TTL)
- Feed data (5 min TTL)
- Rating aggregates (1 hour TTL)

**Implementation**:
- Add Redis dependency to build.gradle.kts
- Create `CacheService.kt` with get/set/invalidate methods
- Add cache interceptor for common queries
- Cache-aside pattern for expensive queries

### 2. Database Query Optimization

**Optimize**:
- Add indexes on frequently queried columns
- Optimize N+1 queries (use eager loading)
- Add database connection pooling config
- Optimize join queries in groups/social features
- Add query result pagination everywhere

**Specific optimizations**:
- Index on `users.email`, `users.created_at`
- Index on `fitness_classes.datetime`, `fitness_classes.provider`
- Index on `messages.conversation_id`, `messages.sent_at`
- Index on `follows.follower_id`, `follows.following_id`
- Composite index on `bookings(user_id, class_datetime)`

### 3. Rate Limiting

**Add rate limits**:
- `/api/v1/auth/*` - 5 requests/min per IP
- `/api/v1/messages/send` - 20 messages/min per user
- `/api/v1/social/activity` - 10 posts/min per user
- `/api/v1/reviews/*` - 5 reviews/hour per user
- Default: 100 requests/min per user

**Implementation**:
- Use Ktor rate limiting plugin or custom middleware
- Redis-backed rate limiter for distributed systems
- Return 429 Too Many Requests with Retry-After header

### 4. Response Compression

- Enable Gzip compression for responses > 1KB
- Ktor Compression plugin configuration

### 5. Background Job Optimization

**Optimize**:
- Batch notification sending
- Aggregate rating calculations (run every 15 min instead of real-time)
- Feed generation (cache and regenerate every 5 min)
- Scheduled jobs use job queue instead of cron

### 6. API Response Optimization

**Improvements**:
- Add field selection (`?fields=id,name,email`)
- Reduce default page sizes (25 -> 10 for heavy endpoints)
- Add ETag support for conditional requests
- Add Last-Modified headers

### 7. Monitoring & Logging

**Add**:
- Request duration logging
- Slow query logging (> 1s)
- Cache hit/miss metrics
- Error rate tracking
- Prometheus metrics endpoint

## 📦 DEPENDENCIES TO ADD

```kotlin
// build.gradle.kts
implementation("io.lettuce:lettuce-core:6.3.0") // Redis client
implementation("io.ktor:ktor-server-compression:$ktor_version")
implementation("io.ktor:ktor-server-caching-headers:$ktor_version")
implementation("io.ktor:ktor-server-rate-limit:$ktor_version")
implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
```

## ✅ ACCEPTANCE CRITERIA

- [ ] Redis caching layer implemented
- [ ] All database indexes created
- [ ] Rate limiting active on all endpoints
- [ ] Response compression enabled
- [ ] Query performance improved (measure before/after)
- [ ] Cache hit rate > 70% for cached endpoints
- [ ] API response times < 200ms for 95th percentile
- [ ] Monitoring metrics exposed
- [ ] Documentation updated

## 📝 DELIVERABLES

- CacheService implementation
- Database migration with indexes
- Rate limiting middleware
- Compression configuration
- Monitoring setup
- Performance benchmarks
- Updated README

## 🚀 COMPLETION

1. Test: `./gradlew test`
2. Benchmark: Run load tests
3. Create `.task-performance-completed`
4. Commit: "Optimize backend performance (caching, indexes, rate limiting)"
5. Push: `git push -u origin claude/optimize-performance`

---

**Est. Time**: 60-75 min | **Priority**: HIGH
