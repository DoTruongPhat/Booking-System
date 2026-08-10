import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'auth/login', renderMode: RenderMode.Prerender },
  { path: '', renderMode: RenderMode.Client },
  { path: 'hotels/**', renderMode: RenderMode.Client },
  { path: 'admin/**', renderMode: RenderMode.Client },
  { path: 'user/**', renderMode: RenderMode.Client },
  { path: '**', renderMode: RenderMode.Client },
];
