# Numina Backend - TODO

Prioritized list of features and improvements for future development.

## High Priority

### 1. User Matching Engine ✅ COMPLETED
- [x] Implement matching algorithm based on fitness interests, level, and location
- [x] Create Matches table schema (MatchActions, MutualMatches, MatchPreferences)
- [x] Add endpoints to view/accept/reject matches
- [x] Implement matching preferences in user profiles
- [x] Add match scoring algorithm (compatibility metrics)

**Implemented Features:**
- User-to-user matching with weighted scoring (fitness level, interests, location, schedule, past interactions)
- User-to-class recommendations with personalized scoring
- Match actions (like/pass/super_like) with mutual match detection
- 4 REST endpoints: GET /matches/partners, GET /matches/classes, GET /matches/mutual, POST /matches/action
- Comprehensive scoring algorithm with explainable match reasons

**Next Steps - ML/AI Enhancements:**
- [ ] Implement machine learning-based matching using collaborative filtering
- [ ] Add neural network for personalized class recommendations
- [ ] Implement real-time preference learning from user actions
- [ ] Add A/B testing framework for matching algorithm improvements
- [ ] Use NLP for analyzing class descriptions and user interests
- [ ] Implement time-series analysis for optimal workout timing recommendations
- [ ] Add anomaly detection for identifying unusual matching patterns
- [ ] Use reinforcement learning to optimize match suggestions over time
- [ ] Implement clustering algorithms to identify user fitness personas
- [ ] Add similarity learning for better feature representation
- [ ] Use graph neural networks for community detection and group matching
- [ ] Implement deep learning models for predicting match success rates

### 2. Messaging System
- [ ] Create Messages table schema
- [ ] Implement WebSocket support for real-time messaging
- [ ] Add REST endpoints for message history
- [ ] Implement message notifications
- [ ] Add message read/unread status tracking

### 3. Role-Based Access Control (RBAC)
- [ ] Add roles table (admin, instructor, user, etc.)
- [ ] Implement role-based authorization
- [ ] Restrict class creation to admins and instructors
- [ ] Add user role management endpoints

### 4. Class Attendance & RSVPs
- [ ] Create ClassAttendance table
- [ ] Add RSVP endpoints (join/leave class)
- [ ] Implement capacity management
- [ ] Add waitlist functionality
- [ ] Track attendance history

## Medium Priority

### 5. Ratings & Reviews
- [ ] Create Ratings table for classes
- [ ] Create UserReviews table for peer reviews
- [ ] Add rating submission endpoints
- [ ] Implement average rating calculations
- [ ] Add review moderation system

### 6. Social Features
- [ ] Create UserConnections table (friends/following)
- [ ] Add friend request system
- [ ] Implement activity feed
- [ ] Add social sharing capabilities
- [ ] Implement user blocking/reporting

### 7. Advanced Search & Filtering
- [ ] Implement full-text search for classes
- [ ] Add Elasticsearch integration for better search
- [ ] Optimize location-based queries with spatial indexes (PostGIS)
- [ ] Add saved searches functionality
- [x] Implement basic recommendation engine (matching system)
- [ ] Enhance with ML-based collaborative filtering
- [ ] Add content-based filtering for classes
- [ ] Implement hybrid recommendation system combining multiple approaches

### 8. Notifications System
- [ ] Create Notifications table
- [ ] Implement push notification service
- [ ] Add email notifications
- [ ] User notification preferences
- [ ] Notification history and management

### 9. Email Integration
- [ ] Email verification for registration
- [ ] Password reset functionality
- [ ] Welcome email templates
- [ ] Class reminder emails
- [ ] Match notification emails

## Low Priority

### 10. File Upload Service
- [ ] Implement file upload for profile photos
- [ ] Add image processing (resize, crop, optimize)
- [ ] Integrate S3 or similar object storage
- [ ] Add file validation and security scanning

### 11. Analytics & Insights
- [ ] User activity tracking
- [ ] Class popularity metrics
- [ ] Matching success rates
- [ ] Dashboard for admin analytics
- [ ] User fitness journey insights

### 12. Performance Optimizations
- [ ] Add Redis caching layer
- [ ] Implement database query optimization
- [ ] Add database connection pooling
- [ ] Implement rate limiting
- [ ] Add API response pagination

### 13. Enhanced Security
- [ ] Implement 2FA (two-factor authentication)
- [ ] Add OAuth2 support (Google, Apple, Facebook)
- [ ] Implement security headers
- [ ] Add CSRF protection
- [ ] Regular security audits

### 14. API Versioning
- [ ] Implement proper API versioning strategy
- [ ] Add API deprecation notices
- [ ] Maintain backward compatibility
- [ ] Document version migration guides

### 15. Admin Panel Features
- [ ] User management interface
- [ ] Class management tools
- [ ] Content moderation dashboard
- [ ] Analytics visualization
- [ ] System health monitoring

## Infrastructure & DevOps

### 16. CI/CD Pipeline
- [ ] GitHub Actions workflow for tests
- [ ] Automated deployment pipeline
- [ ] Staging environment setup
- [ ] Production deployment automation
- [ ] Database migration automation

### 17. Monitoring & Logging
- [ ] Integrate application monitoring (Datadog, New Relic)
- [ ] Structured logging with correlation IDs
- [ ] Error tracking (Sentry)
- [ ] Performance monitoring
- [ ] Health check endpoints

### 18. Documentation
- [ ] OpenAPI/Swagger documentation
- [ ] API client SDKs generation
- [ ] Developer onboarding guide
- [ ] Architecture decision records (ADRs)
- [ ] Runbook for operations

### 19. Testing Improvements
- [ ] Increase test coverage to 80%+
- [ ] Add performance tests
- [ ] Implement contract testing
- [ ] Add E2E testing suite
- [ ] Load testing scenarios

### 20. Compliance & Legal
- [ ] GDPR compliance implementation
- [ ] User data export functionality
- [ ] Data retention policies
- [ ] Terms of service acceptance tracking
- [ ] Privacy policy enforcement

## Future Considerations

- **Mobile App Integration**: Optimize APIs for mobile client consumption
- **Third-party Integrations**: ClassPass, Mindbody, Strava, Apple Health
- **Gamification**: Streaks, achievements, leaderboards
- **Subscription Management**: Premium features, tiered pricing
- **Event Management**: Group workouts, fitness challenges, meetups
- **Video Integration**: Live streaming, recorded classes
- **Payment Processing**: In-app purchases, class bookings
- **Multi-tenancy**: Support for multiple gym/studio chains

---

**Note**: Priorities may shift based on user feedback and business requirements.
