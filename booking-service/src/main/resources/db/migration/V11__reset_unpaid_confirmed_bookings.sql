UPDATE booking.bookings
SET status = 'PENDING',
    updated_at = NOW()
WHERE status = 'CONFIRMED'
  AND payment_status = 'UNPAID';
