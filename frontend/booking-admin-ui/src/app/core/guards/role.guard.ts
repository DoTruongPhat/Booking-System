import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Auth } from '../services/auth';

function ensureUser(auth: Auth, router: Router, returnUrl: string): Observable<boolean> {
  return auth.hydrateUserFromProfile().pipe(
    map(() => {
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
