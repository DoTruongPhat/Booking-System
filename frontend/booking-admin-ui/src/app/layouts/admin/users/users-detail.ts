// ═══════════════════════════════════════════════════════════
// USERS DETAIL PAGE
// Hiển thị chi tiết 1 user + các thao tác admin
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { UserService } from '../../../core/services/user';
import { User, UserStatusKey } from '../../../core/models/auth.model';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-users-detail',
  imports: [
    CommonModule,
    NzCardModule,
    NzDescriptionsModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzAvatarModule,
    NzSpinModule,
    NzDividerModule,
    NzPopconfirmModule,
    NzSpaceModule,
  ],
  templateUrl: './users-detail.html',
  styleUrl: './users.scss',
})
export class UsersDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private message = inject(NzMessageService);

  user: User | null = null;
  isLoading = false;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadUser(id);
  }

  loadUser(id: string) {
    this.isLoading = true;
    this.userService.getUserById(id).subscribe({
      next: (u) => {
        this.user = u;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.message.error(extractErrorMessage(err, 'Failed to load user'));
        this.goBack();
      },
    });
  }

  getStatus(user: User): UserStatusKey {
    if (!user.isActive) return 'inactive';
    if (user.isLocked) return 'locked';
    return 'active';
  }

  getStatusColor(status: UserStatusKey): string {
    const map: Record<UserStatusKey, string> = {
      active: 'green',
      inactive: 'default',
      locked: 'red',
    };
    return map[status];
  }

  deactivate() {
    if (!this.user) return;
    this.userService.deactivateUser(this.user.id).subscribe({
      next: () => {
        this.message.success('User deactivated');
        this.loadUser(this.user!.id);
      },
      error: (err) => this.message.error(extractErrorMessage(err, 'Deactivate failed')),
    });
  }

  revokeSessions() {
    if (!this.user) return;
    this.userService.revokeAllSessions(this.user.id).subscribe({
      next: () => this.message.success('All sessions revoked'),
      error: (err) => this.message.error(extractErrorMessage(err, 'Revoke failed')),
    });
  }

  goBack() {
    this.router.navigate(['/admin/users']);
  }
}
