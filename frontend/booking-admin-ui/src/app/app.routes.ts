import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

// Lazy load Admin Layout
const AdminLayout = () =>
  import('./layouts/admin-layout/admin-layout.component').then((m) => m.AdminLayoutComponent);

// Lazy load Auth Layout
const AuthLayout = () =>
  import('./layouts/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent);

export const routes: Routes = [
  // ═══ AUTH ROUTES ═══ (dùng AuthLayout)
  {
    path: 'auth',
    loadComponent: AuthLayout,
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./views/auth/login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'verify-2fa',
        loadComponent: () =>
          import('./views/auth/verify-2fa/verify-2fa.component').then((m) => m.Verify2faComponent),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },

  // ═══ ADMIN ROUTES ═══ (dùng AdminLayout + authGuard)
  {
    path: 'admin',
    loadComponent: AdminLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./layouts/admin/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        // List + CRUD user (cần quyền ADMIN_ALL)
        path: 'users',
        canActivate: [roleGuard(['ADMIN_ALL'])],
        loadComponent: () => import('./layouts/admin/users/users').then((m) => m.Users),
      },
      {
        // Chi tiết 1 user
        path: 'users/:id',
        canActivate: [roleGuard(['ADMIN_ALL'])],
        loadComponent: () =>
          import('./layouts/admin/users/users-detail').then((m) => m.UsersDetail),
      },
      {
        // Staff xem tickets được assign cho mình
        // Chỉ cần authGuard (không cần ADMIN_ALL) - STAFF + ADMIN đều vào được
        path: 'staff/tickets',
        loadComponent: () =>
          import('./layouts/staff/staff-tickets/staff-tickets').then(
            (m) => m.StaffTicketsComponent,
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ═══ DEFAULT ═══
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: 'auth/login' },
];
