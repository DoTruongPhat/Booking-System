import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-admin-layout',
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzLayoutModule,
    NzMenuModule,
    NzDropDownModule,
    NzTooltipModule,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  isCollapsed = false;
  user: any;

  constructor(
    private auth: Auth,
    private router: Router,
  ) {
    this.user = this.auth.getUser();
  }

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  get userInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() || 'A';
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL') || roles.includes('ADMIN')) return 'Quản trị viên';
    if (roles.includes('HOST')) return 'Chủ khách sạn';
    return 'Người dùng';
  }

  logout() {
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

  hasAnyRole(roles: string[]): boolean {
    const userRoles = this.user?.roles || [];
    return roles.some((r) => userRoles.includes(r));
  }
}
