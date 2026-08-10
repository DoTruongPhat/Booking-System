-- Replace placeholder demo hotels with real-world demo properties.
-- Keep existing ids/foreign keys so old bookings and host ownership remain valid.

BEGIN;

UPDATE booking.hotels
SET name = 'Me Saigon Boutique Hotel',
    description = 'Boutique hotel in the heart of District 1, close to Ben Thanh Market, with rooftop pool, spa services, restaurant, fitness facilities, and modern city-view rooms.',
    address = '280 Le Thanh Ton, Ben Thanh Ward, District 1',
    city = 'Ho Chi Minh',
    country = 'Vietnam',
    rating = 5.0,
    status = 'ACTIVE',
    amenities = '["Free WiFi","Outdoor swimming pool","Spa and wellness center","Fitness center","Restaurant","Bar","Room service","Airport shuttle","Family rooms","Breakfast"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/e85adde5.jpg?impolicy=resizecrop&ra=fit&rw=598",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/af55cffc.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/ec16ced3.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/485be2ed.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/bd9a9dc4.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/w2999h2308x0y0-9a268e42.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://content.r9cdn.net/rimg/himg/3c/ba/ea/expedia_group-2013698-23976852-206538.jpg?width=1200&height=630&crop=true"
    ]'::jsonb,
    check_in_time = '14:00'::time,
    check_out_time = '12:00'::time,
    updated_at = NOW()
WHERE name = 'Paradise Hotel';

UPDATE booking.hotels
SET name = 'Vinpearl Wonderworld Phu Quoc',
    description = 'Luxury beachfront resort in Bai Dai, Phu Quoc with private villas, private beach area, outdoor pools, spa, fitness center, restaurants, airport shuttle, and family-friendly facilities.',
    address = 'Bai Dai Zone, Ganh Dau Commune',
    city = 'Phu Quoc',
    country = 'Vietnam',
    rating = 5.0,
    status = 'ACTIVE',
    amenities = '["Free WiFi","Beachfront","Private beach area","Outdoor swimming pool","Spa and wellness center","Fitness center","Airport shuttle","Restaurant","Bar","Kids club","Private pool","Breakfast"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f865e902.jpg?impolicy=resizecrop&ra=fit&rw=598",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/121fbcc7.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/c715ffee.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f98046fb.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/d8a79e4b.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/00a62faa.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/7dd4bff5.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://content.r9cdn.net/rimg/himg/cc/f6/cb/expedia_group-4657226-70268270-553145.jpg?width=1200&height=630&crop=true"
    ]'::jsonb,
    check_in_time = '15:00'::time,
    check_out_time = '12:00'::time,
    updated_at = NOW()
WHERE name = 'Sunshine Resort';

-- Pin demo hotel ownership to the intended HOST accounts.
-- This keeps the old Paradise/Sunshine ownership flow but makes it explicit by email.
UPDATE booking.hotels h
SET owner_user_id = u.id,
    updated_at = NOW()
FROM auth.users u
JOIN auth.user_roles ur ON ur.user_id = u.id
JOIN auth.roles r ON r.id = ur.role_id
WHERE LOWER(TRIM(u.email)) = 'phatdo2901@gmail.com'
  AND r.code = 'HOST'
  AND u.is_active = TRUE
  AND h.name = 'Me Saigon Boutique Hotel';

UPDATE booking.hotels h
SET owner_user_id = u.id,
    updated_at = NOW()
FROM auth.users u
JOIN auth.user_roles ur ON ur.user_id = u.id
JOIN auth.roles r ON r.id = ur.role_id
WHERE LOWER(TRIM(u.email)) = 'tphat6208@gmail.com'
  AND r.code = 'HOST'
  AND u.is_active = TRUE
  AND h.name = 'Vinpearl Wonderworld Phu Quoc';

-- Me Saigon Boutique Hotel rooms, mapped over the original Paradise Hotel demo room ids.
UPDATE booking.rooms r
SET room_type = 'SUPERIOR_DOUBLE',
    name = 'Superior Double City View Room',
    description = 'Compact city-view room with double bed, air conditioning, minibar, flat-screen TV, desk, safe, and free WiFi.',
    capacity = 2,
    base_price = 1500000.00,
    total_rooms = 10,
    amenities = '["Free WiFi","Air conditioning","Minibar","Flat-screen TV","Desk","Safe","Bathrobes","Hair dryer","City view"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/ec16ced3.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/w2999h2308x0y0-9a268e42.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/485be2ed.jpg?h=800&impolicy=fcrop&quality=medium&w=1200"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Me Saigon Boutique Hotel'
  AND r.name = 'Standard Single Room';

