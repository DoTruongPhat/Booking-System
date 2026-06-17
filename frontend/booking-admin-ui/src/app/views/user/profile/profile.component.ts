import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    NzCardModule,
    NzIconModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    NzAvatarModule,
    NzTagModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  user: any;
  phone = '';
  timezone = 'Asia/Ho_Chi_Minh';

  get userInitials(): string {
    return this.user?.username?.charAt(0).toUpperCase() || '?';
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL')) return 'Admin';
    if (roles.includes('MANAGER')) return 'Manager';
    if (roles.includes('STAFF')) return 'Staff';
    return 'User';
  }

  constructor(private auth: Auth) {
    this.user = this.auth.getUser();
  }

  save(): void {
    // TODO: Gọi API update phone
    console.log('Save phone:', this.phone);
  }
}
