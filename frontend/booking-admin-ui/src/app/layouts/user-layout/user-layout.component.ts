import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzLayoutModule,
    NzMenuModule,
    NzAvatarModule,
    NzDropDownModule,
    NzTooltipModule,
  ],
  templateUrl: './user-layout.component.html',
  styleUrl: './user-layout.component.scss',
})
export class UserLayoutComponent {
  user: any;

  navItems = [
    { path: '/user/bookings', label: 'Đặt phòng của tôi', icon: 'calendar' },
    { path: '/user/booking/new', label: 'Đặt phòng mới', icon: 'plus-circle' },
    { path: '/user/profile', label: 'Tài khoản', icon: 'user' },
    { path: '/user/tickets', label: 'Hỗ trợ', icon: 'inbox' },
  ];

  constructor(
    private auth: Auth,
    private router: Router,
  ) {
    this.user = this.auth.getUser();
  }

  /** User có role admin/manager/staff thì được vào admin */
  canAccessAdmin(): boolean {
    const roles = this.user?.roles || [];
    return roles.includes('ADMIN_ALL') || roles.includes('MANAGER') || roles.includes('STAFF');
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearAll();
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.auth.clearAll();
        this.router.navigate(['/auth/login']);
      },
    });
  }

  get userInitials(): string {
    return this.user?.username?.charAt(0).toUpperCase() || '?';
  }
}