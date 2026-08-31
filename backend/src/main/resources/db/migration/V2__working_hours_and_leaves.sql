-- ============================================================
-- V2 — Configurable working hours + staff leaves
-- ============================================================

-- Outlet business hours (default 09:00–20:00 to preserve current behaviour).
ALTER TABLE outlets ADD COLUMN opening_time TIME NOT NULL DEFAULT '09:00';
ALTER TABLE outlets ADD COLUMN closing_time TIME NOT NULL DEFAULT '20:00';

-- Optional per-staff shift window (NULL = follow the outlet hours).
ALTER TABLE staff_profiles ADD COLUMN shift_start TIME;
ALTER TABLE staff_profiles ADD COLUMN shift_end   TIME;

-- Staff leave / holidays (inclusive date range).
CREATE TABLE staff_leaves (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    staff_id   UUID        NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    reason     VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_staff_leaves_staff_dates ON staff_leaves(staff_id, start_date, end_date);
