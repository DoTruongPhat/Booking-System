// ═══════════════════════════════════════════════════════════
// MOCK CACHE SERVICE
// Foundation cho Phase A - dùng chung cho A.2 (Roles), A.4
// (Bookings), A.5 (Rooms), A.6 (Hotel Rooms).
//
// Cơ chế:
// - Lưu data trong sessionStorage (key: mock_<tên>)
// - Đồng thời giữ BehaviorSubject trong memory → mọi
//   component subscribe đều nhận update real-time
// - Khi một service ghi (add/update/delete) → BehaviorSubject
//   emit → tất cả component cùng view update ngay
//
// Lý do chọn sessionStorage (không phải localStorage):
// - Share được giữa các tab trong 1 session
// - Tự xoá khi đóng browser (data tạm, không ô nhiễm)
// - Không cần backend
//
// Ví dụ sử dụng:
//
//   interface Role { id: string; code: string; name: string; }
//   const cache = new MockCacheService<Role>('roles', seedData);
//
//   cache.list$().subscribe(items => console.log(items));
//   cache.list();           // sync get
//   cache.add(newItem);
//   cache.update(id, patch);
//   cache.delete(id);
//   cache.reset(seedData);  // reset về seed
// ═══════════════════════════════════════════════════════════

import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Service cache generic cho mock data.
 *
 * @template T - Kiểu dữ liệu cần cache (phải có `id: string`)
 */
export class MockCacheService<T extends { id: string }> {
  private readonly storageKey: string;
  private readonly memCache: BehaviorSubject<T[]>;
  private readonly idField: keyof T;

  /**
   * @param name - Tên cache (sẽ thành `mock_<name>` trong sessionStorage)
   * @param seed - Dữ liệu khởi tạo nếu sessionStorage rỗng
   * @param idField - Tên field ID (mặc định 'id')
   */
  constructor(name: string, seed: T[] = [], idField: keyof T = 'id' as keyof T) {
    this.storageKey = `mock_${name}`;
    this.idField = idField;

    // 1. Load từ sessionStorage trước
    const stored = this.readStorage();
    if (stored !== null) {
      this.memCache = new BehaviorSubject<T[]>(stored);
    } else {
      // 2. Nếu chưa có → dùng seed + lưu lại
      this.writeStorage(seed);
      this.memCache = new BehaviorSubject<T[]>(seed);
    }
  }

  // ── READ ─────────────────────────────────────────────────

  /** Observable stream - subscribe để nhận update real-time */
  list$(): Observable<T[]> {
    return this.memCache.asObservable();
  }

  /** Sync get - lấy snapshot hiện tại */
  list(): T[] {
    return this.memCache.value;
  }

  /** Tìm 1 item theo id */
  findById(id: string): T | undefined {
    return this.list().find((item) => item[this.idField] === id);
  }

  /** Filter items theo predicate */
  filter(predicate: (item: T) => boolean): T[] {
    return this.list().filter(predicate);
  }

  // ── WRITE ────────────────────────────────────────────────

  /** Thêm 1 item mới (id không được trùng) */
  add(item: T): T {
    const items = this.list();
    if (items.some((existing) => existing[this.idField] === item[this.idField])) {
      throw new Error(
        `[MockCache] Item với ${String(this.idField)}="${item[this.idField]}" đã tồn tại`,
      );
    }
    const next = [...items, item];
    this.persist(next);
    return item;
  }

  /** Thêm nhiều items cùng lúc (dùng cho seed) */
  addMany(items: T[]): T[] {
    const next = [...this.list(), ...items];
    this.persist(next);
    return items;
  }

  /** Update 1 item theo id (merge patch vào item hiện tại) */
  update(id: string, patch: Partial<T>): T | null {
    const items = this.list();
    const index = items.findIndex((item) => item[this.idField] === id);
    if (index === -1) return null;

    const updated = { ...items[index], ...patch };
    const next = [...items];
    next[index] = updated;
    this.persist(next);
    return updated;
  }

  /** Xoá 1 item theo id */
  delete(id: string): boolean {
    const items = this.list();
    const next = items.filter((item) => item[this.idField] !== id);
    if (next.length === items.length) return false;
    this.persist(next);
    return true;
  }

  /** Replace toàn bộ (dùng cho reset hoặc bulk update) */
  replaceAll(items: T[]): void {
    this.persist([...items]);
  }

  /** Reset về seed ban đầu + xoá sessionStorage */
  reset(seed: T[]): void {
    sessionStorage.removeItem(this.storageKey);
    this.persist(seed);
  }

  /** Xoá hết (items rỗng) */
  clear(): void {
    this.persist([]);
  }

  // ── HELPERS ──────────────────────────────────────────────

  /** Đếm tổng số items */
  count(): number {
    return this.list().length;
  }

  /** Generate ID mới theo pattern (vd: "BK-001", "R-001") */
  generateId(prefix: string, padLength = 3): string {
    const items = this.list();
    let max = 0;
    for (const item of items) {
      const idValue = String(item[this.idField] ?? '');
      if (idValue.startsWith(prefix)) {
        const numPart = idValue.slice(prefix.length);
        const num = parseInt(numPart, 10);
        if (!isNaN(num) && num > max) max = num;
      }
    }
    return `${prefix}${String(max + 1).padStart(padLength, '0')}`;
  }

  // ── INTERNAL ─────────────────────────────────────────────

  private persist(items: T[]): void {
    this.writeStorage(items);
    this.memCache.next(items);
  }

  private readStorage(): T[] | null {
    if (typeof window === 'undefined' || typeof sessionStorage === 'undefined') {
      return null;
    }
    const raw = sessionStorage.getItem(this.storageKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T[];
    } catch {
      // JSON hỏng → xoá đi để seed lại
      sessionStorage.removeItem(this.storageKey);
      return null;
    }
  }

  private writeStorage(items: T[]): void {
    if (typeof window === 'undefined' || typeof sessionStorage === 'undefined') {
      return;
    }
    sessionStorage.setItem(this.storageKey, JSON.stringify(items));
  }
}
