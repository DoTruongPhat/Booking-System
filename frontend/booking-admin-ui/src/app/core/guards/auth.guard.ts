// ═══════════════════════════════════════════════════════════
// AUTH GUARD — BFF Pattern
// Sau BFF redirect, localStorage trống nhưng cookies có.
// → Gọi /api/users/me xác nhận login → lưu localStorage → cho qua.
// ═══════════════════════════════════════════════════════════
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { Auth } from '../services/auth';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (auth.isLoggedIn()) return true;

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
        queryParams: { returnUrl: state.url },
      });
      return of(false);
    }),
  );
};
