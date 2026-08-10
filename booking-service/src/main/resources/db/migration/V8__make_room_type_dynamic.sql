ALTER TABLE booking.rooms
    DROP CONSTRAINT IF EXISTS chk_room_type;

ALTER TABLE booking.rooms
    ALTER COLUMN room_type TYPE VARCHAR(50);

INSERT INTO booking.room_types (hotel_id, code, name, description, default_capacity, default_amenities, active)
SELECT DISTINCT
    r.hotel_id,
    UPPER(TRIM(r.room_type)),
    INITCAP(REPLACE(LOWER(TRIM(r.room_type)), '_', ' ')),
    'Migrated from existing room type',
    MAX(r.capacity),
    '[]'::jsonb,
    TRUE
FROM booking.rooms r
WHERE r.room_type IS NOT NULL
GROUP BY r.hotel_id, UPPER(TRIM(r.room_type)), INITCAP(REPLACE(LOWER(TRIM(r.room_type)), '_', ' '))
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_rooms_hotel_room_type
    ON booking.rooms(hotel_id, room_type);
