// ═══════════════════════════════════════════════════════════
// ERROR UTIL
// Backend trả ErrorResponse: { success, errorCode, message, ... }
// Helper rút message ra để hiển thị cho user
// ═══════════════════════════════════════════════════════════

import { HttpErrorResponse } from '@angular/common/http';
import { ErrorResponse } from '../models/auth.model';

export function extractErrorMessage(
  err: unknown,
  fallback: string = 'Something went wrong',
): string {
  if (!err) return fallback;

  // HttpErrorResponse từ Angular
  if (err instanceof HttpErrorResponse) {
    const body = err.error as ErrorResponse | string | null;

    if (typeof body === 'string') return body;
    if (body?.message) return body.message;

    // Status code message mặc định
    switch (err.status) {
      case 0:
        return 'Cannot connect to server. Please check your network.';
      case 400:
        return 'Invalid request';
      case 401:
        return 'Unauthorized. Please login again.';
      case 403:
        return 'You do not have permission to perform this action';
      case 404:
        return 'Resource not found';
      case 409:
        return 'Conflict. Resource already exists.';
      case 500:
        return 'Server error. Please try again later.';
      default:
        return fallback;
    }
  }

  // Lỗi throw thường
  if (err instanceof Error) return err.message;

  return fallback;
}
