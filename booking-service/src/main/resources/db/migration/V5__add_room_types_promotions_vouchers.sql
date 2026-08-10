CREATE TABLE booking.room_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID REFERENCES booking.hotels(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    default_capacity INT CONSTRAINT chk_room_type_capacity CHECK (default_capacity IS NULL OR default_capacity >= 1),
    default_amenities JSONB DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_room_types_scope_code
    ON booking.room_types (COALESCE(hotel_id, '00000000-0000-0000-0000-000000000000'::uuid), code);
CREATE INDEX idx_room_types_hotel ON booking.room_types(hotel_id);

INSERT INTO booking.room_types (code, name, description, default_capacity)
VALUES
    ('SINGLE', 'Single', 'One guest room type', 1),
    ('DOUBLE', 'Double', 'Two guest room type', 2),
    ('SUITE', 'Suite', 'Premium suite room type', 2),
    ('FAMILY', 'Family', 'Family room type', 4)
ON CONFLICT DO NOTHING;

ALTER TABLE booking.rooms DROP CONSTRAINT IF EXISTS chk_room_type;

CREATE TABLE booking.promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID REFERENCES booking.hotels(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_promotion_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_promotion_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_promotion_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_promotions_hotel ON booking.promotions(hotel_id);
CREATE INDEX idx_promotions_active_dates ON booking.promotions(active, start_date, end_date);

CREATE TABLE booking.vouchers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID REFERENCES booking.hotels(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    max_discount_amount DECIMAL(12,2),
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_voucher_discount_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT chk_voucher_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_voucher_min_order CHECK (min_order_amount IS NULL OR min_order_amount >= 0),
    CONSTRAINT chk_voucher_max_discount CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT chk_voucher_usage_limit CHECK (usage_limit IS NULL OR usage_limit >= 1),
    CONSTRAINT chk_voucher_used_count CHECK (used_count >= 0),
    CONSTRAINT chk_voucher_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_vouchers_hotel ON booking.vouchers(hotel_id);
CREATE INDEX idx_vouchers_active_dates ON booking.vouchers(active, start_date, end_date);
