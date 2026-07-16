// ═══════════════════════════════════════════════════════════
// ROLE GUARD — BFF Pattern
// Sau BFF redirect, localStorage trống nhưng cookies có.
// → Gọi /api/users/me lấy roles → check quyền → cho qua hoặc chặn.
// ═══════════════════════════════════════════════════════════

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map, catchError } from 'rxjs/operators';
import { of, Observable } from 'rxjs';
import { Auth } from '../services/auth';

/**
 * Helper: đảm bảo user đã load (từ localStorage hoặc API)
 * Trả Observable<boolean> — true nếu user đã sẵn sàng
 */
function ensureUser(auth: Auth, router: Router, returnUrl: string): Observable<boolean> {
  if (auth.isLoggedIn()) return of(true);

  return auth.getProfile().pipe(
    map((user: any) => {
      auth.saveUser({
        username: user.username,
        email: user.email,
        roles: user.roles?.map((r: any) => (typeof r === 'string' ? r : r.code)) || [],
        timezone: user.timezone,
        phone: user.phone,
        firstName: user.firstName,
        lastName: user.lastName,
      });
      return true;
    }),
    catchError(() => {
      router.navigate(['/auth/login'], {
        queryParams: { returnUrl },
      });
      return of(false);
    }),
  );
}

/**
 * Role guard — check user có ít nhất 1 role trong danh sách
 */
export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (_route, state) => {
    const auth = inject(Auth);
    const router = inject(Router);

    return ensureUser(auth, router, state.url).pipe(
      map((loaded) => {
        if (!loaded) return false;
        if (auth.hasAnyRole(allowedRoles)) return true;

        router.navigateByUrl(auth.getLandingPath());
        return false;
      }),
    );
  };
};

/**
 * Admin guard — cho phép ADMIN_ALL, ADMIN, HOST vào admin layout
 */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  return ensureUser(auth, router, state.url).pipe(
    map((loaded) => {
      if (!loaded) return false;

      const hasAdminRole = auth.getRoles().some((r) => ['ADMIN_ALL', 'ADMIN', 'HOST'].includes(r));

      if (hasAdminRole) return true;

      router.navigateByUrl(auth.getLandingPath());
      return false;
    }),
  );
};
