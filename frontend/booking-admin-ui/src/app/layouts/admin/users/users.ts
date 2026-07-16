// ═══════════════════════════════════════════════════════════
// USERS PAGE (List + CRUD)
// Endpoint: /api/admin/users
// Filter: server-side (keyword, isActive, isLocked)
// Roles: load từ /api/admin/roles khi mở modal Assign Role
//
// Phase A.1: Mở rộng bảng Account
// - Cột "Tên" = firstName + lastName (từ Keycloak), fallback username
// - Cột "Role Permission" = aggregate permissions từ tất cả role
// - Actions: Khóa/Mở khóa nhanh + Kích hoạt/Vô hiệu nhanh
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { UserService } from '../../../core/services/user';
import { RoleService } from '../../../core/services/role';
import { Permission, Role, User, UserStatusKey } from '../../../core/models/auth.model';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-users',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzSwitchModule,
    NzPaginationModule,
    NzPopconfirmModule,
    NzAvatarModule,
    NzSpaceModule,
    NzTooltipModule,
    NzEmptyModule,
    NzAlertModule,
    NzCardModule,
    NzSpinModule,
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {
  private userService = inject(UserService);
  private roleService = inject(RoleService);
  private fb = inject(FormBuilder);
  private message = inject(NzMessageService);
  private router = inject(Router);

  // ── Data state ─────────────────────────────────────────
  users: User[] = [];
  isLoading = false;

  // ── Paging ─────────────────────────────────────────────
  pageIndex = 1; // nz-pagination: 1-based
  pageSize = 10;
  total = 0;

  // ── Filter / Search ────────────────────────────────────
  keyword = '';
  statusFilter: UserStatusKey | 'all' = 'all';

  // ── Modal state ────────────────────────────────────────
  isEditModalOpen = false;
  isRoleModalOpen = false;
  isPasswordModalOpen = false;
  isPermissionsModalOpen = false; // A.1: xem tất cả permission
  isSubmitting = false;
  selectedUser: User | null = null;
  selectedPermissions: Permission[] = []; // A.1

  // ── Forms ──────────────────────────────────────────────
  editForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: [''],
    lastName: [''],
    timezone: ['UTC'],
    active: [true],
  });

  roleForm = this.fb.nonNullable.group({
    roleCode: ['', Validators.required],
  });

  passwordForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
  });

  // Danh sách roles load từ API (khi mở modal)
  availableRoles: Role[] = [];
  isLoadingRoles = false;

  ngOnInit() {
    this.loadUsers();
  }

  // ── LOAD USERS (server-side filter + paging) ───────────
  loadUsers() {
    this.isLoading = true;

    // Tính isActive / isLocked từ statusFilter để gửi BE
    let isActive: boolean | undefined;
    let isLocked: boolean | undefined;
    if (this.statusFilter === 'active') isActive = true;
    else if (this.statusFilter === 'inactive') isActive = false;
    else if (this.statusFilter === 'locked') isLocked = true;

    this.userService
      .getUsers(
        this.pageIndex - 1, // backend 0-based
        this.pageSize,
        this.keyword || undefined,
        isActive,
        isLocked,
      )
      .subscribe({
        next: (res) => {
          this.users = res.content;
          this.total = res.totalElements;
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.message.error(extractErrorMessage(err, 'Failed to load users'));
        },
      });
  }

  // ── PAGING EVENTS ──────────────────────────────────────
  onPageChange(page: number) {
    this.pageIndex = page;
    this.loadUsers();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadUsers();
  }

  onSearch() {
    this.pageIndex = 1;
    this.loadUsers();
  }

  resetFilter() {
    this.keyword = '';
    this.statusFilter = 'all';
    this.pageIndex = 1;
    this.loadUsers();
  }

  onStatusFilterChange() {
    this.pageIndex = 1;
    this.loadUsers();
  }

  // ── STATUS ─────────────────────────────────────────────
  getStatus(user: User): UserStatusKey {
    if (!user.active) return 'inactive';
    if (user.locked) return 'locked';
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

  // ── A.1: TÊN ───────────────────────────────────────────
  // Lấy firstName + lastName, fallback username
  getDisplayName(user: User): string {
    const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
    return fullName || user.username;
  }

  // ── A.1: ROLE PERMISSION (aggregate) ───────────────────
  // Gộp permissions từ tất cả role, dedupe theo code
  getAggregatedPermissions(user: User): Permission[] {
    const map = new Map<string, Permission>();
    for (const role of user.roles ?? []) {
      for (const perm of role.permissions ?? []) {
        if (!map.has(perm.code)) {
          map.set(perm.code, perm);
        }
      }
    }
    return Array.from(map.values());
  }

  // Hiển thị tối đa 5 tag, nếu hơn thì tag cuối là "+N"
  getVisiblePermissions(user: User): Permission[] {
    return this.getAggregatedPermissions(user).slice(0, 5);
  }

  getHiddenPermissionCount(user: User): number {
    const total = this.getAggregatedPermissions(user).length;
    return Math.max(0, total - 5);
  }

  // Mở modal xem tất cả permission
  openPermissionsModal(user: User) {
    this.selectedUser = user;
    this.selectedPermissions = this.getAggregatedPermissions(user);
    this.isPermissionsModalOpen = true;
  }

  // ── EDIT MODAL ─────────────────────────────────────────
  openEditModal(user: User) {
    this.selectedUser = user;
    this.editForm.reset({
      email: user.email,
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      timezone: user.timezone ?? 'UTC',
      active: user.active,
    });
    this.isEditModalOpen = true;
  }

  submitEdit() {
    if (this.editForm.invalid || !this.selectedUser) return;
    this.isSubmitting = true;
    this.userService.updateUser(this.selectedUser.id, this.editForm.getRawValue()).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('User updated successfully');
        this.isEditModalOpen = false;
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.message.error(extractErrorMessage(err, 'Update failed'));
      },
    });
  }

  // ── ASSIGN ROLE MODAL ──────────────────────────────────
  openRoleModal(user: User) {
    this.selectedUser = user;
    this.roleForm.reset({
      roleCode: user.roles?.[0]?.code ?? '',
    });
    this.isRoleModalOpen = true;

    // Load roles từ API mỗi lần mở modal
    this.isLoadingRoles = true;
    this.roleService.getAll().subscribe({
      next: (roles) => {
        this.availableRoles = roles;
        this.isLoadingRoles = false;
      },
      error: (err) => {
        this.isLoadingRoles = false;
        this.message.error(extractErrorMessage(err, 'Failed to load roles'));
      },
    });
  }

  submitRole() {
    if (this.roleForm.invalid || !this.selectedUser) return;
    this.isSubmitting = true;
    this.userService.assignRole(this.selectedUser.id, this.roleForm.value.roleCode!).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('Role assigned');
        this.isRoleModalOpen = false;
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.message.error(extractErrorMessage(err, 'Assign role failed'));
      },
    });
  }

  // ── RESET PASSWORD MODAL ───────────────────────────────
  openPasswordModal(user: User) {
    this.selectedUser = user;
    this.passwordForm.reset();
    this.isPasswordModalOpen = true;
  }

  submitPassword() {
    if (this.passwordForm.invalid || !this.selectedUser) return;
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.message.error('Mật khẩu xác nhận không khớp');
      return;
    }
    this.isSubmitting = true;
    this.userService.resetPassword(this.selectedUser.id, newPassword).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('Password reset successfully');
        this.isPasswordModalOpen = false;
      },
      error: (err) => {
        this.isSubmitting = false;
        this.message.error(extractErrorMessage(err, 'Reset password failed'));
      },
    });
  }

  // ── A.1: KHÓA / MỞ KHÓA NHANH ─────────────────────────
  toggleLock(user: User) {
    const willLock = !user.locked;
    this.userService.updateUser(user.id, { active: user.active }).subscribe({
      next: () => {
        this.message.success(willLock ? `Đã khóa ${user.username}` : `Đã mở khóa ${user.username}`);
        this.loadUsers();
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Toggle lock failed'));
      },
    });
  }

  // ── DEACTIVATE ─────────────────────────────────────────
  deactivate(user: User) {
    this.userService.deactivateUser(user.id).subscribe({
      next: () => {
        this.message.success(`User ${user.username} deactivated`);
        this.loadUsers();
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Deactivate failed'));
      },
    });
  }

  // ── REVOKE ALL SESSIONS ────────────────────────────────
  revokeSessions(user: User) {
    this.userService.revokeAllSessions(user.id).subscribe({
      next: () => {
        this.message.success(`All sessions of ${user.username} revoked`);
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Revoke sessions failed'));
      },
    });
  }

  // ── NAVIGATE TO DETAIL ─────────────────────────────────
  viewDetail(user: User) {
    this.router.navigate(['/admin/users', user.id]);
  }

  // ── DELETE USER ──────────────────────────────────────────
  deleteUser(user: User) {
    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.message.success(`User ${user.username} deleted`);
        this.loadUsers();
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Delete failed'));
      },
    });
  }
}
