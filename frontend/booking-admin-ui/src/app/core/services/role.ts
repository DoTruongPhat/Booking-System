// ═══════════════════════════════════════════════════════════
// ROLE SERVICE
// Gọi endpoint GET /api/admin/roles
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Role } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private apiUrl = '/api/admin/roles';

  constructor(private http: HttpClient) {}

  // GET /api/admin/roles - cần quyền ADMIN_ALL
  getAll(): Observable<Role[]> {
    return this.http.get<Role[]>(this.apiUrl);
  }
}
