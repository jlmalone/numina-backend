# TASK: Add Reviews & Ratings System to Numina Backend

> **IMPORTANT**: Check for `.task-reviews-completed` before starting.
> **When finished**, create `.task-reviews-completed` file.

## 🎯 OBJECTIVE

Implement a comprehensive reviews and ratings system for fitness classes, trainers, and providers.

## 📋 CORE FEATURES

1. **Class Reviews**
   - Rate classes (1-5 stars)
   - Written reviews with pros/cons
   - Photos (optional)
   - Attendance verification (can only review attended classes)
   - Edit/delete own reviews within 30 days

2. **Trainer Ratings**
   - Rate trainers separately from classes
   - Track trainer average ratings
   - Trainer response to reviews

3. **Provider Ratings**
   - Overall provider/gym ratings
   - Aggregate from class reviews
   - Facility ratings (cleanliness, equipment, amenities)

4. **Review Moderation**
   - Flag inappropriate reviews
   - Admin review approval workflow
   - Helpful/not helpful voting

### API Endpoints

```
# Reviews
POST   /api/v1/reviews/classes/{classId}      # Create class review
GET    /api/v1/reviews/classes/{classId}      # Get class reviews
PUT    /api/v1/reviews/{reviewId}             # Update own review
DELETE /api/v1/reviews/{reviewId}             # Delete own review
POST   /api/v1/reviews/{reviewId}/helpful     # Mark review helpful
POST   /api/v1/reviews/{reviewId}/report      # Report review

# Trainer Reviews
POST   /api/v1/reviews/trainers/{trainerId}   # Review trainer
GET    /api/v1/reviews/trainers/{trainerId}   # Get trainer reviews

# Provider Reviews
GET    /api/v1/reviews/providers/{providerId} # Get provider reviews

# My Reviews
GET    /api/v1/reviews/my-reviews             # User's reviews
GET    /api/v1/reviews/pending                # Classes eligible for review

# Ratings Summary
GET    /api/v1/ratings/classes/{classId}      # Class rating summary
GET    /api/v1/ratings/trainers/{trainerId}   # Trainer rating summary
GET    /api/v1/ratings/providers/{providerId} # Provider rating summary
```

### Database Schema

**reviews**:
- id (UUID, PK)
- user_id (UUID, FK to users)
- class_id (UUID, FK to fitness_classes, nullable)
- trainer_id (UUID, FK to trainers, nullable)
- provider_id (UUID, FK to providers, nullable)
- rating (INT, 1-5)
- title (VARCHAR)
- content (TEXT)
- pros (TEXT, nullable)
- cons (TEXT, nullable)
- attended_on (DATE, nullable)
- verified_attendance (BOOLEAN, default false)
- helpful_count (INT, default 0)
- status (ENUM: pending, approved, rejected, flagged)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
- UNIQUE(user_id, class_id) # One review per user per class

**review_photos**:
- id (UUID, PK)
- review_id (UUID, FK to reviews)
- photo_url (VARCHAR)
- created_at (TIMESTAMP)

**review_votes**:
- id (UUID, PK)
- review_id (UUID, FK to reviews)
- user_id (UUID, FK to users)
- vote_type (ENUM: helpful, not_helpful)
- created_at (TIMESTAMP)
- UNIQUE(review_id, user_id)

**review_reports**:
- id (UUID, PK)
- review_id (UUID, FK to reviews)
- reporter_id (UUID, FK to users)
- reason (VARCHAR)
- status (ENUM: pending, reviewed, resolved)
- created_at (TIMESTAMP)

**rating_aggregates** (materialized view or denormalized table):
- entity_type (ENUM: class, trainer, provider)
- entity_id (UUID)
- average_rating (DECIMAL)
- total_reviews (INT)
- rating_distribution (JSONB) # {1: count, 2: count, ...}
- last_updated (TIMESTAMP)

## 🏗️ IMPLEMENTATION

### Services
- `ReviewService.kt`: CRUD, voting, reporting
- `RatingAggregationService.kt`: Calculate/update rating summaries
- Background job to update aggregates

### Validation
- Verify user attended class before allowing review
- One review per user per class
- Rating must be 1-5
- Content length limits

### Testing
- Review CRUD operations
- Rating calculations
- Attendance verification
- Permission checks
- Aggregate updates

## ✅ ACCEPTANCE CRITERIA

- [ ] Users can review classes they've attended
- [ ] Ratings accurately aggregate
- [ ] Helpful voting works
- [ ] Review moderation flow functional
- [ ] Photo uploads supported
- [ ] All endpoints tested
- [ ] Performance optimized (aggregate queries < 200ms)

## 📝 DELIVERABLES

- Review service implementation
- Rating aggregation service
- API routes
- Database migrations
- Tests
- Documentation

## 🚀 COMPLETION

1. Test: `./gradlew test`
2. Build: `./gradlew build`
3. Create `.task-reviews-completed`
4. Commit: "Add reviews and ratings system"
5. Push: `git push -u origin claude/add-reviews-system`

---

**Estimated Time**: 60-75 minutes
**Priority**: MEDIUM-HIGH
