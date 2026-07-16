// ═══════════════════════════════════════════════════════════
// AUTH INTERCEPTOR (HttpOnly Cookie + Refresh Rotation)
//
// Strategy:
//  - withCredentials cho mọi /api/* (để browser gửi cookie)
//  - Bắt 401 từ protected endpoint → gọi /auth/refresh → retry
//  - Concurrency lock: nhiều 401 cùng lúc → 1 lần refresh, các request kia đợi
//  - Skip refresh logic cho auth endpoints (login, refresh, public)
//  - Refresh fail → Modal "Phiên hết hạn" → clear user → redirect /login
// ═══════════════════════════════════════════════════════════

import {
  HttpInterceptorFn,
  HttpErrorResponse,
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { NzModalService } from 'ng-zorro-antd/modal';
import { BehaviorSubject, Observable, filter, take, catchError, switchMap, throwError } from 'rxjs';
import { Auth } from '../services/auth';

let isRefreshing = false;
const refreshSubject = new BehaviorSubject<'done' | 'failed' | null>(null);
let isSessionExpiredModalOpen = false;

const AUTH_ENDPOINTS = [
  '/auth/login',
  '/auth/refresh',
  '/auth/register',
  '/auth/public-key',
  '/auth/exchange',
  '/auth/forgot-password',
  '/auth/reset-password',
];

function isAuthEndpoint(url: string): boolean {
  return AUTH_ENDPOINTS.some((endpoint) => url.includes(endpoint));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  //  Inject Ở ĐÂY (đang trong injection context)
  const auth = inject(Auth);
  const router = inject(Router);
  const modal = inject(NzModalService);

  console.log('[AuthInt] →', req.method, req.url);

  if (req.url.startsWith('/api/')) {
    req = req.clone({ withCredentials: true });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      console.log('[AuthInt] ✗ error:', err.status, req.url, 'isAuth:', isAuthEndpoint(req.url));

      if (err.status === 401 && !isAuthEndpoint(req.url)) {
        console.log('[AuthInt] → trigger handle401, isRefreshing:', isRefreshing);
        //  Pass services xuống
        return handle401(req, next, auth, router, modal);
      }
      return throwError(() => err);
    }),
  );
};

//  Nhận services qua parameter, KHÔNG dùng inject() ở đây
function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: Auth,
  router: Router,
  modal: NzModalService,
): Observable<HttpEvent<unknown>> {
  if (isRefreshing) {
    console.log('[AuthInt] → queue, waiting refresh result');
    return refreshSubject.pipe(
      filter((status) => status !== null),
      take(1),
      switchMap((status) => {
        console.log('[AuthInt] ← refresh result:', status, 'retry:', req.url);
        if (status === 'done') {
          return next(req.clone({ withCredentials: true }));
        }
        return throwError(() => new Error('Session expired'));
      }),
    );
  }

  isRefreshing = true;
  refreshSubject.next(null);
  console.log('[AuthInt] → calling /auth/refresh');

  return auth.refreshToken().pipe(
    switchMap((response: any) => {
      console.log('[AuthInt] ✓ refresh OK');
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
      console.log('[AuthInt] → retry original:', req.url);
      return next(req.clone({ withCredentials: true }));
    }),
    catchError((refreshErr: HttpErrorResponse) => {
      console.log('[AuthInt] ✗ refresh failed:', refreshErr.status);
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

  modal.warning({
    nzTitle: '⚠️ Phiên đăng nhập đã hết hạn',
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
