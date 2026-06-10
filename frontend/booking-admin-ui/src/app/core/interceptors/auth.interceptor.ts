// ═══════════════════════════════════════════════════════════
// AUTH INTERCEPTOR
// - Tự động gắn "Authorization: Bearer <token>" cho mọi request /api/*
// - Bắt response 401 → clear token + redirect về login
// ═══════════════════════════════════════════════════════════

import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Auth } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);

  const token = auth.getToken();

  // Chỉ gắn token cho các request cùng origin /api/*
  if (token && req.url.startsWith('/api/')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.includes('/auth/login')) {
        // Token hết hạn / không hợp lệ
        auth.clearAll();
        router.navigate(['/auth/login'], {
          queryParams: { returnUrl: router.url },
        });
      }
      return throwError(() => err);
    }),
  );
};
