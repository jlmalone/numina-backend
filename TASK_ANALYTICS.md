# Task: Add Analytics & Insights System

> **IMPORTANT**: Check for `.task-analytics-completed` before starting.
> If it exists, respond: "✅ This task has already been implemented."
> **When finished**, create `.task-analytics-completed` file.

## Overview
Implement comprehensive analytics system to track user engagement, class popularity, trainer performance, and platform health metrics.

## Requirements

### 1. Database Schema

**File**: Create migration `V007__create_analytics_tables.sql`

```sql
-- User analytics tracking
CREATE TABLE user_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    classes_attended INT DEFAULT 0,
    classes_cancelled INT DEFAULT 0,
    profile_views INT DEFAULT 0,
    search_queries INT DEFAULT 0,
    messages_sent INT DEFAULT 0,
    connections_made INT DEFAULT 0,
    time_spent_minutes INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, date)
);

CREATE INDEX idx_user_analytics_user_date ON user_analytics(user_id, date DESC);
CREATE INDEX idx_user_analytics_date ON user_analytics(date DESC);

-- Class analytics
CREATE TABLE class_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    views INT DEFAULT 0,
    bookings INT DEFAULT 0,
    cancellations INT DEFAULT 0,
    average_rating DECIMAL(3,2),
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(class_id, date)
);

CREATE INDEX idx_class_analytics_class_date ON class_analytics(class_id, date DESC);
CREATE INDEX idx_class_analytics_date ON class_analytics(date DESC);

-- Trainer analytics
CREATE TABLE trainer_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL REFERENCES trainers(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    total_classes INT DEFAULT 0,
    total_attendees INT DEFAULT 0,
    average_rating DECIMAL(3,2),
    total_reviews INT DEFAULT 0,
    revenue_cents BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(trainer_id, date)
);

CREATE INDEX idx_trainer_analytics_trainer_date ON trainer_analytics(trainer_id, date DESC);

-- Platform analytics (system-wide metrics)
CREATE TABLE platform_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL UNIQUE,
    total_users INT DEFAULT 0,
    active_users INT DEFAULT 0,
    new_users INT DEFAULT 0,
    total_classes INT DEFAULT 0,
    total_bookings INT DEFAULT 0,
    total_revenue_cents BIGINT DEFAULT 0,
    average_session_minutes INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_platform_analytics_date ON platform_analytics(date DESC);

-- Search analytics
CREATE TABLE search_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    search_query TEXT NOT NULL,
    filters JSONB,
    results_count INT DEFAULT 0,
    clicked_result_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_analytics_created ON search_analytics(created_at DESC);
CREATE INDEX idx_search_analytics_query ON search_analytics USING GIN (to_tsvector('english', search_query));
```

### 2. Domain Models

**File**: `src/main/kotlin/com/numina/domain/analytics/UserAnalytics.kt`

```kotlin
package com.numina.domain.analytics

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserAnalytics(
    val id: String,
    val userId: String,
    val date: String, // ISO date
    val classesAttended: Int,
    val classesCancelled: Int,
    val profileViews: Int,
    val searchQueries: Int,
    val messagesSent: Int,
    val connectionsMade: Int,
    val timeSpentMinutes: Int,
    val createdAt: String
)

@Serializable
data class ClassAnalytics(
    val id: String,
    val classId: String,
    val date: String,
    val views: Int,
    val bookings: Int,
    val cancellations: Int,
    val averageRating: Double?,
    val totalReviews: Int,
    val createdAt: String
)

@Serializable
data class TrainerAnalytics(
    val id: String,
    val trainerId: String,
    val date: String,
    val totalClasses: Int,
    val totalAttendees: Int,
    val averageRating: Double?,
    val totalReviews: Int,
    val revenueCents: Long,
    val createdAt: String
)

@Serializable
data class PlatformAnalytics(
    val date: String,
    val totalUsers: Int,
    val activeUsers: Int,
    val newUsers: Int,
    val totalClasses: Int,
    val totalBookings: Int,
    val totalRevenueCents: Long,
    val averageSessionMinutes: Int
)

@Serializable
data class SearchQuery(
    val id: String,
    val userId: String?,
    val searchQuery: String,
    val filters: Map<String, String>?,
    val resultsCount: Int,
    val clickedResultId: String?,
    val createdAt: String
)

// Aggregated insights
@Serializable
data class UserInsights(
    val userId: String,
    val period: String, // "week", "month", "year"
    val classesAttended: Int,
    val favoriteClassTypes: List<String>,
    val streakDays: Int,
    val totalPointsEarned: Int,
    val topTrainers: List<TrainerSummary>,
    val activityTrend: List<DayActivity>
)

@Serializable
data class TrainerSummary(
    val trainerId: String,
    val name: String,
    val classesAttended: Int
)

@Serializable
data class DayActivity(
    val date: String,
    val classCount: Int,
    val minutesActive: Int
)

@Serializable
data class ClassInsights(
    val classId: String,
    val period: String,
    val totalViews: Int,
    val totalBookings: Int,
    val conversionRate: Double,
    val averageRating: Double?,
    val popularityTrend: List<DayPopularity>
)

@Serializable
data class DayPopularity(
    val date: String,
    val views: Int,
    val bookings: Int
)
```

