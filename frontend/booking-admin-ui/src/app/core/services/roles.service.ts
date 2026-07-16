// ═══════════════════════════════════════════════════════════
// ROLES SERVICE (A.2)
// CRUD role + permission sử dụng MockCacheService
// Cache: sessionStorage key = 'mock_roles'
// Seed: 3 role (ADMIN/HOST/USER) + 32 permission
// ═══════════════════════════════════════════════════════════

import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MockCacheService } from './mock-cache.service';
import {
  AdminPermission,
  AdminRole,
  PERMISSION_SEED,
  PermissionCode,
  ROLE_SEED,
} from '../models/role-admin.model';

@Injectable({ providedIn: 'root' })
export class RolesService {
  // 2 cache riêng: 1 cho roles, 1 cho permissions catalog
  private rolesCache = new MockCacheService<AdminRole>('roles', ROLE_SEED);
  private permissionsCache = new MockCacheService<AdminPermission>(
    'permissions',
    PERMISSION_SEED,
  );

  // ── ROLES CRUD ──────────────────────────────────────────

  /** Stream danh sách roles */
  getAll$(): Observable<AdminRole[]> {
    return this.rolesCache.list$();
  }

  /** Lấy tất cả roles (sync) */
  getAll(): AdminRole[] {
    return this.rolesCache.list();
  }

  /** Lấy role theo id */
  getById(id: string): AdminRole | undefined {
    return this.rolesCache.findById(id);
  }

  /** Lấy role theo code */
  getByCode(code: string): AdminRole | undefined {
    return this.rolesCache.filter((r) => r.code === code)[0];
  }

  /** Tạo role mới */
  create(input: {
    code: string;
    name: string;
    description: string;
    permissions: PermissionCode[];
    active: boolean;
  }): AdminRole {
    const code = input.code.toUpperCase().trim();

    // Validate code unique
    if (this.rolesCache.filter((r) => r.code === code).length > 0) {
      throw new Error(`Role code "${code}" đã tồn tại`);
    }

    const now = new Date().toISOString();
    const newRole: AdminRole = {
      id: this.rolesCache.generateId('R-'),
      code: code as AdminRole['code'],
      name: input.name.trim(),
      description: input.description.trim(),
      permissions: input.permissions,
      active: input.active,
      createdAt: now,
      updatedAt: now,
    };

    return this.rolesCache.add(newRole);
  }

  /** Update role */
  update(id: string, patch: Partial<AdminRole>): AdminRole | null {
    const role = this.getById(id);
    if (!role) return null;

    // Nếu đổi code → check unique
    if (patch.code && patch.code !== role.code) {
      const newCode = patch.code.toUpperCase().trim();
      if (this.rolesCache.filter((r) => r.code === newCode).length > 0) {
        throw new Error(`Role code "${newCode}" đã tồn tại`);
      }
      patch.code = newCode as AdminRole['code'];
    }

    return this.rolesCache.update(id, { ...patch, updatedAt: new Date().toISOString() });
  }

  /** Xóa role */
  delete(id: string): boolean {
    const role = this.getById(id);
    if (!role) return false;

    // Không cho xóa role mặc định (ADMIN/HOST/USER)
    if (['R-001', 'R-002', 'R-003'].includes(id)) {
      throw new Error('Không thể xóa role hệ thống (ADMIN/HOST/USER)');
    }

    return this.rolesCache.delete(id);
  }

  /** Đếm số user đang dùng role này (dùng UserService khi có BE) */
  countUsersUsingRole(_roleCode: string): number {
    // Mock: random từ 0-5
    return Math.floor(Math.random() * 6);
  }

  // ── PERMISSIONS (chỉ đọc) ───────────────────────────────

  /** Lấy toàn bộ permission catalog */
  getAllPermissions(): AdminPermission[] {
    return this.permissionsCache.list();
  }

  /** Stream permission catalog */
  getAllPermissions$(): Observable<AdminPermission[]> {
    return this.permissionsCache.list$();
  }

  /** Lấy permission objects từ danh sách code */
  getPermissionsByCodes(codes: PermissionCode[]): AdminPermission[] {
    return this.getAllPermissions().filter((p) => codes.includes(p.code));
  }

  // ── RESET (dùng cho debug) ──────────────────────────────

  /** Reset về seed ban đầu */
  resetToSeed(): void {
    this.rolesCache.reset(ROLE_SEED);
    this.permissionsCache.reset(PERMISSION_SEED);
  }
}
