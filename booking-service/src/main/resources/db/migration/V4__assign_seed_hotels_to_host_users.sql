-- V4__assign_seed_hotels_to_host_users.sql
-- Map demo hotels from placeholder owner ids to real HOST users.

DO $$
DECLARE
    first_host_id UUID;
    second_host_id UUID;
BEGIN
    SELECT host_user.id
    INTO first_host_id
    FROM (
        SELECT u.id, ROW_NUMBER() OVER (ORDER BY u.created_at, u.username) AS rn
        FROM auth.users u
                 JOIN auth.user_roles ur ON ur.user_id = u.id
                 JOIN auth.roles r ON r.id = ur.role_id
        WHERE r.code = 'HOST'
          AND u.is_active = TRUE
    ) host_user
    WHERE host_user.rn = 1;

    SELECT host_user.id
    INTO second_host_id
    FROM (
        SELECT u.id, ROW_NUMBER() OVER (ORDER BY u.created_at, u.username) AS rn
        FROM auth.users u
                 JOIN auth.user_roles ur ON ur.user_id = u.id
                 JOIN auth.roles r ON r.id = ur.role_id
        WHERE r.code = 'HOST'
          AND u.is_active = TRUE
    ) host_user
    WHERE host_user.rn = 2;

    IF first_host_id IS NOT NULL THEN
        UPDATE booking.hotels
        SET owner_user_id = first_host_id
        WHERE name = 'Paradise Hotel'
          AND owner_user_id = '11111111-1111-1111-1111-111111111111';

        UPDATE booking.hotels
        SET owner_user_id = COALESCE(second_host_id, first_host_id)
        WHERE name = 'Sunshine Resort'
          AND owner_user_id = '22222222-2222-2222-2222-222222222222';
    END IF;
END $$;
