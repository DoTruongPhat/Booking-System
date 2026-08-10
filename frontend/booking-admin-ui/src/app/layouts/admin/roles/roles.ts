// ═══════════════════════════════════════════════════════════
// ROLES PAGE (A.2)
// Trang quản lý Roles + Permissions
// - List 3 role mặc định: ADMIN, HOST, USER
// - CRUD role (Create/Edit/Delete)
// - Modal multi-select permission theo resource group
// - Mock data dùng MockCacheService
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
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
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { RolesService } from '../../../core/services/roles.service';
import {
  AdminPermission,
  AdminRole,
  PermissionCode,
} from '../../../core/models/role-admin.model';

interface PermissionGroup {
  resource: string;
  label: string;
  icon: string;
  permissions: AdminPermission[];
}

@Component({
  selector: 'app-roles',
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
    NzPopconfirmModule,
    NzSpaceModule,
    NzTooltipModule,
    NzCardModule,
    NzCheckboxModule,
    NzDividerModule,
    NzAlertModule,
    NzEmptyModule,
    NzAvatarModule,
  ],
  templateUrl: './roles.html',
  styleUrl: './roles.scss',
})
export class Roles implements OnInit {
  private rolesService = inject(RolesService);
  private fb = inject(FormBuilder);
  private message = inject(NzMessageService);

  // ── Data ────────────────────────────────────────────────
  roles: AdminRole[] = [];
  allPermissions: AdminPermission[] = [];
  permissionGroups: PermissionGroup[] = [];

  // ── Modal state ─────────────────────────────────────────
  isFormModalOpen = false;
  isPermissionDetailModalOpen = false;
  isSubmitting = false;
  editingRole: AdminRole | null = null;
  selectedRole: AdminRole | null = null;
  selectedPermissions: AdminPermission[] = [];

