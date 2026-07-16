import { RenderMode, ServerRoute } from '@angular/ssr';

// Tất cả route admin cần auth + gọi API → dùng Server (SSR), không Prerender
// Prerender chỉ dành cho login page
export const serverRoutes: ServerRoute[] = [
  { path: 'auth/login', renderMode: RenderMode.Prerender },
  { path: '', renderMode: RenderMode.Server },
  { path: 'hotels/**', renderMode: RenderMode.Server },
  { path: 'admin/**', renderMode: RenderMode.Client }, // ← Client only
  { path: 'user/**', renderMode: RenderMode.Client }, // ← Client only
  { path: '**', renderMode: RenderMode.Server },
];