UPDATE booking.rooms r
SET room_type = 'PREMIER_DOUBLE',
    name = 'Premier Double Room',
    description = 'Modern king-bed room with minibar, in-room safe, desk, laptop workspace, bathtub, and free WiFi.',
    capacity = 3,
    base_price = 1800000.00,
    total_rooms = 12,
    amenities = '["Free WiFi","King bed","Air conditioning","Minibar","In-room safe","Desk","Bathtub","Hair dryer","Bathrobes"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/w2999h2308x0y0-9a268e42.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/485be2ed.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/e85adde5.jpg?impolicy=resizecrop&ra=fit&rw=598"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Me Saigon Boutique Hotel'
  AND r.name = 'Deluxe Double Room';

UPDATE booking.rooms r
SET room_type = 'ME_SIGNATURE_SUITE',
    name = 'ME Signature Suite High-Floor Balcony',
    description = 'High-floor signature suite with balcony, king bed, seating area, minibar, workspace, city view, and premium bathroom amenities.',
    capacity = 3,
    base_price = 3400000.00,
    total_rooms = 5,
    amenities = '["Free WiFi","Balcony","City view","King bed","Seating area","Minibar","In-room safe","Workspace","Bathtub","Bathrobes"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/bd9a9dc4.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/af55cffc.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://content.r9cdn.net/rimg/himg/3c/ba/ea/expedia_group-2013698-23976852-206538.jpg?width=1200&height=630&crop=true"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Me Saigon Boutique Hotel'
  AND r.name = 'Executive Suite';

UPDATE booking.rooms r
SET room_type = 'FAMILY_PREMIUM_SUITE',
    name = 'Family Premium Connecting Suite City View',
    description = 'Connecting family suite with city view, one king bed and one queen bed, living space, minibar, safe, and free WiFi.',
    capacity = 4,
    base_price = 4600000.00,
    total_rooms = 4,
    amenities = '["Free WiFi","City view","King bed","Queen bed","Connecting rooms","Living area","Minibar","In-room safe","Family rooms"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/af55cffc.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/ec16ced3.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/7000000/6850000/6840800/6840786/bd9a9dc4.jpg?h=800&impolicy=fcrop&quality=medium&w=1200"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Me Saigon Boutique Hotel'
  AND r.name = 'Family Room';

-- Vinpearl Wonderworld Phu Quoc rooms, mapped over the original Sunshine Resort demo room ids.
UPDATE booking.rooms r
SET room_type = 'VILLA_1_BEDROOM',
    name = 'Villa, 1 Bedroom',
    description = 'Garden-view villa with king bed or twin beds, separate living and dining areas, private bathroom, terrace, air conditioning, and free WiFi.',
    capacity = 2,
    base_price = 4500000.00,
    total_rooms = 6,
    amenities = '["Free WiFi","Garden view","Terrace","Air conditioning","Flat-screen TV","Living room","Dining area","Electric kettle","Bathrobes"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/00a62faa.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f98046fb.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f865e902.jpg?impolicy=resizecrop&ra=fit&rw=598"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Vinpearl Wonderworld Phu Quoc'
  AND r.name = 'Beach Single Room';

UPDATE booking.rooms r
SET room_type = 'VILLA_2BR_POOL',
    name = 'Two Bedroom Villa - Private Pool',
    description = 'Lake-view two-bedroom villa with private pool, king bed, twin beds, balcony or terrace, living room, dining area, and free WiFi.',
    capacity = 8,
    base_price = 6500000.00,
    total_rooms = 8,
    amenities = '["Free WiFi","Private pool","Lake view","Balcony","Terrace","King bed","Twin beds","Living room","Dining area","Flat-screen TV"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/d8a79e4b.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/7dd4bff5.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/121fbcc7.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://content.r9cdn.net/rimg/himg/cc/f6/cb/expedia_group-4657226-70268270-553145.jpg?width=1200&height=630&crop=true"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Vinpearl Wonderworld Phu Quoc'
  AND r.name = 'Ocean View Double';

UPDATE booking.rooms r
SET room_type = 'VILLA_3BR_POOL',
    name = 'Three Bedroom Villa - Private Pool',
    description = 'Spacious three-bedroom lake-view villa with private pool, two king beds, two single beds, terrace, living room, and family-friendly amenities.',
    capacity = 12,
    base_price = 9800000.00,
    total_rooms = 6,
    amenities = '["Free WiFi","Private pool","Lake view","Terrace","Two king beds","Two single beds","Living room","Dining area","Kids friendly","Flat-screen TV"]'::jsonb,
    images = '[
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/7dd4bff5.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/c715ffee.jpg?impolicy=resizecrop&ra=fit&rw=297",
      "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/d8a79e4b.jpg?h=800&impolicy=fcrop&quality=medium&w=1200"
    ]'::jsonb,
    updated_at = NOW()
