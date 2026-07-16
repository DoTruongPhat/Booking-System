// ═══════════════════════════════════════════════════════════
// MOCK CACHE FACTORY
// Helper tạo MockCacheService instance nhanh, tránh lặp code
// ở mỗi service (A.2, A.4, A.5, A.6).
//
// Ví dụ:
//
//   // roles.service.ts
//   private cache = inject(MockCacheFactory).create<Role>('roles', ROLE_SEED);
//
//   // rooms.service.ts
//   private cache = inject(MockCacheFactory).create<Room>('rooms', ROOM_SEED);
//
// Lý do cần factory: MockCacheService là class generic, mỗi
// service cần 1 instance riêng với name/seed riêng. Factory
// giúp inject 1 chỗ rồi tạo nhiều instance khác nhau.
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { MockCacheService } from './mock-cache.service';

@Injectable({ providedIn: 'root' })
export class MockCacheFactory {
  /**
   * Tạo 1 MockCacheService instance mới với name + seed riêng.
   *
   * @param name - Tên cache (vd: 'roles', 'rooms', 'bookings', 'hotels')
   * @param seed - Dữ liệu khởi tạo nếu sessionStorage rỗng
   * @param idField - Field ID (mặc định 'id')
   */
  create<T extends { id: string }>(
    name: string,
    seed: T[] = [],
    idField: keyof T = 'id' as keyof T,
  ): MockCacheService<T> {
    return new MockCacheService<T>(name, seed, idField);
  }
}
