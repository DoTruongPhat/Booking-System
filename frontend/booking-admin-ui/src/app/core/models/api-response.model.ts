// ═══════════════════════════════════════════════════════════
// API RESPONSE MODEL
// Map theo chuẩn response wrapper của core-service (Spring Boot)
// Tất cả endpoint đều trả { timestamp, status, message, data }
// ═══════════════════════════════════════════════════════════

/**
 * Wrapper response chuẩn từ backend.
 * Mọi endpoint đều trả format này.
 */
export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
}

/**
 * Pagination response nằm trong `data` của ApiResponse.
 * Map theo Spring Data Page<T>.
 */
export interface PaginatedData<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-based)
  size: number;
}

/**
 * Helper type: ApiResponse chứa paginated data.
 * Dùng cho các endpoint có pagination.
 *
 * Ví dụ: ApiResponse<PaginatedData<Booking>>
 */
export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>;
