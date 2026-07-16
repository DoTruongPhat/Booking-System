import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard, adminGuard } from './core/guards/role.guard';
import { onboardingGuard } from './core/guards/onboarding.guard';

// Lazy load Layouts
const AdminLayout = () =>
  import('./layouts/admin-layout/admin-layout.component').then((m) => m.AdminLayoutComponent);

const AuthLayout = () =>
  import('./layouts/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent);

export const routes: Routes = [
  // ═══ HOMEPAGE ═══ (public)
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./views/home/home.component').then((m) => m.HomeComponent),
  },

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
        path: 'register',
        loadComponent: () =>
          import('./views/auth/register/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./views/auth/forgot-password/forgot-password.component').then(
            (m) => m.ForgotPasswordComponent,
          ),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./views/auth/reset-password/reset-password.component').then(
            (m) => m.ResetPasswordComponent,
          ),
      },
      {
        path: 'complete-profile',
        loadComponent: () =>
          import('./views/auth/complete-profile/complete-profile.component').then(
            (m) => m.CompleteProfileComponent,
          ),
      },
      {
        path: 'verify-2fa',
        loadComponent: () =>
          import('./views/auth/verify-2fa/verify-2fa.component').then((m) => m.Verify2faComponent),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },

  // ═══ HOTEL ROUTES ═══ (public)
  {
    path: 'hotels',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./views/hotels/search/search.component').then((m) => m.HotelSearchComponent),
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./views/hotels/detail/detail.component').then((m) => m.HotelDetailComponent),
      },
    ],
  },

  // ═══ BOOKING SUCCESS ═══ (public, có booking ID là đủ)
  {
    path: 'booking/success/:id',
    loadComponent: () =>
      import('./views/booking/success/success.component').then((m) => m.BookingSuccessComponent),
  },

  // ═══ USER ROUTES ═══ (cần authGuard + onboardingGuard)
  {
    path: 'user',
    canActivate: [authGuard, onboardingGuard],
    children: [
      {
        path: 'bookings',
        loadComponent: () =>
          import('./views/user/booking/my-bookings/my-bookings.component').then(
            (m) => m.MyBookingsComponent,
          ),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./views/user/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'tickets',
        loadComponent: () =>
          import('./views/user/tickets/tickets.component').then((m) => m.MyTicketsComponent),
      },
      {
        path: 'booking/new',
        loadComponent: () =>
          import('./views/hotels/detail/detail.component').then((m) => m.HotelDetailComponent),
      },
      { path: '', redirectTo: 'bookings', pathMatch: 'full' },
    ],
  },

  // ═══ ADMIN ROUTES ═══ (dùng AdminLayout + adminGuard + onboardingGuard)
  {
    path: 'admin',
    loadComponent: AdminLayout,
    canActivate: [adminGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./layouts/admin/dashboard/dashboard').then((m) => m.DashboardComponent),
      },
      // Thêm sau route 'dashboard':
      {
        path: 'profile',
        loadComponent: () =>
          import('./views/user/profile/profile.component').then((m) => m.ProfileComponent),
      },
      {
        path: 'users',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () => import('./layouts/admin/users/users').then((m) => m.Users),
      },
      {
        path: 'users/:id',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () =>
          import('./layouts/admin/users/users-detail').then((m) => m.UsersDetail),
      },
      {
        path: 'roles',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () => import('./layouts/admin/roles/roles').then((m) => m.Roles),
      },
      {
        path: 'staff/tickets',
        loadComponent: () =>
          import('./layouts/staff/staff-tickets/staff-tickets').then(
            (m) => m.StaffTicketsComponent,
          ),
      },
      {
        path: 'tickets',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () => import('./layouts/admin/tickets/tickets').then((m) => m.AdminTickets),
      },
      {
        path: 'tickets/:id',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () =>
          import('./layouts/admin/tickets/ticket-detail').then((m) => m.AdminTicketDetail),
      },
      {
        path: 'bookings',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () =>
          import('./layouts/admin/bookings/bookings').then((m) => m.AdminBookings),
      },
      {
        path: 'bookings/:id',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () =>
          import('./layouts/admin/bookings/booking-detail').then((m) => m.AdminBookingDetail),
      },
      {
        path: 'rooms',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () => import('./layouts/admin/rooms/rooms').then((m) => m.Rooms),
      },
      {
        path: 'hotels',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () =>
          import('./layouts/admin/hotels/manage-hotels.component').then((m) => m.ManageHotels),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ═══ WILDCARD ═══
  { path: '**', redirectTo: '' },
];
