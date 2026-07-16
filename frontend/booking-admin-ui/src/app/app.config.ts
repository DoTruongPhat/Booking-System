import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { vi_VN, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import vi from '@angular/common/locales/vi';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { apiKeyInterceptor } from './core/interceptors/api-key.interceptor';
import { idempotencyInterceptor } from './core/interceptors/idempotency.interceptor';
import { NzIconsProvider } from './core/providers/nz-icons.provider';
import { NzModalService } from 'ng-zorro-antd/modal';

registerLocaleData(vi);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([apiKeyInterceptor, idempotencyInterceptor, authInterceptor]),
    ),
    provideAnimations(),
    provideNzIcons(NzIconsProvider.icons),
    provideNzI18n(vi_VN),
    NzModalService,
  ],
};
