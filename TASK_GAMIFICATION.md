# Task: Add Gamification & User Engagement Features

> **IMPORTANT**: Check for `.task-gamification-completed` before starting.
> If it exists, respond: "✅ This task has already been implemented."
> **When finished**, create `.task-gamification-completed` file.

## Overview
Add comprehensive gamification features including challenges, achievements, leaderboards, and user statistics to increase engagement and retention.

## Requirements

### 1. Database Schema

**File**: `src/main/resources/db/migration/V9__Add_Gamification.sql`
```sql
-- Achievements
CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    icon_url VARCHAR(500),
    category VARCHAR(50) NOT NULL, -- attendance, social, fitness, milestone
    tier VARCHAR(20) NOT NULL, -- bronze, silver, gold, platinum
    points INTEGER NOT NULL DEFAULT 0,
    requirement_type VARCHAR(50) NOT NULL, -- class_count, streak_days, follower_count, etc.
    requirement_value INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Achievements
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id UUID NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_unlocked ON user_achievements(unlocked_at);

-- Challenges
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    image_url VARCHAR(500),
    challenge_type VARCHAR(50) NOT NULL, -- attendance, streak, social, distance, calories
    goal_value INTEGER NOT NULL,
    goal_metric VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    points_reward INTEGER NOT NULL DEFAULT 0,
    max_participants INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_challenges_active ON challenges(is_active, end_date);
CREATE INDEX idx_challenges_dates ON challenges(start_date, end_date);

-- User Challenge Participation
CREATE TABLE user_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    challenge_id UUID NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_progress INTEGER NOT NULL DEFAULT 0,
    completed_at TIMESTAMP,
    points_earned INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, challenge_id)
);

CREATE INDEX idx_user_challenges_user ON user_challenges(user_id);
CREATE INDEX idx_user_challenges_challenge ON user_challenges(challenge_id);
CREATE INDEX idx_user_challenges_completed ON user_challenges(completed_at);

-- User Stats
CREATE TABLE user_stats (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_classes_attended INTEGER NOT NULL DEFAULT 0,
    current_streak_days INTEGER NOT NULL DEFAULT 0,
    longest_streak_days INTEGER NOT NULL DEFAULT 0,
    last_class_date DATE,
    total_points INTEGER NOT NULL DEFAULT 0,
    total_achievements INTEGER NOT NULL DEFAULT 0,
    total_challenges_completed INTEGER NOT NULL DEFAULT 0,
    favorite_activity_type VARCHAR(100),
    total_distance_km DECIMAL(10,2) DEFAULT 0,
    total_calories_burned INTEGER DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_stats_points ON user_stats(total_points DESC);
CREATE INDEX idx_user_stats_streak ON user_stats(current_streak_days DESC);

-- Leaderboards (materialized view or table)
CREATE TABLE leaderboard_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    leaderboard_type VARCHAR(50) NOT NULL, -- points, streak, classes, challenges
    time_period VARCHAR(20) NOT NULL, -- all_time, monthly, weekly
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_name VARCHAR(255) NOT NULL,
    user_photo_url VARCHAR(500),
    rank INTEGER NOT NULL,
    value INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(leaderboard_type, time_period, user_id)
);

CREATE INDEX idx_leaderboard_type_period ON leaderboard_cache(leaderboard_type, time_period, rank);
```

### 2. Domain Models

**File**: `src/main/kotlin/com/numina/domain/model/Achievement.kt`
```kotlin
package com.numina.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String? = null,
    val category: AchievementCategory,
    val tier: AchievementTier,
    val points: Int,
    val requirementType: String,
    val requirementValue: Int,
    val createdAt: String
)

@Serializable
enum class AchievementCategory {
    ATTENDANCE, SOCIAL, FITNESS, MILESTONE
}

@Serializable
enum class AchievementTier {
    BRONZE, SILVER, GOLD, PLATINUM
}

@Serializable
data class UserAchievement(
    val id: String,
    val userId: String,
    val achievement: Achievement,
    val unlockedAt: String,
    val progress: Int
)
```

**File**: `src/main/kotlin/com/numina/domain/model/Challenge.kt`
```kotlin
package com.numina.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val challengeType: ChallengeType,
    val goalValue: Int,
    val goalMetric: String,
    val startDate: String,
    val endDate: String,
    val pointsReward: Int,
    val maxParticipants: Int? = null,
    val currentParticipants: Int = 0,
    val isActive: Boolean,
    val createdBy: String? = null,
    val createdAt: String
)

@Serializable
enum class ChallengeType {
    ATTENDANCE, STREAK, SOCIAL, DISTANCE, CALORIES
}

@Serializable
data class UserChallenge(
    val id: String,
    val userId: String,
    val challenge: Challenge,
    val joinedAt: String,
    val currentProgress: Int,
    val completedAt: String? = null,
    val pointsEarned: Int
)

@Serializable
data class ChallengeProgress(
    val challengeId: String,
    val currentProgress: Int,
    val goalValue: Int,
    val percentComplete: Int,
    val isCompleted: Boolean
)
```

