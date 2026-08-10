// ═══════════════════════════════════════════════════════════
// ONBOARDING GUARD — BFF Pattern
// Chặn user chưa có phone khi đặt phòng → ép vào /user/profile
// Sau BFF redirect, localStorage có thể trống → gọi /users/me trước.
// ═══════════════════════════════════════════════════════════

import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { Auth } from '../services/auth';

export const onboardingGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  const router = inject(Router);

  // Chỉ chặn khi đặt phòng mới
  if (!state.url.startsWith('/user/booking/new')) {
    return true;
  }

  // User đã có trong localStorage
  // localStorage trống → gọi API
  return auth.hydrateUserFromProfile().pipe(
    map((profile: any) => {
      auth.saveUser({
        username: profile.username,
        email: profile.email,
        roles: profile.roles?.map((r: any) => (typeof r === 'string' ? r : r.code)) || [],
        timezone: profile.timezone,
        phone: profile.phone,
        firstName: profile.firstName,
        lastName: profile.lastName,
      });
      return checkPhone(profile, router, state.url);
    }),
    catchError(() => {
      router.navigate(['/auth/login'], {
        queryParams: { returnUrl: state.url },
      });
      return of(false);
    }),
  );
};

function checkPhone(user: any, router: Router, returnUrl: string): boolean {
  if (!user?.phone || user.phone.trim() === '') {
    router.navigate(['/user/profile'], {
      queryParams: { onboarding: 'true', returnUrl },
    });
    return false;
  }
  return true;
}
