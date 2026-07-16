    -- V9__update_roles_and_phone.sql
    -- ============================================================
    -- PHASE 1: TẠO PERMISSIONS MỚI
    -- ============================================================
    INSERT INTO auth.permissions (id, code, name, resource, action, description) VALUES
     (gen_random_uuid(), 'ROOM_CREATE', 'Tạo phòng', 'ROOM', 'CREATE', 'Tạo phòng mới'),
     (gen_random_uuid(), 'ROOM_READ', 'Xem phòng', 'ROOM', 'READ', 'Xem thông tin phòng'),
     (gen_random_uuid(), 'ROOM_UPDATE', 'Cập nhật phòng', 'ROOM', 'UPDATE', 'Cập nhật phòng, giá, ảnh, status, vô hiệu hóa'),
     (gen_random_uuid(), 'HOTEL_CREATE', 'Tạo khách sạn', 'HOTEL', 'CREATE', 'Tạo khách sạn mới'),
     (gen_random_uuid(), 'HOTEL_READ', 'Xem khách sạn', 'HOTEL', 'READ', 'Xem thông tin khách sạn'),
     (gen_random_uuid(), 'HOTEL_UPDATE', 'Cập nhật khách sạn', 'HOTEL', 'UPDATE', 'Cập nhật thông tin khách sạn'),
     (gen_random_uuid(), 'HOTEL_DELETE', 'Vô hiệu khách sạn', 'HOTEL', 'DELETE', 'Vô hiệu hóa khách sạn'),
     (gen_random_uuid(), 'PAYMENT_CREATE', 'Tạo thanh toán', 'PAYMENT', 'CREATE', 'Tạo thanh toán'),
     (gen_random_uuid(), 'PAYMENT_READ', 'Xem thanh toán', 'PAYMENT', 'READ', 'Xem thanh toán'),
     (gen_random_uuid(), 'PAYMENT_UPDATE', 'Xác nhận thanh toán', 'PAYMENT', 'UPDATE', 'Xác nhận / refund thanh toán'),
     (gen_random_uuid(), 'TICKET_CREATE', 'Tạo ticket', 'TICKET', 'CREATE', 'Tạo ticket hỗ trợ'),
     (gen_random_uuid(), 'TICKET_READ', 'Xem ticket', 'TICKET', 'READ', 'Xem ticket hỗ trợ'),
     (gen_random_uuid(), 'TICKET_UPDATE', 'Phản hồi ticket', 'TICKET', 'UPDATE', 'Phản hồi ticket, đổi status'),
     (gen_random_uuid(), 'TICKET_ASSIGN', 'Gán ticket', 'TICKET', 'ASSIGN', 'Gán ticket cho HOST')
    ON CONFLICT (code) DO NOTHING;

    -- ============================================================
    -- PHASE 2: TẠO ROLE HOST (nếu chưa có)
    -- ============================================================
    INSERT INTO auth.roles (id, code, name, description)
    SELECT gen_random_uuid(), 'HOST', 'Chủ khách sạn', 'Quản lý phòng, booking, thanh toán'
    WHERE NOT EXISTS (SELECT 1 FROM auth.roles WHERE code = 'HOST');

    -- ============================================================
    -- PHASE 3: GÁN 15 PERMISSIONS CHO HOST
    -- ============================================================
    INSERT INTO auth.role_permissions (role_id, permission_id)
    SELECT
     (SELECT id FROM auth.roles WHERE code = 'HOST'),
     p.id
    FROM auth.permissions p
    WHERE p.code IN (
     'BOOKING_CREATE', 'BOOKING_READ', 'BOOKING_UPDATE',
     'ROOM_CREATE', 'ROOM_READ', 'ROOM_UPDATE',
     'HOTEL_READ', 'HOTEL_UPDATE',
     'PAYMENT_READ', 'PAYMENT_UPDATE',
     'TICKET_READ', 'TICKET_UPDATE',
     'REPORT_READ', 'REPORT_EXPORT',
     'USER_READ'
     )
    AND NOT EXISTS (
     SELECT 1 FROM auth.role_permissions rp
     WHERE rp.role_id = (SELECT id FROM auth.roles WHERE code = 'HOST')
     AND rp.permission_id = p.id
    );

    -- ============================================================
    -- PHASE 4: GÁN 5 PERMISSIONS CHO USER
    -- ============================================================
    INSERT INTO auth.role_permissions (role_id, permission_id)
    SELECT
     (SELECT id FROM auth.roles WHERE code = 'USER'),
     p.id
    FROM auth.permissions p
    WHERE p.code IN (
     'BOOKING_CREATE', 'BOOKING_READ',
     'PAYMENT_READ',
     'TICKET_CREATE', 'TICKET_READ'
     )
    AND NOT EXISTS (
     SELECT 1 FROM auth.role_permissions rp
     WHERE rp.role_id = (SELECT id FROM auth.roles WHERE code = 'USER')
     AND rp.permission_id = p.id
    );

    -- ============================================================
    -- PHASE 5: USER CÓ STAFF/MANAGER → XỬ LÝ
    -- (Xóa STAFF/MANAGER cho user đã có role USER trước, tránh duplicate)
    -- ============================================================
    DELETE FROM auth.user_roles
    WHERE role_id IN (SELECT id FROM auth.roles WHERE code IN ('STAFF', 'MANAGER'))
     AND user_id IN (
     SELECT user_id FROM auth.user_roles
     WHERE role_id = (SELECT id FROM auth.roles WHERE code = 'USER')
     );

    -- Chuyển STAFF/MANAGER → USER cho user chưa có role USER
    UPDATE auth.user_roles
    SET role_id = (SELECT id FROM auth.roles WHERE code = 'USER')
    WHERE role_id IN (SELECT id FROM auth.roles WHERE code IN ('STAFF', 'MANAGER'));

    -- ============================================================
    -- PHASE 6: DỌN DẸP STAFF/MANAGER (sau khi đã chuyển user)
    -- ============================================================
    DELETE FROM auth.role_permissions
    WHERE role_id IN (SELECT id FROM auth.roles WHERE code IN ('STAFF', 'MANAGER'));

    DELETE FROM auth.roles WHERE code IN ('STAFF', 'MANAGER');

    -- ============================================================
    -- PHASE 7: XÓA PERMISSIONS DELETE (BOOKING_DELETE, USER_DELETE)
    -- ============================================================
    DELETE FROM auth.role_permissions
    WHERE permission_id IN (SELECT id FROM auth.permissions WHERE code IN ('BOOKING_DELETE', 'USER_DELETE'));

    DELETE FROM auth.permissions WHERE code IN ('BOOKING_DELETE', 'USER_DELETE');

    -- ============================================================
    -- PHASE 8: PHONE - XỬ LÝ AN TOÀN
    -- Bước 8.1: DROP unique constraint TRƯỚC (nếu tồn tại)
    -- Bước 8.2: Drop unique index (nếu có index riêng)
    -- Bước 8.3: Set DEFAULT ''
    -- Bước 8.4: Update phone null → ''
    -- Bước 8.5: Set NOT NULL
    -- Bước 8.6: Tạo NON-UNIQUE index
    -- ============================================================

    -- 8.1: Drop unique constraint
    ALTER TABLE auth.users DROP CONSTRAINT IF EXISTS idx_users_phone_unique;

    -- 8.2: Drop unique index (nếu có index riêng)
    DROP INDEX IF EXISTS auth.idx_users_phone_unique;

    -- 8.3: Set default ''
    ALTER TABLE auth.users ALTER COLUMN phone SET DEFAULT '';

    -- 8.4: Update NULL → '' (an toàn vì constraint đã drop)
    UPDATE auth.users SET phone = '' WHERE phone IS NULL;

    -- 8.5: Set NOT NULL
    ALTER TABLE auth.users ALTER COLUMN phone SET NOT NULL;

    -- 8.6: Tạo NON-UNIQUE index (vẫn index để search, nhưng không unique)
    CREATE INDEX IF NOT EXISTS idx_users_phone ON auth.users(phone);