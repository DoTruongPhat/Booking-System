// ═══════════════════════════════════════════════════════════
// AUTH / USER MODELS
// Map theo domain User.java + Role.java của auth-service
// ═══════════════════════════════════════════════════════════

// ── ROLE ────────────────────────────────────────────────────
export interface Role {
  id: string;
  code: string; // VD: "ADMIN_ALL", "STAFF", "USER"
  name: string;
  description?: string;
  active: boolean;
  createdAt?: string;
  permissions?: Permission[];
}

export interface Permission {
  id: string;
  code: string; // VD: "USER_UPDATE", "ROLE_CREATE"
  name: string; // VD: "Update user"
  resource?: string; // VD: "user", "role"
  action?: string; // VD: "update", "create", "delete"
  description?: string;
  createdAt?: string;
}

// ── USER ────────────────────────────────────────────────────
export interface User {
  id: string; // UUID
  username: string;
  email: string;
  active: boolean;
  locked: boolean;
  failedAttempts: number;
  lockedUntil: string | null; // ZonedDateTime -> ISO string
  timezone: string | null;
  twoFactorEnabled: boolean;
  createdAt: string;
  updatedAt: string;
  roles: Role[];
  /** Phase 7: phone của user (optional). */
  phone?: string | null;
  /** Phase A.1: firstName + lastName từ Keycloak (optional). */
  firstName?: string | null;
  lastName?: string | null;
}

// User thô lưu trong localStorage (sau login) - roles chỉ là string[] (code)
export interface AuthUser {
  id?: string;
  username: string;
  email: string;
  roles?: string[];
  timezone?: string;
  /** Phase 7: phone của user (vd KC sync lần đầu chưa có). */
  phone?: string;
  firstName?: string;
  lastName?: string;
}

// ── SUPPORT TICKET ────────────────────────────────────────
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface SupportTicket {
  id: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  createdBy: string; // UUID của user tạo
  assignedTo: string | null; // UUID của staff được assign
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
}

export type UserStatusKey = 'active' | 'inactive' | 'locked';

// ── SPRING PAGE<T> ──────────────────────────────────────────
// Backend trả về Page<User> của Spring Data, có thêm các field mặc định
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalElements2?: number;
  totalPages: number;
  number: number; // current page (0-based)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
  pageable?: {
    pageNumber: number;
    pageSize: number;
    sort: { empty: boolean; sorted: boolean; unsorted: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  sort?: { empty: boolean; sorted: boolean; unsorted: boolean };
}

// ── REQUEST DTOs ────────────────────────────────────────────

// Gửi lên backend: UpdateUserRequest (BE dùng field "active", không phải "isActive")
export interface UpdateUserRequest {
  email?: string;
  timezone?: string;
  active?: boolean;
}

export interface AssignRoleRequest {
  roleCode: string;
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// ── LOGIN RESPONSE ─────────────────────────────────────────
export interface LoginResponse {
  token?: string;
  username: string;
  email: string;
  roles: string[];
  timezone?: string;
  twoFactorRequired: boolean;
  mfaSessionToken?: string;
  /** Phase 7: true nếu user cần bổ sung phone (vd mới sync từ KC). */
  phoneRequired?: boolean;
  phone?: string;
  passwordRequired?: boolean;
  firstName?: string;
  lastName?: string;
}

// ── REGISTER REQUEST / RESPONSE ───────────────────────────
export interface RegisterRequest {
  fullName: string;
  username: string;
  email: string;
  phone: string;
  password: string;
  timeZone?: string;
}

export interface RegisterResponse {
  success: boolean;
  message: string;
  userId?: string;
  username?: string;
}

// ── FORGOT / RESET PASSWORD ────────────────────────────────
export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
}

// ── ERROR RESPONSE (BE chuẩn) ───────────────────────────────
export interface ErrorResponse {
  success: boolean;
  errorCode: string; // VD: "USR_001"
  message: string;
  timestamp: string;
  requestId?: string;
  traceId?: string;
  details?: unknown;
}
