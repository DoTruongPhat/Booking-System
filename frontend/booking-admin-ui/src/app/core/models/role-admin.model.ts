// ═══════════════════════════════════════════════════════════
// ROLE ADMIN MODELS (A.2)
// Model cho trang quản lý Roles + Permissions
// Seed 3 role: ADMIN (toàn quyền), HOST (quản lý hotels của mình), USER
// Seed ~30 permission
// ═══════════════════════════════════════════════════════════

export type PermissionCode =
  // User
  | 'USER_READ'
  | 'USER_WRITE'
  | 'USER_DELETE'
  // Role
  | 'ROLE_READ'
  | 'ROLE_WRITE'
  | 'ROLE_DELETE'
  // Hotel
  | 'HOTEL_READ_ALL'
  | 'HOTEL_READ_OWN'
  | 'HOTEL_WRITE_ALL'
  | 'HOTEL_WRITE_OWN'
  // Room
  | 'ROOM_READ_ALL'
  | 'ROOM_READ_OWN'
  | 'ROOM_WRITE_ALL'
  | 'ROOM_WRITE_OWN'
  // Room type
  | 'ROOM_TYPE_READ_ALL'
  | 'ROOM_TYPE_READ_OWN'
  | 'ROOM_TYPE_WRITE_ALL'
  | 'ROOM_TYPE_WRITE_OWN'
  // Booking
  | 'BOOKING_READ_ALL'
  | 'BOOKING_READ_OWN_HOTEL'
  | 'BOOKING_WRITE_ALL'
  | 'BOOKING_WRITE_OWN_HOTEL'
  | 'BOOKING_CONFIRM_OWN_HOTEL'
  // Payment
  | 'PAYMENT_READ_ALL'
  | 'PAYMENT_READ_OWN_HOTEL'
  | 'PAYMENT_CONFIRM_OWN_HOTEL'
  // Ticket
  | 'TICKET_READ_ALL'
  | 'TICKET_READ_OWN'
  | 'TICKET_WRITE_ALL'
  | 'TICKET_WRITE_OWN'
  // Dashboard
  | 'DASHBOARD_VIEW_ALL'
  | 'DASHBOARD_VIEW_OWN_HOTEL';

export interface AdminPermission {
  id: string;
  code: PermissionCode;
  name: string;
  resource: string;
  action: string;
  description?: string;
}

