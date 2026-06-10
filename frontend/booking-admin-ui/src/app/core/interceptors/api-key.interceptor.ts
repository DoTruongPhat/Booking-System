import { HttpInterceptorFn } from '@angular/common/http';

export const apiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  // Lấy API key từ environment
  const apiKey = 'dev-api-key-abc123';

  const cloned = req.clone({
    setHeaders: {
      'X-API-KEY': apiKey,
    },
  });

  return next(cloned);
};
