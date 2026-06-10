// ═══════════════════════════════════════════════════════════
// ROLE GUARD
// Kiểm tra user có ít nhất 1 role trong danh sách cho phép
// Dùng cho các route admin cần quyền cụ thể (VD: ADMIN_ALL)
// ═══════════════════════════════════════════════════════════

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Auth } from '../services/auth';

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (_route, state) => {
    const auth = inject(Auth);
    const router = inject(Router);

    if (!auth.isLoggedIn()) {
      router.navigate(['/auth/login'], {
        queryParams: { returnUrl: state.url },
      });
      return false;
    }

    if (auth.hasAnyRole(allowedRoles)) return true;

    // Không đủ quyền → đá về dashboard
    router.navigate(['/admin/dashboard']);
    return false;
  };
};