  // ── Forms ───────────────────────────────────────────────
  form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^[A-Z_]+$/)]],
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(5)]],
    active: [true],
  });

  // Checkbox state cho từng permission
  permissionChecks: Record<string, boolean> = {};

  // Resource labels (dùng để group hiển thị)
  private readonly resourceLabels: Record<string, { label: string; icon: string }> = {
    user: { label: 'Quản lý User', icon: 'user' },
    role: { label: 'Quản lý Role', icon: 'safety-certificate' },
    hotel: { label: 'Khách sạn', icon: 'home' },
    room: { label: 'Phòng', icon: 'build' },
    room_type: { label: 'Loại phòng', icon: 'appstore' },
    booking: { label: 'Đặt phòng', icon: 'calendar' },
    payment: { label: 'Thanh toán', icon: 'credit-card' },
    ticket: { label: 'Phiếu hỗ trợ', icon: 'inbox' },
    dashboard: { label: 'Dashboard', icon: 'dashboard' },
  };

  ngOnInit() {
    // Subscribe stream
    this.rolesService.getAll$().subscribe((roles) => {
      this.roles = roles;
    });
    this.rolesService.getAllPermissions$().subscribe((perms) => {
      this.allPermissions = perms;
      this.permissionGroups = this.groupPermissions(perms);
    });
  }

  // ── GROUP PERMISSIONS BY RESOURCE ───────────────────────
  private groupPermissions(perms: AdminPermission[]): PermissionGroup[] {
    const map = new Map<string, AdminPermission[]>();
    for (const p of perms) {
      if (!map.has(p.resource)) map.set(p.resource, []);
      map.get(p.resource)!.push(p);
    }
    return Array.from(map.entries()).map(([resource, permissions]) => ({
      resource,
      label: this.resourceLabels[resource]?.label ?? resource,
      icon: this.resourceLabels[resource]?.icon ?? 'tag',
      permissions: permissions.sort((a, b) => a.code.localeCompare(b.code)),
    }));
  }

  // ── ROLE COLOR ──────────────────────────────────────────
  getRoleColor(code: string): string {
    const map: Record<string, string> = {
      ADMIN: 'red',
      HOST: 'orange',
      USER: 'green',
    };
    return map[code] ?? 'default';
  }

  // ── CRUD: CREATE / EDIT ─────────────────────────────────
  openCreateModal() {
    this.editingRole = null;
    this.form.reset({
      code: '',
      name: '',
      description: '',
      active: true,
    });
    this.resetPermissionChecks();
    this.isFormModalOpen = true;
  }

  openEditModal(role: AdminRole) {
    this.editingRole = role;
    this.form.reset({
      code: role.code,
      name: role.name,
      description: role.description,
      active: role.active,
    });
    this.form.controls.code.disable(); // không cho đổi code khi edit
    this.resetPermissionChecks();
    // Check sẵn permission của role
    for (const code of role.permissions) {
      this.permissionChecks[code] = true;
    }
    this.isFormModalOpen = true;
  }

  resetPermissionChecks() {
    this.permissionChecks = {};
    for (const p of this.allPermissions) {
      this.permissionChecks[p.code] = false;
    }
  }

  // Check all / uncheck all theo group
  toggleGroup(group: PermissionGroup, checked: boolean) {
    for (const p of group.permissions) {
      this.permissionChecks[p.code] = checked;
    }
  }

  isGroupAllChecked(group: PermissionGroup): boolean {
    return group.permissions.every((p) => this.permissionChecks[p.code]);
  }

  isGroupIndeterminate(group: PermissionGroup): boolean {
    const checked = group.permissions.filter((p) => this.permissionChecks[p.code]).length;
    return checked > 0 && checked < group.permissions.length;
  }

  // Đếm số permission đã chọn
  getSelectedPermissionCount(): number {
    return Object.values(this.permissionChecks).filter((v) => v).length;
  }

  // Lấy danh sách permission code đã chọn
  getSelectedPermissionCodes(): PermissionCode[] {
    return Object.entries(this.permissionChecks)
      .filter(([_, v]) => v)
      .map(([k, _]) => k as PermissionCode);
  }

  submitForm() {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach((c) => c.markAsTouched());
      return;
    }

    const permissions = this.getSelectedPermissionCodes();
    if (permissions.length === 0) {
      this.message.warning('Vui lòng chọn ít nhất 1 permission');
      return;
    }

    this.isSubmitting = true;
    try {
      const raw = this.form.getRawValue();
      if (this.editingRole) {
        // UPDATE
        this.rolesService.update(this.editingRole.id, {
          name: raw.name,
          description: raw.description,
          permissions,
          active: raw.active,
        });
        this.message.success(`Đã cập nhật role "${raw.name}"`);
      } else {
        // CREATE
        this.rolesService.create({
          code: raw.code,
          name: raw.name,
          description: raw.description,
          permissions,
          active: raw.active,
        });
        this.message.success(`Đã tạo role "${raw.name}"`);
      }
      this.isFormModalOpen = false;
    } catch (err: any) {
      this.message.error(err.message ?? 'Có lỗi xảy ra');
    } finally {
      this.isSubmitting = false;
    }
  }

  // ── DELETE ──────────────────────────────────────────────
  deleteRole(role: AdminRole) {
    try {
      this.rolesService.delete(role.id);
      this.message.success(`Đã xóa role "${role.name}"`);
    } catch (err: any) {
      this.message.error(err.message ?? 'Không thể xóa role');
    }
  }

  // ── VIEW PERMISSIONS DETAIL ─────────────────────────────
  openPermissionDetail(role: AdminRole) {
    this.selectedRole = role;
    this.selectedPermissions = this.rolesService.getPermissionsByCodes(role.permissions);
    this.isPermissionDetailModalOpen = true;
  }

  /** Check role có ít nhất 1 permission trong group không (dùng cho modal detail) */
  hasAnyPermissionInGroup(group: PermissionGroup): boolean {
    if (!this.selectedRole) return false;
    return group.permissions.some((p) =>
      this.selectedRole!.permissions.includes(p.code),
    );
  }

  /** Check 1 permission có thuộc role đang xem không */
  isPermissionInRole(p: AdminPermission): boolean {
    if (!this.selectedRole) return false;
    return this.selectedRole.permissions.includes(p.code);
  }

  // ── UTILS ───────────────────────────────────────────────
  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('vi-VN');
  }

  isSystemRole(id: string): boolean {
    return ['R-001', 'R-002', 'R-003'].includes(id);
  }
}
