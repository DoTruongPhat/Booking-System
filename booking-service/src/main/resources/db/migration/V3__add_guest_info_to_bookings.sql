-- V3__add_guest_info_to_bookings.sql
-- Snapshot guest info at booking time for PDF report generation

ALTER TABLE booking.bookings ADD COLUMN guest_name VARCHAR(100);
ALTER TABLE booking.bookings ADD COLUMN guest_email VARCHAR(100);
ALTER TABLE booking.bookings ADD COLUMN guest_phone VARCHAR(20);

COMMENT ON COLUMN booking.bookings.guest_name IS 'Guest name snapshot at booking time';
COMMENT ON COLUMN booking.bookings.guest_email IS 'Guest email snapshot at booking time';
COMMENT ON COLUMN booking.bookings.guest_phone IS 'Guest phone snapshot at booking time';