FROM booking.hotels h
WHERE r.hotel_id = h.id
  AND h.name = 'Vinpearl Wonderworld Phu Quoc'
  AND r.name = 'Beachfront Suite';

INSERT INTO booking.rooms (hotel_id, room_type, name, description, capacity, base_price, total_rooms, amenities, status, images)
SELECT h.id,
       'VILLA_4BR_POOL',
       'Four Bedroom Villa - Private Pool',
       'Large four-bedroom villa with private pool, lake or garden view, three king beds, two single beds, dining area, terrace, and free WiFi.',
       16,
       12500000.00,
       4,
       '["Free WiFi","Private pool","Lake view","Garden view","Terrace","Three king beds","Two single beds","Dining area","Living room","Flat-screen TV"]'::jsonb,
       'AVAILABLE',
       '[
         "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f98046fb.jpg?h=800&impolicy=fcrop&quality=medium&w=1200",
         "https://images.trvl-media.com/lodging/35000000/34020000/34014200/34014178/f865e902.jpg?impolicy=resizecrop&ra=fit&rw=598",
         "https://content.r9cdn.net/rimg/himg/cc/f6/cb/expedia_group-4657226-70268270-553145.jpg?width=1200&height=630&crop=true"
       ]'::jsonb
FROM booking.hotels h
WHERE h.name = 'Vinpearl Wonderworld Phu Quoc'
  AND NOT EXISTS (
      SELECT 1
      FROM booking.rooms existing
      WHERE existing.hotel_id = h.id
        AND existing.room_type = 'VILLA_4BR_POOL'
  );

INSERT INTO booking.room_types (hotel_id, code, name, description, default_capacity, default_amenities)
SELECT h.id, rt.code, rt.name, rt.description, rt.default_capacity, rt.default_amenities
FROM booking.hotels h
CROSS JOIN (
    VALUES
        ('SUPERIOR_DOUBLE', 'Superior Double City View Room', 'City-view boutique double room', 2, '["Free WiFi","Air conditioning","Minibar","City view"]'::jsonb),
        ('PREMIER_DOUBLE', 'Premier Double Room', 'King-bed premier room with workspace', 3, '["Free WiFi","King bed","Minibar","Workspace"]'::jsonb),
        ('ME_SIGNATURE_SUITE', 'ME Signature Suite High-Floor Balcony', 'High-floor suite with balcony and city view', 3, '["Free WiFi","Balcony","City view","Seating area"]'::jsonb),
        ('FAMILY_PREMIUM_SUITE', 'Family Premium Connecting Suite City View', 'Connecting family suite with city view', 4, '["Free WiFi","Connecting rooms","Family rooms","City view"]'::jsonb)
) AS rt(code, name, description, default_capacity, default_amenities)
WHERE h.name = 'Me Saigon Boutique Hotel'
ON CONFLICT DO NOTHING;

INSERT INTO booking.room_types (hotel_id, code, name, description, default_capacity, default_amenities)
SELECT h.id, rt.code, rt.name, rt.description, rt.default_capacity, rt.default_amenities
FROM booking.hotels h
CROSS JOIN (
    VALUES
        ('VILLA_1_BEDROOM', 'Villa, 1 Bedroom', 'Garden-view private villa', 2, '["Free WiFi","Garden view","Terrace","Living room"]'::jsonb),
        ('VILLA_2BR_POOL', 'Two Bedroom Villa - Private Pool', 'Two-bedroom villa with private pool', 8, '["Free WiFi","Private pool","Lake view","Terrace"]'::jsonb),
        ('VILLA_3BR_POOL', 'Three Bedroom Villa - Private Pool', 'Three-bedroom private-pool villa', 12, '["Free WiFi","Private pool","Lake view","Family friendly"]'::jsonb),
        ('VILLA_4BR_POOL', 'Four Bedroom Villa - Private Pool', 'Four-bedroom private-pool villa', 16, '["Free WiFi","Private pool","Lake view","Dining area"]'::jsonb)
) AS rt(code, name, description, default_capacity, default_amenities)
WHERE h.name = 'Vinpearl Wonderworld Phu Quoc'
ON CONFLICT DO NOTHING;

