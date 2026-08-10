import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { generateIdempotencyKey, IDEMPOTENCY_HEADER } from '../http/idempotency';
import { catchError, Observable, switchMap, throwError, timer } from 'rxjs';

const TARGET_METHOD = 'POST';
const MAX_CONFLICT_RETRIES = 3;
const DEFAULT_RETRY_AFTER_SECONDS = 5;

const TARGET_ENDPOINTS = [
  '/api/user/bookings',
  '/api/user/bookings/*/cancel',
  '/api/user/payments/init',
  '/api/user/payments/*/cancel',
  '/api/admin/payments/*/refund',
  '/api/host/hotels',
  '/api/host/hotels/*/rooms',
  '/api/host/rooms/*/availability/block',
];

export const idempotencyInterceptor: HttpInterceptorFn = (req, next) => {
  if (!shouldApply(req)) {
    return next(req);
  }

  const requestWithKey = req.headers.has(IDEMPOTENCY_HEADER)
    ? req
    : req.clone({
        setHeaders: { [IDEMPOTENCY_HEADER]: generateIdempotencyKey() },
      });

  return sendWithConflictRetry(requestWithKey, next, MAX_CONFLICT_RETRIES);
};

function sendWithConflictRetry(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  retriesLeft: number,
): Observable<HttpEvent<unknown>> {
  return next(req).pipe(
    catchError((error: unknown) => {
      if (retriesLeft <= 0 || !isIdempotencyConflict(error)) {
        return throwError(() => error);
      }

      const retryAfterSeconds = resolveRetryAfter(error as HttpErrorResponse);
      return timer(retryAfterSeconds * 1000).pipe(
        switchMap(() => sendWithConflictRetry(req, next, retriesLeft - 1)),
      );
    }),
  );
}

function isIdempotencyConflict(error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse) || error.status !== 409) {
    return false;
  }

  const body = error.error;
  return body?.errorCode === 'IDEMPOTENCY_002' || String(body?.message || '').includes('Request in progress');
}

function resolveRetryAfter(error: HttpErrorResponse): number {
  const headerValue = error.headers.get('Retry-After');
  const bodyValue = Number(error.error?.retryAfter);
  const parsedHeader = Number(headerValue);
  const seconds = Number.isFinite(parsedHeader) && parsedHeader > 0
    ? parsedHeader
    : Number.isFinite(bodyValue) && bodyValue > 0
      ? bodyValue
      : DEFAULT_RETRY_AFTER_SECONDS;

  return Math.min(seconds, 10);
}

function shouldApply(req: HttpRequest<unknown>): boolean {
  if (req.method.toUpperCase() !== TARGET_METHOD) {
    return false;
  }

  const path = extractPath(req.url);
  return TARGET_ENDPOINTS.some((pattern) => matchesPath(pattern, path));
}

function extractPath(url: string): string {
  try {
    return new URL(url, window.location.origin).pathname;
  } catch {
    return url.split('?')[0] ?? url;
  }
}

function matchesPath(pattern: string, path: string): boolean {
  const patternSegments = trimSlashes(pattern).split('/');
  const pathSegments = trimSlashes(path).split('/');
  if (patternSegments.length !== pathSegments.length) {
    return false;
  }

  return patternSegments.every((segment, index) => segment === '*' || segment === pathSegments[index]);
}

function trimSlashes(value: string): string {
  return value.replace(/^\/+|\/+$/g, '');
}
