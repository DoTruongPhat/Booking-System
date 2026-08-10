import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard, adminGuard } from './core/guards/role.guard';
import { onboardingGuard } from './core/guards/onboarding.guard';

const AdminLayout = () =>
  import('./layouts/admin-layout/admin-layout.component').then((m) => m.AdminLayoutComponent);

const AuthLayout = () =>
  import('./layouts/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent);

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./views/home/home.component').then((m) => m.HomeComponent),
  },

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

  {
    path: 'booking/success/:id',
    loadComponent: () =>
      import('./views/booking/success/success.component').then((m) => m.BookingSuccessComponent),
  },
  {
    path: 'booking/checkout/:bookingId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./views/booking/checkout/checkout.component').then((m) => m.CheckoutComponent),
  },

  {
    path: 'payment',
    children: [
      {
        path: 'callback',
        loadComponent: () =>
          import('./views/payment/callback/callback.component').then(
            (m) => m.PaymentCallbackComponent,
          ),
      },
      {
        path: 'success',
        loadComponent: () =>
          import('./views/payment/success/success.component').then(
            (m) => m.PaymentSuccessComponent,
          ),
      },
      {
        path: 'failed',
        loadComponent: () =>
          import('./views/payment/failed/failed.component').then((m) => m.PaymentFailedComponent),
      },
    ],
  },

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
        path: 'bookings/:id',
        loadComponent: () =>
          import('./views/user/booking/booking-detail/booking-detail.component').then(
            (m) => m.UserBookingDetailComponent,
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
        path: 'payments',
        loadComponent: () =>
          import('./views/user/payments/payments.component').then((m) => m.UserPaymentsComponent),
      },
      {
        path: 'booking/new',
        redirectTo: '/hotels',
        pathMatch: 'full',
      },
      { path: '', redirectTo: 'bookings', pathMatch: 'full' },
    ],
  },

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
        path: 'audit-logs',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () =>
          import('./layouts/admin/audit-logs/audit-logs').then((m) => m.AuditLogs),
      },
      {
        path: 'staff/tickets',
        redirectTo: 'tickets',
        pathMatch: 'full',
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
        path: 'reports',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () => import('./layouts/admin/reports/reports').then((m) => m.Reports),
      },
      {
        path: 'payments',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () => import('./layouts/admin/payments/payments').then((m) => m.Payments),
      },
      {
        path: 'rooms',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () => import('./layouts/admin/rooms/rooms').then((m) => m.Rooms),
      },
      {
        path: 'room-types',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () =>
          import('./layouts/admin/room-types/room-types').then((m) => m.RoomTypes),
      },
      {
        path: 'hotels',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        loadComponent: () =>
          import('./layouts/admin/hotels/manage-hotels.component').then((m) => m.ManageHotels),
      },
      {
        path: 'promotions',
        redirectTo: 'vouchers',
        pathMatch: 'full',
      },
      {
        path: 'vouchers',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN', 'HOST'])],
        data: {
          title: 'Mã giảm giá',
          subtitle: 'Quản lý voucher, điều kiện áp dụng và số lượt sử dụng',
          icon: 'tags',
          apiNote:
            'Đã có backend CRUD và API validate voucher. Bước còn lại là nối voucher vào luồng tạo booking nếu muốn giảm giá khi thanh toán.',
        },
        loadComponent: () =>
          import('./layouts/admin/marketing/marketing-placeholder').then(
            (m) => m.MarketingPlaceholder,
          ),
      },
      {
        path: 'workflow',
        canActivate: [roleGuard(['ADMIN_ALL', 'ADMIN'])],
        loadComponent: () =>
          import('./layouts/admin/workflow/workflow').then((m) => m.WorkflowComponent),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: '' },
];
