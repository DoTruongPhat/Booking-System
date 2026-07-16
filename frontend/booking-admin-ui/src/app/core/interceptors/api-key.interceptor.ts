// ═══════════════════════════════════════════════════════════
// API KEY INTERCEPTOR
// Gắn header X-API-KEY vào request đến BE (/api/*).
// Bỏ qua request đến domain khác (Keycloak, CDN, etc).
// ═══════════════════════════════════════════════════════════

import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  // Chỉ gắn cho request đến BE của mình
  if (!req.url.startsWith('/api/') && !req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        'X-API-KEY': environment.apiKey,
      },
    }),
  );
};