### 3. Repository Layer

**File**: `src/main/kotlin/com/numina/data/repository/AnalyticsRepository.kt`

```kotlin
package com.numina.data.repository

import com.numina.domain.analytics.*
import kotlinx.datetime.LocalDate
import java.util.UUID

interface AnalyticsRepository {
    // User analytics
    suspend fun trackUserActivity(
        userId: UUID,
        date: LocalDate,
        classesAttended: Int = 0,
        profileViews: Int = 0,
        searchQueries: Int = 0,
        messagesSent: Int = 0
    )

    suspend fun getUserAnalytics(userId: UUID, startDate: LocalDate, endDate: LocalDate): List<UserAnalytics>
    suspend fun getUserInsights(userId: UUID, period: String): UserInsights

    // Class analytics
    suspend fun trackClassView(classId: UUID, date: LocalDate)
    suspend fun trackClassBooking(classId: UUID, date: LocalDate)
    suspend fun getClassAnalytics(classId: UUID, startDate: LocalDate, endDate: LocalDate): List<ClassAnalytics>
    suspend fun getClassInsights(classId: UUID, period: String): ClassInsights

    // Trainer analytics
    suspend fun getTrainerAnalytics(trainerId: UUID, startDate: LocalDate, endDate: LocalDate): List<TrainerAnalytics>

    // Platform analytics
    suspend fun getPlatformAnalytics(startDate: LocalDate, endDate: LocalDate): List<PlatformAnalytics>
    suspend fun refreshPlatformAnalytics(date: LocalDate)

    // Search analytics
    suspend fun trackSearch(userId: UUID?, query: String, filters: Map<String, String>?, resultsCount: Int)
    suspend fun getPopularSearches(limit: Int = 20): List<SearchQuery>
    suspend fun getTrendingClasses(limit: Int = 10): List<String> // Returns class IDs
}
```

Implement this interface with SQL queries using Exposed.

### 4. API Routes

**File**: `src/main/kotlin/com/numina/routes/AnalyticsRoutes.kt`

```kotlin
// User analytics
GET /analytics/users/me?period=week|month|year
    - Returns UserInsights for current user
    - Authentication required

GET /analytics/users/{userId}?start={date}&end={date}
    - Returns UserAnalytics time series
    - Admin only

// Class analytics
GET /analytics/classes/{classId}?period=week|month|year
    - Returns ClassInsights
    - Public or provider-only

// Trainer analytics
GET /analytics/trainers/{trainerId}?start={date}&end={date}
    - Returns TrainerAnalytics
    - Trainer or admin only

// Platform analytics (admin only)
GET /analytics/platform?start={date}&end={date}
    - Returns PlatformAnalytics
    - Admin only

GET /analytics/searches/popular
    - Returns popular search queries
    - Admin only

GET /analytics/classes/trending
    - Returns trending class IDs
    - Public
```

### 5. Background Jobs

**File**: `src/main/kotlin/com/numina/jobs/AnalyticsJobs.kt`

Create scheduled jobs:

1. **DailyAnalyticsAggregation** (runs at 1 AM daily)
   - Aggregates previous day's user activity
   - Updates class analytics
   - Calculates trainer metrics
   - Refreshes platform analytics

2. **TrendingClassesRefresh** (runs every hour)
   - Updates trending classes based on recent views/bookings

### 6. Tracking Middleware

**File**: `src/main/kotlin/com/numina/middleware/AnalyticsMiddleware.kt`

Create middleware to automatically track:
- Page views
- Search queries
- API endpoint usage
- Session duration

## Completion Checklist
- [ ] Database migration created
- [ ] All domain models implemented
- [ ] Repository interface and implementation
- [ ] API routes with proper auth
- [ ] Background jobs scheduled
- [ ] Tracking middleware installed
- [ ] `.task-analytics-completed` file created

## Success Criteria
1. ✅ User can view their personal analytics and insights
2. ✅ Trainers can see their performance metrics
3. ✅ Admins can view platform-wide analytics
4. ✅ Trending classes updated automatically
5. ✅ Search analytics captured for future improvements