INSERT INTO booking.room_availability (room_id, date, available_count, price_override, status)
SELECT r.id, CURRENT_DATE + gs, r.total_rooms, NULL, 'AVAILABLE'
FROM booking.rooms r
JOIN booking.hotels h ON h.id = r.hotel_id
CROSS JOIN generate_series(0, 89) AS gs
WHERE h.name IN ('Me Saigon Boutique Hotel', 'Vinpearl Wonderworld Phu Quoc')
ON CONFLICT (room_id, date) DO NOTHING;

UPDATE booking.room_availability ra
SET available_count = LEAST(ra.available_count, r.total_rooms),
    updated_at = NOW()
FROM booking.rooms r
JOIN booking.hotels h ON h.id = r.hotel_id
WHERE ra.room_id = r.id
  AND h.name IN ('Me Saigon Boutique Hotel', 'Vinpearl Wonderworld Phu Quoc')
  AND ra.date >= CURRENT_DATE;

UPDATE booking.bookings
SET unit_price = 1800000.00,
    total_price = 3600000.00,
    payment_method = 'PAYOS',
    updated_at = NOW()
WHERE booking_code = 'BK-2024-000001';

UPDATE booking.bookings
SET unit_price = 6500000.00,
    total_price = 19400000.00,
    updated_at = NOW()
WHERE booking_code = 'BK-2024-000002';

UPDATE booking.bookings
SET unit_price = 3400000.00,
    total_price = 9950000.00,
    payment_method = 'PAYOS',
    updated_at = NOW()
WHERE booking_code = 'BK-2024-000003';

INSERT INTO booking.promotions (hotel_id, title, description, discount_type, discount_value, start_date, end_date, active)
SELECT h.id,
       'Saigon City Break',
       'Special boutique stay offer for central District 1 rooms.',
       'PERCENT',
       12,
       CURRENT_DATE,
       CURRENT_DATE + 60,
       TRUE
FROM booking.hotels h
WHERE h.name = 'Me Saigon Boutique Hotel'
  AND NOT EXISTS (
      SELECT 1 FROM booking.promotions p
      WHERE p.hotel_id = h.id AND p.title = 'Saigon City Break'
  );

INSERT INTO booking.promotions (hotel_id, title, description, discount_type, discount_value, start_date, end_date, active)
SELECT h.id,
       'Phu Quoc Villa Escape',
       'Limited resort villa offer for beach and private-pool stays.',
       'PERCENT',
       15,
       CURRENT_DATE,
       CURRENT_DATE + 60,
       TRUE
FROM booking.hotels h
WHERE h.name = 'Vinpearl Wonderworld Phu Quoc'
  AND NOT EXISTS (
      SELECT 1 FROM booking.promotions p
      WHERE p.hotel_id = h.id AND p.title = 'Phu Quoc Villa Escape'
  );

INSERT INTO booking.vouchers (hotel_id, code, description, discount_type, discount_value, min_order_amount, max_discount_amount, usage_limit, start_date, end_date, active)
SELECT h.id, 'MESAIGON10', '10 percent off Me Saigon Boutique Hotel bookings.', 'PERCENT', 10, 1000000.00, 500000.00, 100, CURRENT_DATE, CURRENT_DATE + 90, TRUE
FROM booking.hotels h
WHERE h.name = 'Me Saigon Boutique Hotel'
ON CONFLICT (code) DO UPDATE
SET hotel_id = EXCLUDED.hotel_id,
    description = EXCLUDED.description,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    min_order_amount = EXCLUDED.min_order_amount,
    max_discount_amount = EXCLUDED.max_discount_amount,
    usage_limit = EXCLUDED.usage_limit,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO booking.vouchers (hotel_id, code, description, discount_type, discount_value, min_order_amount, max_discount_amount, usage_limit, start_date, end_date, active)
SELECT h.id, 'VINPEARL15', '15 percent off Vinpearl Wonderworld Phu Quoc villa bookings.', 'PERCENT', 15, 3000000.00, 1500000.00, 100, CURRENT_DATE, CURRENT_DATE + 90, TRUE
FROM booking.hotels h
WHERE h.name = 'Vinpearl Wonderworld Phu Quoc'
ON CONFLICT (code) DO UPDATE
SET hotel_id = EXCLUDED.hotel_id,
    description = EXCLUDED.description,
    discount_type = EXCLUDED.discount_type,
    discount_value = EXCLUDED.discount_value,
    min_order_amount = EXCLUDED.min_order_amount,
    max_discount_amount = EXCLUDED.max_discount_amount,
    usage_limit = EXCLUDED.usage_limit,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    active = EXCLUDED.active,
    updated_at = NOW();

COMMIT;
