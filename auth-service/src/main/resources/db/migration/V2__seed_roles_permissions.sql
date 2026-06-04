-- Tạo 4 roles cơ bản
INSERT INTO auth.roles (code, name, description) VALUES
('ADMIN',   'Administrator', 'Toàn quyền hệ thống'),
('MANAGER', 'Manager',       'Quản lý booking'),
('STAFF',   'Staff',         'Nhân viên'),
('USER',    'User',          'Người dùng thường')
    ON CONFLICT (code) DO NOTHING;

-- Tạo permissions
INSERT INTO auth.permissions (code, name, resource, action) VALUES
 ('USER_READ',      'Xem user',    'USER',    'READ'),
 ('USER_CREATE',    'Tạo user',    'USER',    'CREATE'),
 ('USER_UPDATE',    'Sửa user',    'USER',    'UPDATE'),
 ('USER_DELETE',    'Xóa user',    'USER',    'DELETE'),
 ('BOOKING_READ',   'Xem booking', 'BOOKING', 'READ'),
 ('BOOKING_CREATE', 'Tạo booking', 'BOOKING', 'CREATE'),
 ('BOOKING_UPDATE', 'Sửa booking', 'BOOKING', 'UPDATE'),
 ('BOOKING_DELETE', 'Xóa booking', 'BOOKING', 'DELETE'),
 ('REPORT_READ',    'Xem report',  'REPORT',  'READ'),
 ('REPORT_EXPORT',  'Xuất report', 'REPORT',  'EXPORT'),
 ('ADMIN_ALL',      'Full admin',  'ADMIN',   'ALL')
    ON CONFLICT (code) DO NOTHING;

-- ADMIN: tất cả permissions
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.code = 'ADMIN'
    ON CONFLICT DO NOTHING;

-- MANAGER
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.code = 'MANAGER'
  AND p.code IN ('USER_READ','BOOKING_READ','BOOKING_CREATE','BOOKING_UPDATE','REPORT_READ','REPORT_EXPORT')
    ON CONFLICT DO NOTHING;

-- STAFF
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.code = 'STAFF'
  AND p.code IN ('BOOKING_READ','BOOKING_CREATE','BOOKING_UPDATE')
    ON CONFLICT DO NOTHING;

-- USER
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.code = 'USER'
  AND p.code IN ('BOOKING_READ','BOOKING_CREATE')
    ON CONFLICT DO NOTHING;