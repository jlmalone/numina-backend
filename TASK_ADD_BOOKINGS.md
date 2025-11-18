# TASK: Add Bookings Management to Numina Backend

> **IMPORTANT**: Check for `.task-bookings-completed` before starting.
> **When finished**, create `.task-bookings-completed` file.

## 🎯 OBJECTIVE

Track user bookings, build a personal fitness calendar, and send reminders for upcoming classes.

## 📋 CORE FEATURES

1. **Booking Tracking**
   - Mark classes as "booked" (tracked externally)
   - Track booking source (Mindbody, ClassPass, etc.)
   - Booking status (booked, attended, cancelled, no-show)
   - Sync with external booking systems (optional)

2. **Personal Calendar**
   - View upcoming booked classes
   - Calendar view (day, week, month)
   - Filter by class type, location, time
   - Export to iCal/Google Calendar

3. **Reminders**
   - Configurable reminders (1 hour, 1 day before)
   - Push notifications for reminders
   - Email reminders
   - Reminder preferences per class type

4. **Attendance Tracking**
   - Mark classes as attended
   - Track attendance history
   - Workout streak tracking
   - Attendance-based achievements

### API Endpoints

```
# Bookings
POST   /api/v1/bookings                       # Create booking
GET    /api/v1/bookings                       # List my bookings
GET    /api/v1/bookings/{id}                  # Get booking details
PUT    /api/v1/bookings/{id}                  # Update booking
DELETE /api/v1/bookings/{id}                  # Delete booking
POST   /api/v1/bookings/{id}/mark-attended    # Mark as attended
POST   /api/v1/bookings/{id}/cancel           # Cancel booking

# Calendar
GET    /api/v1/calendar/upcoming              # Upcoming classes
GET    /api/v1/calendar/month/{yyyy-MM}       # Calendar month view
GET    /api/v1/calendar/export                # Export iCal format

# Reminders
GET    /api/v1/bookings/reminder-preferences  # Get preferences
PUT    /api/v1/bookings/reminder-preferences  # Update preferences

# Stats
GET    /api/v1/bookings/stats                 # Attendance stats
GET    /api/v1/bookings/streak                # Current streak
```

### Database Schema

**bookings**:
- id (UUID, PK)
- user_id (UUID, FK to users)
- class_id (UUID, FK to fitness_classes)
- booking_source (VARCHAR) # "mindbody", "classpass", "manual"
- external_booking_id (VARCHAR, nullable)
- booked_at (TIMESTAMP)
- class_datetime (TIMESTAMP) # Denormalized for queries
- status (ENUM: booked, attended, cancelled, no_show)
- attended_at (TIMESTAMP, nullable)
- cancelled_at (TIMESTAMP, nullable)
- cancellation_reason (TEXT, nullable)
- reminder_sent_1h (BOOLEAN, default false)
- reminder_sent_24h (BOOLEAN, default false)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

**booking_reminders**:
- id (UUID, PK)
- user_id (UUID, FK to users, unique)
- enabled (BOOLEAN, default true)
- reminder_1h (BOOLEAN, default true)
- reminder_24h (BOOLEAN, default true)
- email_reminders (BOOLEAN, default false)
- push_reminders (BOOLEAN, default true)
- updated_at (TIMESTAMP)

**attendance_stats** (denormalized):
- user_id (UUID, FK to users, PK)
- total_booked (INT, default 0)
- total_attended (INT, default 0)
- total_cancelled (INT, default 0)
- current_streak (INT, default 0)
- longest_streak (INT, default 0)
- last_attended_date (DATE, nullable)
- updated_at (TIMESTAMP)

## 🏗️ IMPLEMENTATION

### Services
- `BookingService.kt`: CRUD, status management
- `CalendarService.kt`: Calendar views, iCal export
- `ReminderService.kt`: Send reminders
- `AttendanceStatsService.kt`: Track stats and streaks

### Background Jobs
- Reminder processor: Check hourly for upcoming classes
- Stats updater: Daily job to update attendance stats
- Streak calculator: Daily job to check/update streaks

### Integration
- Integrate with notification system for reminders
- Update attendance stats when marking attended
- Verify booking eligibility for reviews

### Testing
- Booking CRUD operations
- Calendar queries
- iCal export format
- Reminder scheduling
- Streak calculations

## ✅ ACCEPTANCE CRITERIA

- [ ] Users can track their bookings
- [ ] Calendar views work correctly
- [ ] Reminders sent at correct times
- [ ] Attendance tracking updates stats
- [ ] Streak calculations accurate
- [ ] iCal export works with external calendars
- [ ] All endpoints tested
- [ ] Background jobs scheduled properly

## 📝 DELIVERABLES

- Booking service implementation
- Calendar service with iCal export
- Reminder scheduling
- API routes
- Database migrations
- Background jobs
- Tests
- Documentation

## 🚀 COMPLETION

1. Test: `./gradlew test`
2. Build: `./gradlew build`
3. Create `.task-bookings-completed`
4. Commit: "Add bookings management and calendar system"
5. Push: `git push -u origin claude/add-bookings-system`

---

**Estimated Time**: 60-75 minutes
**Priority**: MEDIUM
