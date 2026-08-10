ALTER TABLE booking.bookings
    ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_bookings_voucher_code
    ON booking.bookings(voucher_code)
    WHERE voucher_code IS NOT NULL;
