import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';

/**
 * Idempotency Interceptor
 *
 * Tự động sinh header `Idempotency-Key` (UUID v4) cho các request có nguy cơ
 * duplicate (POST/PUT/PATCH/DELETE) vào các endpoint nhạy cảm về tiền/đặt chỗ.
 *
 * Gateway sẽ:
 * - Lần 1: forward → cache response 24h
 * - Lần 2 (cùng key): trả cached response (không gọi lại downstream)
 * - Lần 2 (đang xử lý): trả 409 Conflict
 *
 * → Tránh trừ tiền 2 lần khi user double-click "Thanh toán"
 */

// HTTP methods cần idempotency
const TARGET_METHODS = ['POST', 'PUT', 'PATCH', 'DELETE'];

// URL patterns cần áp dụng (match với gateway config)
const TARGET_PATHS = ['/api/bookings', '/api/payments'];

const HEADER_NAME = 'Idempotency-Key';

export const idempotencyInterceptor: HttpInterceptorFn = (req, next) => {
  if (!shouldApply(req)) {
    return next(req);
  }

  // Nếu request đã có header (vd: developer override) → giữ nguyên
  if (req.headers.has(HEADER_NAME)) {
    return next(req);
  }

  const key = generateUUID();
  const cloned = req.clone({
    setHeaders: { [HEADER_NAME]: key },
  });

  return next(cloned);
};

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

function shouldApply(req: HttpRequest<unknown>): boolean {
  const method = req.method.toUpperCase();
  if (!TARGET_METHODS.includes(method)) return false;

  // req.url có thể là full URL hoặc relative path
  return TARGET_PATHS.some((path) => req.url.includes(path));
}

/**
 * UUID v4 generator.
 * Ưu tiên crypto.randomUUID() (modern browsers, secure context).
 * Fallback cho môi trường cũ hoặc test/SSR.
 */
function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  // Fallback: RFC 4122 v4
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
