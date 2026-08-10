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

  return auth.hydrateUserFromProfile().pipe(
    map(() => {
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
