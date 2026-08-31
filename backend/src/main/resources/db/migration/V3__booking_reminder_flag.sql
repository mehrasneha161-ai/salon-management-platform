-- ============================================================
-- V3 — Track whether a booking's reminder has been sent
-- ============================================================
ALTER TABLE bookings ADD COLUMN reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_bookings_reminder ON bookings(scheduled_date, reminder_sent);
