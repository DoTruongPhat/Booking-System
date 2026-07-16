-- ================================================
-- V2__seed_booking_data.sql
-- Seed Data for Booking Domain
-- ================================================
BEGIN;

-- ================================================
-- STEP 1: INSERT HOTELS
-- ================================================
INSERT INTO booking.hotels (
    owner_user_id, name, description, address, city, country,
    rating, status, amenities, images,
    check_in_time, check_out_time
) VALUES
      (
          '11111111-1111-1111-1111-111111111111',
          'Paradise Hotel',
          'Luxury hotel in Ho Chi Minh City with stunning views and premium amenities',
          '123 Nguyen Hue, District 1',
          'Ho Chi Minh',
          'Vietnam',
          4.8, 'ACTIVE',
          '["Wifi","Pool","Gym","Parking","Spa","Restaurant"]'::jsonb,
          '["https://picsum.photos/800/500?random=1", "https://picsum.photos/800/500?random=2", "https://picsum.photos/800/500?random=3"]'::jsonb,
          '14:00'::time, '12:00'::time
      ),
      (
          '22222222-2222-2222-2222-222222222222',
          'Sunshine Resort',
          'Beautiful beach resort with private beach access',
          '456 Beach Road, Son Tra',
          'Da Nang',
          'Vietnam',
          4.6, 'ACTIVE',
          '["Wifi","Beach","Breakfast","Bar","Pool"]'::jsonb,
          '["https://picsum.photos/800/500?random=4", "https://picsum.photos/800/500?random=5", "https://picsum.photos/800/500?random=6"]'::jsonb,
          '14:00'::time, '12:00'::time
      );

-- ================================================
-- STEP 2: INSERT ROOMS
-- ================================================
-- Paradise Hotel rooms
INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'SINGLE', 'Standard Single Room', 'Cozy single room with city view', 1, 600000.00, 10,
       '["TV","AC","Wifi","Safe"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=10"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Paradise Hotel';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'DOUBLE', 'Deluxe Double Room', 'Spacious double room with premium bedding', 2, 1200000.00, 15,
       '["TV","AC","Wifi","Mini Bar","Safe","Bathtub"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=11"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Paradise Hotel';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'SUITE', 'Executive Suite', 'Luxury suite with living area and city panorama', 4, 2500000.00, 5,
       '["TV","AC","Wifi","Mini Bar","Safe","Bathtub","Living Room","Jacuzzi"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=12", "https://picsum.photos/400/300?random=13"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Paradise Hotel';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'FAMILY', 'Family Room', 'Large room perfect for families with children', 6, 1800000.00, 8,
       '["TV","AC","Wifi","Mini Bar","Safe","Extra Beds"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=14"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Paradise Hotel';

-- Sunshine Resort rooms
INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'SINGLE', 'Beach Single Room', 'Single room with garden view', 1, 550000.00, 8,
       '["TV","AC","Wifi","Balcony"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=20"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Sunshine Resort';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'DOUBLE', 'Ocean View Double', 'Double room with stunning ocean view', 2, 1100000.00, 12,
       '["TV","AC","Wifi","Mini Bar","Balcony","Ocean View"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=21"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Sunshine Resort';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id, 'SUITE', 'Beachfront Suite', 'Premium suite with direct beach access', 4, 2800000.00, 4,
       '["TV","AC","Wifi","Mini Bar","Safe","Beach Access","Private Pool"]'::jsonb, 'AVAILABLE',
       '["https://picsum.photos/400/300?random=22", "https://picsum.photos/400/300?random=23"]'::jsonb
FROM booking.hotels h WHERE h.name = 'Sunshine Resort';

-- ================================================
-- STEP 3: INSERT ROOM AVAILABILITY (30 days)
-- ================================================
INSERT INTO booking.room_availability (room_id, date, available_count, price_override, status)
SELECT r.id, CURRENT_DATE + gs, r.total_rooms, NULL, 'AVAILABLE'
FROM booking.rooms r
         CROSS JOIN generate_series(0, 29) AS gs;

