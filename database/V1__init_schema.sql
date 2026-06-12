-- ============================================================
-- Salon Management Platform — Flyway Migration V1
-- PostgreSQL 15+
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ─── 1. users ─────────────────────────────────────────────
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name       VARCHAR(100)  NOT NULL,
    phone_number    VARCHAR(15)   UNIQUE NOT NULL,
    email           VARCHAR(150)  UNIQUE,
    password_hash   TEXT          NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    profile_pic_url TEXT,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_users_phone  ON users(phone_number);
CREATE INDEX idx_users_email  ON users(email);
CREATE INDEX idx_users_role   ON users(role);

-- ─── 2. refresh_tokens ────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT        NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    is_revoked  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ─── 3. outlets ───────────────────────────────────────────
CREATE TABLE outlets (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(150) NOT NULL,
    address    TEXT         NOT NULL,
    city       VARCHAR(100),
    phone      VARCHAR(15),
    latitude   DECIMAL(10,8),
    longitude  DECIMAL(11,8),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ─── 4. staff_profiles ────────────────────────────────────
CREATE TABLE staff_profiles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    outlet_id       UUID         NOT NULL REFERENCES outlets(id) ON DELETE RESTRICT,
    specialization  VARCHAR(200),
    bio             TEXT,
    profile_pic_url TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_staff_outlet  ON staff_profiles(outlet_id);
CREATE INDEX idx_staff_status  ON staff_profiles(status);

-- ─── 5. staff_attendance ──────────────────────────────────
CREATE TABLE staff_attendance (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    staff_id     UUID        NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    date         DATE        NOT NULL,
    check_in_at  TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    status       VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(staff_id, date)
);
CREATE INDEX idx_attendance_staff_date ON staff_attendance(staff_id, date);

-- ─── 6. service_categories ────────────────────────────────
CREATE TABLE service_categories (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) NOT NULL,
    icon_url   TEXT,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN      NOT NULL DEFAULT FALSE
);

-- ─── 7. services ──────────────────────────────────────────
CREATE TABLE services (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id      UUID           NOT NULL REFERENCES service_categories(id) ON DELETE RESTRICT,
    outlet_id        UUID           REFERENCES outlets(id) ON DELETE SET NULL,
    name             VARCHAR(200)   NOT NULL,
    description      TEXT,
    duration_minutes INT            NOT NULL,
    price            DECIMAL(10,2)  NOT NULL,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_services_category ON services(category_id);
CREATE INDEX idx_services_outlet   ON services(outlet_id);

-- ─── 8. packages ──────────────────────────────────────────
CREATE TABLE packages (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    outlet_id    UUID           REFERENCES outlets(id) ON DELETE SET NULL,
    name         VARCHAR(200)   NOT NULL,
    description  TEXT,
    price        DECIMAL(10,2)  NOT NULL,
    discount_pct DECIMAL(5,2)   NOT NULL DEFAULT 0,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted   BOOLEAN        NOT NULL DEFAULT FALSE
);

-- ─── 9. package_services ──────────────────────────────────
CREATE TABLE package_services (
    package_id UUID NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (package_id, service_id)
);

-- ─── 10. bookings ─────────────────────────────────────────
CREATE TABLE bookings (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_ref      VARCHAR(20)    UNIQUE NOT NULL,
    customer_id      UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    outlet_id        UUID           NOT NULL REFERENCES outlets(id) ON DELETE RESTRICT,
    staff_id         UUID           REFERENCES staff_profiles(id) ON DELETE SET NULL,
    service_id       UUID           REFERENCES services(id) ON DELETE SET NULL,
    package_id       UUID           REFERENCES packages(id) ON DELETE SET NULL,
    scheduled_date   DATE           NOT NULL,
    scheduled_time   TIME           NOT NULL,
    duration_minutes INT            NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    total_amount     DECIMAL(10,2)  NOT NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted       BOOLEAN        NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_bookings_customer       ON bookings(customer_id);
CREATE INDEX idx_bookings_staff          ON bookings(staff_id);
CREATE INDEX idx_bookings_outlet_date    ON bookings(outlet_id, scheduled_date);
CREATE INDEX idx_bookings_status         ON bookings(status);
CREATE INDEX idx_bookings_scheduled_date ON bookings(scheduled_date);

-- ─── 11. payments ─────────────────────────────────────────
CREATE TABLE payments (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id     UUID           NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE RESTRICT,
    amount         DECIMAL(10,2)  NOT NULL,
    currency       VARCHAR(5)     NOT NULL DEFAULT 'INR',
    gateway        VARCHAR(50),
    gateway_txn_id VARCHAR(200),
    status         VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    paid_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_status  ON payments(status);

-- ─── 12. reviews ──────────────────────────────────────────
CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id  UUID     NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    customer_id UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id    UUID     REFERENCES staff_profiles(id) ON DELETE SET NULL,
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_reviews_staff    ON reviews(staff_id);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);

-- ─── 13. favorite_staff ───────────────────────────────────
CREATE TABLE favorite_staff (
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    staff_id    UUID NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, staff_id)
);

-- ─── 14. gallery_items ────────────────────────────────────
CREATE TABLE gallery_items (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id  UUID        NOT NULL REFERENCES service_categories(id) ON DELETE RESTRICT,
    title        VARCHAR(200),
    before_url   TEXT        NOT NULL,
    after_url    TEXT        NOT NULL,
    uploaded_by  UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    is_published BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted   BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_gallery_category ON gallery_items(category_id);

-- ─── 15. whatsapp_notifications ───────────────────────────
CREATE TABLE whatsapp_notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient_phone VARCHAR(15)  NOT NULL,
    message_type    VARCHAR(50)  NOT NULL,
    template_name   VARCHAR(100),
    payload         JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    sent_at         TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_whatsapp_status ON whatsapp_notifications(status);
CREATE INDEX idx_whatsapp_phone  ON whatsapp_notifications(recipient_phone);

-- ─── Seed: default admin user (password: Admin@123) ───────
INSERT INTO users (full_name, phone_number, email, password_hash, role)
VALUES (
    'Super Admin',
    '9999999999',
    'admin@salon.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/HS.iK8i',
    'ADMIN'
);

-- ─── Seed: service categories ─────────────────────────────
INSERT INTO service_categories (name, sort_order) VALUES
    ('Hair',     1),
    ('Skin',     2),
    ('Nails',    3),
    ('Makeup',   4),
    ('Massage',  5);
