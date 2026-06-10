import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Router } from '@angular/router';
import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-admin-layout',
  imports: [CommonModule, RouterModule, NzIconModule, NzLayoutModule, NzMenuModule],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  isCollapsed = false;

  constructor(
    private auth: Auth,
    private router: Router,
  ) {}

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  logout() {
    // Xóa token
    this.auth.removeToken();
    // Chuyển về trang login
    this.router.navigate(['/auth/login']);
  }
}