-- ================================================
-- STEP 4: INSERT SAMPLE BOOKINGS
-- ================================================
-- Booking 1: Confirmed at Paradise Hotel (Double)
INSERT INTO booking.bookings (
    booking_code, user_id, room_id, hotel_id,
    check_in_date, check_out_date, num_nights,
    num_guests, num_rooms,
    unit_price, discount_amount, total_price,
    status, payment_status, payment_method, paid_at,
    special_request,
    created_at, updated_at
) VALUES (
             'BK-2024-000001',
             '11111111-1111-1111-1111-111111111111',
             (SELECT id FROM booking.rooms WHERE hotel_id = (SELECT id FROM booking.hotels WHERE name = 'Paradise Hotel') AND room_type = 'DOUBLE' LIMIT 1),
         (SELECT id FROM booking.hotels WHERE name = 'Paradise Hotel'),
            CURRENT_DATE + 5, CURRENT_DATE + 7, 2,
    2, 1,
    1200000.00, 0.00, 2400000.00,
    'CONFIRMED', 'PAID', 'VNPAY', CURRENT_TIMESTAMP - INTERVAL '1 day',
    'Late check-in around 10 PM',
    CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '1 day'
    );

-- Booking 2: Pending at Sunshine Resort (Double)
INSERT INTO booking.bookings (
    booking_code, user_id, room_id, hotel_id,
    check_in_date, check_out_date, num_nights,
    num_guests, num_rooms,
    unit_price, discount_amount, total_price,
    status, payment_status,
    special_request,
    created_at, updated_at
) VALUES (
             'BK-2024-000002',
             '22222222-2222-2222-2222-222222222222',
             (SELECT id FROM booking.rooms WHERE hotel_id = (SELECT id FROM booking.hotels WHERE name = 'Sunshine Resort') AND room_type = 'DOUBLE' LIMIT 1),
         (SELECT id FROM booking.hotels WHERE name = 'Sunshine Resort'),
            CURRENT_DATE + 10, CURRENT_DATE + 13, 3,
    2, 1,
    1100000.00, 100000.00, 3200000.00,
    'PENDING', 'UNPAID',
    'Honeymoon trip - please prepare welcome drink',
            CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour'
    );

-- Booking 3: Completed at Paradise Hotel (Suite)
INSERT INTO booking.bookings (
    booking_code, user_id, room_id, hotel_id,
    check_in_date, check_out_date, num_nights,
    num_guests, num_rooms,
    unit_price, discount_amount, total_price,
    status, payment_status, payment_method, paid_at,
    created_at, updated_at
) VALUES (
             'BK-2024-000003',
             '33333333-3333-3333-3333-333333333333',
             (SELECT id FROM booking.rooms WHERE hotel_id = (SELECT id FROM booking.hotels WHERE name = 'Paradise Hotel') AND room_type = 'SUITE' LIMIT 1),
         (SELECT id FROM booking.hotels WHERE name = 'Paradise Hotel'),
            CURRENT_DATE - 10, CURRENT_DATE - 7, 3,
    4, 1,
    2500000.00, 250000.00, 7250000.00,
    'COMPLETED', 'PAID', 'MOMO', CURRENT_DATE - 12,
            CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '7 days'
    );

-- ================================================
-- STEP 5: UPDATE AVAILABILITY for booked dates
-- ================================================
UPDATE booking.room_availability ra
SET available_count = available_count - 1
    FROM booking.bookings b
WHERE ra.room_id = b.room_id
  AND ra.date >= b.check_in_date
  AND ra.date < b.check_out_date
  AND b.status NOT IN ('CANCELLED', 'NO_SHOW');

-- ================================================
-- VERIFICATION
-- ================================================
DO $$
BEGIN
    RAISE NOTICE '=== Seed Data Summary ===';
    RAISE NOTICE 'Hotels: %', (SELECT COUNT(*) FROM booking.hotels);
    RAISE NOTICE 'Rooms: %', (SELECT COUNT(*) FROM booking.rooms);
    RAISE NOTICE 'Availability records: %', (SELECT COUNT(*) FROM booking.room_availability);
    RAISE NOTICE 'Bookings: %', (SELECT COUNT(*) FROM booking.bookings);
END $$;

COMMIT;