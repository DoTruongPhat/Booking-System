// ═══════════════════════════════════════════════════════════
// AUTH GUARD
// Chặn truy cập /admin/** khi chưa đăng nhập
// → Redirect về /auth/login kèm returnUrl
// ═══════════════════════════════════════════════════════════

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { Auth } from '../services/auth';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isLoggedIn()) return true;

  router.navigate(['/auth/login'], {
    queryParams: { returnUrl: state.url },
  });
  return false;
};
