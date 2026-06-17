import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { Auth } from '../../core/services/auth';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzButtonModule,
    NavbarComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  user: any;

  constructor(
    private auth: Auth,
    private router: Router,
  ) {
    this.user = this.auth.getUser();
  }

  // Nếu đã login → redirect về dashboard
  goToDashboard() {
    if (this.user) {
      this.router.navigateByUrl(this.auth.getLandingPath());
    } else {
      this.router.navigate(['/auth/login']);
    }
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }

  logout() {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearAll();
        this.user = null;
        this.router.navigate(['/']);
      },
      error: () => {
        this.auth.clearAll();
        this.user = null;
      }
    });
  }

  get userInitials(): string {
    return this.user?.username?.charAt(0).toUpperCase() || '?';
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL')) return 'Admin';
    if (roles.includes('MANAGER')) return 'Manager';
    if (roles.includes('STAFF')) return 'Staff';
    return 'Khách hàng';
  }
}