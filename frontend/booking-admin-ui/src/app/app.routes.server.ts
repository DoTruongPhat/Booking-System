import { RenderMode, ServerRoute } from '@angular/ssr';

// Tất cả route admin cần auth + gọi API → dùng Server (SSR), không Prerender
// Prerender chỉ dành cho login page
export const serverRoutes: ServerRoute[] = [
  {
    path: 'auth/login',
    renderMode: RenderMode.Prerender,
  },
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
