// AUTH INTERCEPTOR (HttpOnly Cookie + Refresh Rotation)
//
// Strategy:
//  - Use withCredentials for /api/* requests so HttpOnly cookies are sent.
//  - On 401 from protected endpoints, refresh once and retry the original request.
//  - Concurrent 401 responses wait for the same refresh result.
//  - Auth endpoints do not trigger refresh handling.
//  - Refresh failure clears local user state and redirects to login.

import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { NzModalService } from 'ng-zorro-antd/modal';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { Auth } from '../services/auth';

let isRefreshing = false;
const refreshSubject = new BehaviorSubject<'done' | 'failed' | null>(null);
let isSessionExpiredModalOpen = false;

const AUTH_ENDPOINTS = [
  '/auth/login',
  '/api/users/me',
  '/auth/refresh',
  '/auth/register',
  '/auth/public-key',
  '/auth/exchange',
  '/auth/forgot-password',
  '/auth/reset-password',
  '/auth/verify-2fa',
  '/auth/complete-profile',
];

function isAuthEndpoint(url: string): boolean {
  return AUTH_ENDPOINTS.some((endpoint) => url.includes(endpoint));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const modal = inject(NzModalService);

  if (req.url.startsWith('/api/')) {
    req = req.clone({ withCredentials: true });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !isAuthEndpoint(req.url)) {
        return handle401(req, next, auth, router, modal);
      }

      return throwError(() => err);
    }),
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: Auth,
  router: Router,
  modal: NzModalService,
): Observable<HttpEvent<unknown>> {
  if (isRefreshing) {
    return refreshSubject.pipe(
      filter((status) => status !== null),
      take(1),
      switchMap((status) => {
        if (status === 'done') {
          return next(req.clone({ withCredentials: true }));
        }

        return throwError(() => new Error('Session expired'));
      }),
    );
  }

  isRefreshing = true;
  refreshSubject.next(null);

  return auth.refreshToken().pipe(
    switchMap((response: any) => {
      if (response?.username) {
        auth.saveUser({
          username: response.username,
          email: response.email,
          roles: response.roles,
          timezone: response.timezone,
          phone: response.phone,
        });
      }

      isRefreshing = false;
      refreshSubject.next('done');

      return next(req.clone({ withCredentials: true }));
    }),
    catchError((refreshErr: HttpErrorResponse) => {
      isRefreshing = false;
      refreshSubject.next('failed');
      handleSessionExpired(auth, router, modal);

      return throwError(() => refreshErr);
    }),
  );
}

function handleSessionExpired(auth: Auth, router: Router, modal: NzModalService): void {
  if (isSessionExpiredModalOpen) {
    return;
  }

  isSessionExpiredModalOpen = true;

  const currentUrl = router.url;
  const isProtectedRoute = currentUrl.startsWith('/admin') || currentUrl.startsWith('/user');

  auth.clearAll();

  if (!isProtectedRoute) {
    isSessionExpiredModalOpen = false;
    return;
  }

  modal.warning({
    nzTitle: 'Phiên đăng nhập đã hết hạn',
    nzContent: isProtectedRoute
      ? 'Phiên đăng nhập của bạn đã hết hạn hoặc bạn đã đăng nhập trên thiết bị khác. Vui lòng đăng nhập lại để tiếp tục.'
      : 'Vui lòng đăng nhập lại để tiếp tục sử dụng.',
    nzOkText: 'Đăng nhập lại',
    nzCancelText: null,
    nzClosable: false,
    nzMaskClosable: false,
    nzKeyboard: false,
    nzOnOk: () => {
      isSessionExpiredModalOpen = false;
      router.navigate(['/auth/login'], {
        queryParams: {
          returnUrl: currentUrl.startsWith('/auth/') ? '/' : currentUrl,
        },
      });
    },
  });
}
