# 🤖 WAVE 2 TASK: Implement Matching Engine

> **IMPORTANT**: Before starting, check if `.task-matching-completed` file exists.
> If it exists, respond: "✅ This task has already been completed."
> **When finished**, create `.task-matching-completed` file with timestamp and summary.

---

# TASK: Build User & Class Matching Engine

## Context
This is a **Wave 2 enhancement** for numina-backend. The base API (auth, profiles, classes) should already exist.

## Your Task
Implement a sophisticated matching engine that:
1. Matches **users to users** (workout partners)
2. Matches **users to classes** (personalized recommendations)
3. Provides match scoring and explanations

### Core Requirements

1. **User-to-User Matching Algorithm**

   **Matching Factors** (weighted scoring):
   - Fitness level similarity (20%): ±2 levels is ideal
   - Shared fitness interests (30%): overlap in interests (yoga, HIIT, etc.)
   - Geographic proximity (25%): within reasonable distance
   - Schedule compatibility (20%): overlapping availability
   - Past interactions (5%): positive ratings, previous partnerships

   **Match Score**: 0-100, with explanations

2. **User-to-Class Matching Algorithm**

   **Matching Factors** (weighted scoring):
   - Fitness interests match (35%): class type matches user interests
   - Appropriate intensity (25%): class intensity aligns with user level
   - Schedule fit (20%): class time matches user availability
   - Location convenience (15%): class within user's preferred radius
   - Price range (5%): within user's budget preferences

3. **API Endpoints**

   ```kotlin
   // Get potential workout partners for current user
   GET /api/v1/matches/partners
   Query params:
     - limit: Int (default: 20)
     - minScore: Int (default: 60, 0-100)
     - radius: Float (km, default: 10)

   Response: List<UserMatch> {
     userId: String,
     profile: UserProfile,
     matchScore: Int,
     matchReasons: List<String>,
     sharedInterests: List<String>,
     distanceKm: Float
   }

   // Get recommended classes for current user
   GET /api/v1/matches/classes
   Query params:
     - limit: Int (default: 20)
     - minScore: Int (default: 50)
     - startDate: Date (default: today)
     - endDate: Date (default: +7 days)

   Response: List<ClassMatch> {
     classId: String,
     classDetails: FitnessClass,
     matchScore: Int,
     matchReasons: List<String>,
     estimatedFit: String ("perfect" | "good" | "okay")
   }

   // Get mutual matches (both users matched each other)
   GET /api/v1/matches/mutual

   Response: List<MutualMatch> {
     userId: String,
     profile: UserProfile,
     matchScore: Int,
     matchedAt: DateTime
   }

   // Record a match action (like/pass)
   POST /api/v1/matches/action
   Body: {
     targetUserId: String,
     action: "like" | "pass" | "super_like"
   }

   Response: {
     mutual: Boolean,  // true if both users liked each other
     match: UserMatch? // if mutual=true
   }
   ```

4. **Database Schema**

   ```sql
   -- User match actions
   CREATE TABLE match_actions (
     id UUID PRIMARY KEY,
     user_id UUID NOT NULL,
     target_user_id UUID NOT NULL,
     action VARCHAR(20) NOT NULL, -- 'like', 'pass', 'super_like'
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (user_id) REFERENCES users(id),
     FOREIGN KEY (target_user_id) REFERENCES users(id),
     UNIQUE (user_id, target_user_id)
   );

   -- Mutual matches (cached for performance)
   CREATE TABLE mutual_matches (
     id UUID PRIMARY KEY,
     user1_id UUID NOT NULL,
     user2_id UUID NOT NULL,
     match_score INT NOT NULL,
     matched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (user1_id) REFERENCES users(id),
     FOREIGN KEY (user2_id) REFERENCES users(id),
     CHECK (user1_id < user2_id), -- ensure canonical ordering
     UNIQUE (user1_id, user2_id)
   );

   -- Match preferences (optional enhancements)
   CREATE TABLE match_preferences (
     user_id UUID PRIMARY KEY,
     max_distance_km FLOAT DEFAULT 10.0,
     min_fitness_level INT,
     max_fitness_level INT,
     preferred_age_min INT,
     preferred_age_max INT,
     FOREIGN KEY (user_id) REFERENCES users(id)
   );
   ```

5. **Matching Logic Implementation**

   Create service classes:
   - `MatchingService.kt`: Core matching algorithms
   - `UserMatcher.kt`: User-to-user matching logic
   - `ClassMatcher.kt`: User-to-class recommendation logic
   - `ScoreCalculator.kt`: Weighted scoring algorithms

6. **Performance Optimization**
   - Cache match calculations (expire after profile updates)
   - Use database indexes on user_id, location, fitness_level
   - Batch scoring for multiple candidates
   - Limit candidate pool using geographic prefiltering

### Deliverables

1. **Matching Engine Implementation**:
   - All API endpoints working
   - Database tables created and migrated
   - Service layer with matching algorithms
   - Repository layer for match data

2. **Testing**:
   - Unit tests for scoring algorithms
   - Integration tests for match endpoints
   - Test with realistic user/class data

3. **Documentation**:
   - Update README with matching endpoints
   - Document matching algorithm and weights
   - Provide example API calls with curl

4. **TODO.md Updates**:
   - List enhancements: ML-based matching, collaborative filtering, etc.

### Acceptance Criteria

1. ✅ User can get recommended workout partners
2. ✅ User can get recommended classes
3. ✅ Match scores are meaningful (60+ = good match)
4. ✅ Match reasons explain why users/classes matched
5. ✅ Like/pass actions are recorded
6. ✅ Mutual matches are detected and returned
7. ✅ API performance is acceptable (<500ms for match queries)
8. ✅ All endpoints have proper authentication
9. ✅ Tests cover matching logic
10. ✅ Documentation is comprehensive

### How to Report Back

1. **Update README.md** with matching endpoints and algorithm explanation
2. **Update TODO.md** with ML/AI enhancement ideas
3. **Create `.task-matching-completed`** file with:
   ```
   Completed: [timestamp]
   Summary: Matching engine implemented successfully
   Endpoints: 4 new endpoints for partner/class matching
   Features: User-to-user matching, user-to-class recommendations
   Next: See TODO.md for ML enhancements
   ```
4. **Commit and push** with message:
   ```
   feat: Implement matching engine for partners and classes

   🤖 Generated with [Claude Code](https://claude.com/claude-code)

   Co-Authored-By: Claude <noreply@anthropic.com>
   ```
