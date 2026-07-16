-- ═══════════════════════════════════════════════════════════
-- V1__create_core_tables.sql
-- Core Service: Hotels, Rooms, RoomAvailability, Bookings
-- ═══════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS booking;

-- ─── HOTELS ────────────────────────────────
CREATE TABLE booking.hotels (
                                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                owner_user_id   UUID            NOT NULL,
                                name            VARCHAR(255)    NOT NULL,
                                description     TEXT,
                                address         VARCHAR(500)    NOT NULL,
                                city            VARCHAR(100)    NOT NULL,
                                country         VARCHAR(100)    NOT NULL,
                                rating          DECIMAL(2,1)    DEFAULT 0.0
                                    CONSTRAINT chk_hotel_rating CHECK (rating >= 0.0 AND rating <= 5.0),
                                status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING_APPROVAL',
                                amenities       JSONB           DEFAULT '[]'::jsonb,
                                check_in_time   TIME            DEFAULT '14:00',
                                check_out_time  TIME            DEFAULT '12:00',
                                images          JSONB           DEFAULT '[]'::jsonb,
                                created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                CONSTRAINT uq_hotel_name_city UNIQUE (name, city),
                                CONSTRAINT chk_hotel_status CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_hotels_owner    ON booking.hotels(owner_user_id);
CREATE INDEX idx_hotels_city     ON booking.hotels(city);
CREATE INDEX idx_hotels_status   ON booking.hotels(status);

-- ─── ROOMS ─────────────────────────────────
CREATE TABLE booking.rooms (
                               id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               hotel_id        UUID            NOT NULL
                                   REFERENCES booking.hotels(id) ON DELETE RESTRICT,
                               room_type       VARCHAR(20)     NOT NULL,
                               name            VARCHAR(255)    NOT NULL,
                               description     TEXT,
                               capacity        INT             NOT NULL
                                   CONSTRAINT chk_room_capacity CHECK (capacity >= 1),
                               base_price      DECIMAL(12,2)   NOT NULL
                                   CONSTRAINT chk_room_price CHECK (base_price > 0),
                               total_rooms     INT             NOT NULL
                                   CONSTRAINT chk_room_total CHECK (total_rooms >= 1),
                               amenities       JSONB           DEFAULT '[]'::jsonb,
                               status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
                               images          JSONB           DEFAULT '[]'::jsonb,
                               created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                               updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                               CONSTRAINT chk_room_type CHECK (room_type IN ('SINGLE', 'DOUBLE', 'SUITE', 'FAMILY')),
                               CONSTRAINT chk_room_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'INACTIVE'))
);

CREATE INDEX idx_rooms_hotel    ON booking.rooms(hotel_id);
CREATE INDEX idx_rooms_status   ON booking.rooms(status);

-- ─── ROOM AVAILABILITY ────────────────────
CREATE TABLE booking.room_availability (
                                           id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           room_id         UUID            NOT NULL
                                               REFERENCES booking.rooms(id) ON DELETE CASCADE,
                                           date            DATE            NOT NULL,
                                           available_count INT             NOT NULL
                                               CONSTRAINT chk_avail_count CHECK (available_count >= 0),
                                           price_override  DECIMAL(12,2)
                                               CONSTRAINT chk_avail_price CHECK (price_override IS NULL OR price_override >= 0),
                                           status          VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
                                           created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                           updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                           CONSTRAINT uq_room_date UNIQUE (room_id, date),
                                           CONSTRAINT chk_avail_status CHECK (status IN ('AVAILABLE', 'BLOCKED'))
);

CREATE INDEX idx_avail_room_date ON booking.room_availability(room_id, date);
CREATE INDEX idx_avail_date      ON booking.room_availability(date);

-- ─── BOOKINGS ──────────────────────────────
CREATE TABLE booking.bookings (
                                  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  booking_code        VARCHAR(50)     NOT NULL,
                                  user_id             UUID            NOT NULL,
                                  room_id             UUID            NOT NULL
                                      REFERENCES booking.rooms(id) ON DELETE RESTRICT,
                                  hotel_id            UUID            NOT NULL
                                      REFERENCES booking.hotels(id) ON DELETE RESTRICT,

    -- Dates
                                  check_in_date       DATE            NOT NULL,
                                  check_out_date      DATE            NOT NULL,
                                  num_nights          INT             NOT NULL
                                      CONSTRAINT chk_booking_nights CHECK (num_nights >= 1),

    -- Guests & Rooms
                                  num_guests          INT             NOT NULL
                                      CONSTRAINT chk_booking_guests CHECK (num_guests >= 1),
                                  num_rooms           INT             NOT NULL DEFAULT 1
                                      CONSTRAINT chk_booking_rooms CHECK (num_rooms >= 1),

    -- Price Snapshot
                                  unit_price          DECIMAL(12,2)   NOT NULL
                                      CONSTRAINT chk_booking_unit_price CHECK (unit_price >= 0),
                                  discount_amount     DECIMAL(12,2)   DEFAULT 0
                                      CONSTRAINT chk_booking_discount CHECK (discount_amount >= 0),
                                  total_price         DECIMAL(12,2)   NOT NULL
                                      CONSTRAINT chk_booking_total CHECK (total_price >= 0),

    -- Booking Status
                                  status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Payment (embedded — tách payment service sau)
                                  payment_status      VARCHAR(25)     NOT NULL DEFAULT 'UNPAID',
                                  payment_method      VARCHAR(50),
                                  paid_at             TIMESTAMPTZ,
                                  refund_amount       DECIMAL(12,2)
                                      CONSTRAINT chk_booking_refund CHECK (refund_amount IS NULL OR refund_amount >= 0),

    -- Cancellation
                                  cancellation_reason TEXT,
                                  cancelled_at        TIMESTAMPTZ,
                                  cancelled_by        VARCHAR(20),

    -- Extra
                                  special_request     TEXT,

    -- Timestamps
                                  created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Constraints
                                  CONSTRAINT uq_booking_code UNIQUE (booking_code),
                                  CONSTRAINT chk_checkout_after_checkin CHECK (check_out_date > check_in_date),
                                  CONSTRAINT chk_booking_status CHECK (status IN (
                                                                                  'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
                                      )),
                                  CONSTRAINT chk_payment_status CHECK (payment_status IN (
                                                                                          'UNPAID', 'PAID', 'REFUNDED', 'PARTIALLY_REFUNDED'
                                      )),
                                  CONSTRAINT chk_cancelled_by CHECK (cancelled_by IS NULL OR cancelled_by IN ('USER', 'HOST', 'SYSTEM'))
);

CREATE INDEX idx_bookings_user     ON booking.bookings(user_id);
CREATE INDEX idx_bookings_room     ON booking.bookings(room_id);
CREATE INDEX idx_bookings_hotel    ON booking.bookings(hotel_id);
CREATE INDEX idx_bookings_status   ON booking.bookings(status);
CREATE INDEX idx_bookings_code     ON booking.bookings(booking_code);
CREATE INDEX idx_bookings_dates    ON booking.bookings(check_in_date, check_out_date);