**File**: `src/main/kotlin/com/numina/domain/model/UserStats.kt`
```kotlin
package com.numina.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val userId: String,
    val totalClassesAttended: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastClassDate: String? = null,
    val totalPoints: Int,
    val totalAchievements: Int,
    val totalChallengesCompleted: Int,
    val favoriteActivityType: String? = null,
    val totalDistanceKm: Double,
    val totalCaloriesBurned: Int,
    val updatedAt: String
)

@Serializing
data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val userPhotoUrl: String? = null,
    val rank: Int,
    val value: Int,
    val isCurrentUser: Boolean = false
)

@Serializable
data class Leaderboard(
    val type: LeaderboardType,
    val timePeriod: TimePeriod,
    val entries: List<LeaderboardEntry>,
    val currentUserRank: Int? = null
)

@Serializable
enum class LeaderboardType {
    POINTS, STREAK, CLASSES, CHALLENGES
}

@Serializable
enum class TimePeriod {
    ALL_TIME, MONTHLY, WEEKLY
}
```

### 3. Repository Layer

**File**: `src/main/kotlin/com/numina/data/repository/GamificationRepository.kt`
```kotlin
package com.numina.data.repository

import com.numina.domain.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class GamificationRepository {
    // Achievements
    fun getAllAchievements(): List<Achievement> = transaction {
        // Implementation
    }

    fun getUserAchievements(userId: UUID): List<UserAchievement> = transaction {
        // Implementation
    }

    fun unlockAchievement(userId: UUID, achievementId: UUID, progress: Int = 100) = transaction {
        // Implementation
    }

    fun updateAchievementProgress(userId: UUID, achievementId: UUID, progress: Int) = transaction {
        // Implementation
    }

    // Challenges
    fun getActiveChallenges(): List<Challenge> = transaction {
        // Implementation
    }

    fun getChallengeById(id: UUID): Challenge? = transaction {
        // Implementation
    }

    fun createChallenge(challenge: Challenge): Challenge = transaction {
        // Implementation
    }

    fun joinChallenge(userId: UUID, challengeId: UUID) = transaction {
        // Implementation
    }

    fun getUserChallenges(userId: UUID): List<UserChallenge> = transaction {
        // Implementation
    }

    fun updateChallengeProgress(userId: UUID, challengeId: UUID, progress: Int) = transaction {
        // Check if completed and award points
    }

    // Stats
    fun getUserStats(userId: UUID): UserStats? = transaction {
        // Implementation
    }

    fun updateUserStats(userId: UUID, stats: UserStats) = transaction {
        // Implementation
    }

    fun incrementClassCount(userId: UUID) = transaction {
        // Update stats and check achievements
    }

    fun updateStreak(userId: UUID) = transaction {
        // Calculate and update streak
    }

    // Leaderboards
    fun getLeaderboard(type: LeaderboardType, period: TimePeriod, limit: Int = 100): Leaderboard = transaction {
        // Implementation
    }

    fun refreshLeaderboardCache() = transaction {
        // Recalculate leaderboards
    }
}
```

### 4. Routes

**File**: `src/main/kotlin/com/numina/routes/GamificationRoutes.kt`
```kotlin
package com.numina.routes

import com.numina.domain.model.*
import com.numina.plugins.getUserIdFromToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.gamificationRoutes(repository: GamificationRepository) {
    route("/api/gamification") {

        // Achievements
        get("/achievements") {
            val achievements = repository.getAllAchievements()
            call.respond(HttpStatusCode.OK, achievements)
        }

        get("/achievements/user") {
            val userId = call.getUserIdFromToken()
            val userAchievements = repository.getUserAchievements(userId)
            call.respond(HttpStatusCode.OK, userAchievements)
        }

        // Challenges
        get("/challenges") {
            val challenges = repository.getActiveChallenges()
            call.respond(HttpStatusCode.OK, challenges)
        }

        get("/challenges/{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val challenge = repository.getChallengeById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, challenge)
        }

        post("/challenges") {
            val userId = call.getUserIdFromToken()
            val request = call.receive<CreateChallengeRequest>()

            val challenge = repository.createChallenge(request.toChallenge(userId))
            call.respond(HttpStatusCode.Created, challenge)
        }

        post("/challenges/{id}/join") {
            val userId = call.getUserIdFromToken()
            val challengeId = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            repository.joinChallenge(userId, challengeId)
            call.respond(HttpStatusCode.OK)
        }

        get("/challenges/user") {
            val userId = call.getUserIdFromToken()
            val userChallenges = repository.getUserChallenges(userId)
            call.respond(HttpStatusCode.OK, userChallenges)
        }

        // Stats
        get("/stats") {
            val userId = call.getUserIdFromToken()
            val stats = repository.getUserStats(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, stats)
        }

        get("/stats/{userId}") {
            val userId = call.parameters["userId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val stats = repository.getUserStats(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(HttpStatusCode.OK, stats)
        }

        // Leaderboards
        get("/leaderboards/{type}/{period}") {
            val type = call.parameters["type"]?.let { LeaderboardType.valueOf(it.uppercase()) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val period = call.parameters["period"]?.let { TimePeriod.valueOf(it.uppercase()) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

            val leaderboard = repository.getLeaderboard(type, period, limit)
            call.respond(HttpStatusCode.OK, leaderboard)
        }
    }
}
```

