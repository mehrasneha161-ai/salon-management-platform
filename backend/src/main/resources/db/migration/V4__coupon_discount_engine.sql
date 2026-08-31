-- ============================================================
-- V4 — Coupon discount engine and immutable booking snapshots
-- ============================================================

CREATE TABLE coupons (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code                  VARCHAR(50)    NOT NULL,
    normalized_code       VARCHAR(50)    NOT NULL UNIQUE,
    name                  VARCHAR(150)   NOT NULL,
    description           TEXT,
    discount_type         VARCHAR(20)    NOT NULL,
    discount_value        DECIMAL(10,2)  NOT NULL,
    minimum_spend         DECIMAL(10,2)  NOT NULL DEFAULT 0,
    maximum_discount      DECIMAL(10,2),
    valid_from            TIMESTAMPTZ    NOT NULL,
    valid_until           TIMESTAMPTZ    NOT NULL,
    usage_limit           INTEGER,
    per_customer_limit    INTEGER,
    outlet_id             UUID REFERENCES outlets(id) ON DELETE RESTRICT,
    service_id            UUID REFERENCES services(id) ON DELETE RESTRICT,
    package_id            UUID REFERENCES packages(id) ON DELETE RESTRICT,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted            BOOLEAN        NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_coupons_code_not_blank
        CHECK (BTRIM(code) <> '' AND BTRIM(normalized_code) <> ''),
    CONSTRAINT chk_coupons_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    CONSTRAINT chk_coupons_discount_value
        CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_percentage_value
        CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT chk_coupons_minimum_spend
        CHECK (minimum_spend >= 0),
    CONSTRAINT chk_coupons_maximum_discount
        CHECK (maximum_discount IS NULL OR maximum_discount > 0),
    CONSTRAINT chk_coupons_usage_limit
        CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT chk_coupons_per_customer_limit
        CHECK (per_customer_limit IS NULL OR per_customer_limit > 0),
    CONSTRAINT chk_coupons_validity_window
        CHECK (valid_until > valid_from),
    CONSTRAINT chk_coupons_single_item_scope
        CHECK (service_id IS NULL OR package_id IS NULL)
);

CREATE INDEX idx_coupons_active ON coupons(is_active, is_deleted);
CREATE INDEX idx_coupons_outlet ON coupons(outlet_id) WHERE outlet_id IS NOT NULL;
CREATE INDEX idx_coupons_service ON coupons(service_id) WHERE service_id IS NOT NULL;
CREATE INDEX idx_coupons_package ON coupons(package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_coupons_validity ON coupons(valid_from, valid_until);

-- Preserve existing bookings by first adding a nullable subtotal, backfilling it
-- from the current trusted total, and only then making it mandatory.
ALTER TABLE bookings ADD COLUMN subtotal_amount DECIMAL(10,2);
UPDATE bookings SET subtotal_amount = total_amount;
ALTER TABLE bookings ALTER COLUMN subtotal_amount SET NOT NULL;

ALTER TABLE bookings ADD COLUMN slot_locked_at TIMESTAMPTZ;
UPDATE bookings SET slot_locked_at = COALESCE(updated_at, created_at);
ALTER TABLE bookings ALTER COLUMN slot_locked_at SET NOT NULL;

ALTER TABLE bookings
    ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN coupon_id UUID REFERENCES coupons(id) ON DELETE SET NULL,
    ADD COLUMN coupon_code VARCHAR(50),
    ADD COLUMN coupon_discount_type VARCHAR(20),
    ADD COLUMN coupon_discount_value DECIMAL(10,2),
    ADD COLUMN coupon_maximum_discount DECIMAL(10,2),
    ADD CONSTRAINT chk_bookings_pricing_nonnegative
        CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND total_amount >= 0),
    ADD CONSTRAINT chk_bookings_discount_within_subtotal
        CHECK (discount_amount <= subtotal_amount),
    ADD CONSTRAINT chk_bookings_pricing_arithmetic
        CHECK (total_amount = subtotal_amount - discount_amount),
    ADD CONSTRAINT chk_bookings_coupon_snapshot_type
        CHECK (coupon_discount_type IS NULL OR coupon_discount_type IN ('PERCENTAGE', 'FIXED')),
    ADD CONSTRAINT chk_bookings_coupon_snapshot_value
        CHECK (coupon_discount_value IS NULL OR (
            coupon_discount_value > 0
            AND (coupon_discount_type <> 'PERCENTAGE' OR coupon_discount_value <= 100)
        )),
    ADD CONSTRAINT chk_bookings_coupon_snapshot_cap
        CHECK (coupon_maximum_discount IS NULL OR coupon_maximum_discount > 0),
    ADD CONSTRAINT chk_bookings_coupon_snapshot_complete
        CHECK (
            (coupon_code IS NULL
                AND coupon_discount_type IS NULL
                AND coupon_discount_value IS NULL
                AND coupon_maximum_discount IS NULL
                AND coupon_id IS NULL)
            OR
            (coupon_code IS NOT NULL
                AND coupon_discount_type IS NOT NULL
                AND coupon_discount_value IS NOT NULL)
        );

CREATE INDEX idx_bookings_coupon ON bookings(coupon_id) WHERE coupon_id IS NOT NULL;

ALTER TABLE payments
    ADD COLUMN reconciliation_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reconciliation_reason VARCHAR(500);

CREATE TABLE coupon_redemptions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    coupon_id         UUID           NOT NULL REFERENCES coupons(id) ON DELETE RESTRICT,
    customer_id       UUID           NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    booking_id        UUID           NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE RESTRICT,
    status            VARCHAR(20)    NOT NULL,
    discount_amount   DECIMAL(10,2)  NOT NULL,
    reserved_at       TIMESTAMPTZ,
    redeemed_at       TIMESTAMPTZ,
    released_at       TIMESTAMPTZ,
    release_reason    VARCHAR(500),
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    is_deleted        BOOLEAN        NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_coupon_redemptions_status
        CHECK (status IN ('RESERVED', 'REDEEMED', 'RELEASED')),
    CONSTRAINT chk_coupon_redemptions_discount
        CHECK (discount_amount >= 0),
    CONSTRAINT chk_coupon_redemptions_timestamps
        CHECK (
            (status = 'RESERVED' AND reserved_at IS NOT NULL AND redeemed_at IS NULL AND released_at IS NULL)
            OR (status = 'REDEEMED' AND reserved_at IS NOT NULL AND redeemed_at IS NOT NULL AND released_at IS NULL)
            OR (status = 'RELEASED' AND reserved_at IS NOT NULL AND redeemed_at IS NULL AND released_at IS NOT NULL)
        )
);

CREATE INDEX idx_coupon_redemptions_coupon_status
    ON coupon_redemptions(coupon_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_coupon_redemptions_coupon_customer_status
    ON coupon_redemptions(coupon_id, customer_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_coupon_redemptions_customer
    ON coupon_redemptions(customer_id) WHERE is_deleted = FALSE;