export interface AdminRole {
  id: string;
  code: 'ADMIN' | 'HOST' | 'USER';
  name: string;
  description: string;
  permissions: PermissionCode[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// ═══ SEED PERMISSIONS (30+) ═══
export const PERMISSION_SEED: AdminPermission[] = [
  // USER
  { id: 'P-001', code: 'USER_READ', name: 'Xem user', resource: 'user', action: 'read' },
  { id: 'P-002', code: 'USER_WRITE', name: 'Sửa user', resource: 'user', action: 'write' },
  { id: 'P-003', code: 'USER_DELETE', name: 'Xóa user', resource: 'user', action: 'delete' },
  // ROLE
  { id: 'P-004', code: 'ROLE_READ', name: 'Xem role', resource: 'role', action: 'read' },
  { id: 'P-005', code: 'ROLE_WRITE', name: 'Sửa role', resource: 'role', action: 'write' },
  { id: 'P-006', code: 'ROLE_DELETE', name: 'Xóa role', resource: 'role', action: 'delete' },
  // HOTEL
  { id: 'P-007', code: 'HOTEL_READ_ALL', name: 'Xem tất cả hotel', resource: 'hotel', action: 'read_all' },
  { id: 'P-008', code: 'HOTEL_READ_OWN', name: 'Xem hotel của mình', resource: 'hotel', action: 'read_own' },
  { id: 'P-009', code: 'HOTEL_WRITE_ALL', name: 'Sửa tất cả hotel', resource: 'hotel', action: 'write_all' },
  { id: 'P-010', code: 'HOTEL_WRITE_OWN', name: 'Sửa hotel của mình', resource: 'hotel', action: 'write_own' },
  // ROOM
  { id: 'P-011', code: 'ROOM_READ_ALL', name: 'Xem tất cả phòng', resource: 'room', action: 'read_all' },
  { id: 'P-012', code: 'ROOM_READ_OWN', name: 'Xem phòng của hotel mình', resource: 'room', action: 'read_own' },
  { id: 'P-013', code: 'ROOM_WRITE_ALL', name: 'Sửa tất cả phòng', resource: 'room', action: 'write_all' },
  { id: 'P-014', code: 'ROOM_WRITE_OWN', name: 'Sửa phòng của hotel mình', resource: 'room', action: 'write_own' },
  // ROOM TYPE
  { id: 'P-015', code: 'ROOM_TYPE_READ_ALL', name: 'Xem tất cả loại phòng', resource: 'room_type', action: 'read_all' },
  { id: 'P-016', code: 'ROOM_TYPE_READ_OWN', name: 'Xem loại phòng của mình', resource: 'room_type', action: 'read_own' },
  { id: 'P-017', code: 'ROOM_TYPE_WRITE_ALL', name: 'Sửa tất cả loại phòng', resource: 'room_type', action: 'write_all' },
  { id: 'P-018', code: 'ROOM_TYPE_WRITE_OWN', name: 'Sửa loại phòng của mình', resource: 'room_type', action: 'write_own' },
  // BOOKING
  { id: 'P-019', code: 'BOOKING_READ_ALL', name: 'Xem tất cả đơn đặt', resource: 'booking', action: 'read_all' },
  { id: 'P-020', code: 'BOOKING_READ_OWN_HOTEL', name: 'Xem đơn đặt của hotel mình', resource: 'booking', action: 'read_own_hotel' },
  { id: 'P-021', code: 'BOOKING_WRITE_ALL', name: 'Sửa tất cả đơn đặt', resource: 'booking', action: 'write_all' },
  { id: 'P-022', code: 'BOOKING_WRITE_OWN_HOTEL', name: 'Sửa đơn đặt của hotel mình', resource: 'booking', action: 'write_own_hotel' },
  { id: 'P-023', code: 'BOOKING_CONFIRM_OWN_HOTEL', name: 'Xác nhận đơn đặt hotel mình', resource: 'booking', action: 'confirm_own_hotel' },
  // PAYMENT
  { id: 'P-024', code: 'PAYMENT_READ_ALL', name: 'Xem tất cả thanh toán', resource: 'payment', action: 'read_all' },
  { id: 'P-025', code: 'PAYMENT_READ_OWN_HOTEL', name: 'Xem thanh toán hotel mình', resource: 'payment', action: 'read_own_hotel' },
  { id: 'P-026', code: 'PAYMENT_CONFIRM_OWN_HOTEL', name: 'Xác nhận thanh toán hotel mình', resource: 'payment', action: 'confirm_own_hotel' },
  // TICKET
  { id: 'P-027', code: 'TICKET_READ_ALL', name: 'Xem tất cả phiếu hỗ trợ', resource: 'ticket', action: 'read_all' },
  { id: 'P-028', code: 'TICKET_READ_OWN', name: 'Xem phiếu hỗ trợ của mình', resource: 'ticket', action: 'read_own' },
  { id: 'P-029', code: 'TICKET_WRITE_ALL', name: 'Sửa tất cả phiếu hỗ trợ', resource: 'ticket', action: 'write_all' },
  { id: 'P-030', code: 'TICKET_WRITE_OWN', name: 'Sửa phiếu hỗ trợ của mình', resource: 'ticket', action: 'write_own' },
  // DASHBOARD
  { id: 'P-031', code: 'DASHBOARD_VIEW_ALL', name: 'Xem dashboard toàn hệ thống', resource: 'dashboard', action: 'view_all' },
  { id: 'P-032', code: 'DASHBOARD_VIEW_OWN_HOTEL', name: 'Xem dashboard hotel mình', resource: 'dashboard', action: 'view_own_hotel' },
];

// ═══ SEED ROLES (3 role theo yêu cầu Phase A) ═══
export const ROLE_SEED: AdminRole[] = [
  {
    id: 'R-001',
    code: 'ADMIN',
    name: 'Administrator',
    description: 'Toàn quyền hệ thống - quản lý users, roles, hotels, bookings, payments, tickets',
    permissions: [
      'USER_READ', 'USER_WRITE', 'USER_DELETE',
      'ROLE_READ', 'ROLE_WRITE', 'ROLE_DELETE',
      'HOTEL_READ_ALL', 'HOTEL_WRITE_ALL',
      'ROOM_READ_ALL', 'ROOM_WRITE_ALL',
      'ROOM_TYPE_READ_ALL', 'ROOM_TYPE_WRITE_ALL',
      'BOOKING_READ_ALL', 'BOOKING_WRITE_ALL',
      'PAYMENT_READ_ALL',
      'TICKET_READ_ALL', 'TICKET_WRITE_ALL',
      'DASHBOARD_VIEW_ALL',
    ],
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'R-002',
    code: 'HOST',
    name: 'Host',
    description: 'Chủ khách sạn - quản lý hotels, phòng và bookings của mình. Không xem được users/roles.',
    permissions: [
      'HOTEL_READ_OWN', 'HOTEL_WRITE_OWN',
      'ROOM_READ_OWN', 'ROOM_WRITE_OWN',
      'ROOM_TYPE_READ_OWN', 'ROOM_TYPE_WRITE_OWN',
      'BOOKING_READ_OWN_HOTEL', 'BOOKING_WRITE_OWN_HOTEL', 'BOOKING_CONFIRM_OWN_HOTEL',
      'PAYMENT_READ_OWN_HOTEL', 'PAYMENT_CONFIRM_OWN_HOTEL',
      'TICKET_READ_OWN', 'TICKET_WRITE_OWN',
      'DASHBOARD_VIEW_OWN_HOTEL',
    ],
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'R-003',
    code: 'USER',
    name: 'Customer',
    description: 'Khách hàng - đặt phòng, xem booking của mình, tạo ticket hỗ trợ',
    permissions: [
      'TICKET_READ_OWN', 'TICKET_WRITE_OWN',
    ],
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
];

// ═══ HELPER: Lấy permission object từ code ═══
export function getPermissionByCode(
  code: PermissionCode,
  allPerms: AdminPermission[],
): AdminPermission | undefined {
  return allPerms.find((p) => p.code === code);
}