### 5. Background Jobs

**File**: `src/main/kotlin/com/numina/jobs/GamificationJobs.kt`
```kotlin
package com.numina.jobs

import com.numina.data.repository.GamificationRepository
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class GamificationJobs(private val repository: GamificationRepository) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        // Refresh leaderboards every 15 minutes
        scope.launch {
            while (isActive) {
                try {
                    repository.refreshLeaderboardCache()
                } catch (e: Exception) {
                    // Log error
                }
                delay(TimeUnit.MINUTES.toMillis(15))
            }
        }

        // Check and update user streaks daily
        scope.launch {
            while (isActive) {
                try {
                    // Update all user streaks
                } catch (e: Exception) {
                    // Log error
                }
                delay(TimeUnit.HOURS.toMillis(24))
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
```

### 6. Seed Data

**File**: `src/main/kotlin/com/numina/data/SeedAchievements.kt`
```kotlin
package com.numina.data

fun seedAchievements() {
    val achievements = listOf(
        // Attendance Achievements
        Achievement("First Class", "Attend your first fitness class", ATTENDANCE, BRONZE, 10, "class_count", 1),
        Achievement("Getting Started", "Attend 5 classes", ATTENDANCE, BRONZE, 50, "class_count", 5),
        Achievement("Dedicated", "Attend 25 classes", ATTENDANCE, SILVER, 100, "class_count", 25),
        Achievement("Committed", "Attend 50 classes", ATTENDANCE, GOLD, 250, "class_count", 50),
        Achievement("Fitness Legend", "Attend 100 classes", ATTENDANCE, PLATINUM, 500, "class_count", 100),

        // Streak Achievements
        Achievement("Consistent", "Maintain a 3-day streak", ATTENDANCE, BRONZE, 25, "streak_days", 3),
        Achievement("On Fire", "Maintain a 7-day streak", ATTENDANCE, SILVER, 100, "streak_days", 7),
        Achievement("Unstoppable", "Maintain a 30-day streak", ATTENDANCE, GOLD, 500, "streak_days", 30),
        Achievement("Iron Will", "Maintain a 100-day streak", ATTENDANCE, PLATINUM, 1000, "streak_days", 100),

        // Social Achievements
        Achievement("Social Butterfly", "Follow 10 users", SOCIAL, BRONZE, 25, "following_count", 10),
        Achievement("Popular", "Get 25 followers", SOCIAL, SILVER, 100, "follower_count", 25),
        Achievement("Influencer", "Get 100 followers", SOCIAL, GOLD, 500, "follower_count", 100),
        Achievement("Community Leader", "Create a group with 50+ members", SOCIAL, GOLD, 250, "group_members", 50),

        // Milestone Achievements
        Achievement("Explorer", "Try 5 different activity types", FITNESS, BRONZE, 50, "activity_types", 5),
        Achievement("Versatile Athlete", "Try 10 different activity types", FITNESS, SILVER, 150, "activity_types", 10),
        Achievement("Distance Runner", "Complete 100km total", FITNESS, SILVER, 200, "distance_km", 100),
        Achievement("Calorie Crusher", "Burn 10,000 calories", FITNESS, SILVER, 200, "calories_burned", 10000)
    )

    // Insert into database
}
```

## Completion Checklist
- [ ] Database migration created and tested
- [ ] All domain models implemented
- [ ] Repository layer complete
- [ ] API routes implemented
- [ ] Background jobs configured
- [ ] Achievement seed data created
- [ ] All endpoints tested
- [ ] `.task-gamification-completed` file created

## Testing
Test endpoints:
```bash
# Get all achievements
curl http://localhost:8080/api/gamification/achievements

# Get user stats
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/gamification/stats

# Get leaderboard
curl http://localhost:8080/api/gamification/leaderboards/points/all_time

# Join challenge
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/gamification/challenges/{id}/join
```

## Success Criteria
1. ✅ Complete gamification system with achievements, challenges, and leaderboards
2. ✅ User stats tracking functional
3. ✅ Background jobs running
4. ✅ All API endpoints working
5. ✅ Seed data populated
