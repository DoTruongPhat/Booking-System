ALTER TABLE booking.bookings
    DROP CONSTRAINT IF EXISTS chk_booking_status;

ALTER TABLE booking.bookings
    ADD CONSTRAINT chk_booking_status CHECK (status IN (
        'PENDING',
        'CONFIRMED',
        'CHECKED_IN',
        'COMPLETED',
        'CANCELLED',
        'NO_SHOW'
    ));
