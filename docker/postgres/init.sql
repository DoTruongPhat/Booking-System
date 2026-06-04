-- File này chạy 1 lần duy nhất khi Postgres khởi động lần đầu
-- Mục đích: tạo extension và schema cần thiết

-- pgcrypto: cung cấp gen_random_uuid() để tạo UUID tự động
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- pg_trgm: hỗ trợ tìm kiếm text nhanh hơn
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Tạo 3 schema riêng biệt, không dùng "public" mặc định
-- auth: lưu user, role, permission, tokenEntity
-- booking: lưu dữ liệu nghiệp vụ
-- keycloak: Keycloak tự quản lý
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS booking;
CREATE SCHEMA IF NOT EXISTS keycloak;

-- Cấp quyền cho user ứng dụng
GRANT ALL PRIVILEGES ON SCHEMA auth     TO booking_user;
GRANT ALL PRIVILEGES ON SCHEMA booking  TO booking_user;
GRANT ALL PRIVILEGES ON SCHEMA keycloak TO booking_user;

-- Đặt search_path mặc định
-- Sau này viết query không cần gõ "auth." ở mọi chỗ
ALTER USER booking_user SET search_path TO auth, booking, public;