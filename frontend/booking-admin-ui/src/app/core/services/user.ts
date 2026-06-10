// ═══════════════════════════════════════════════════════════
// USER SERVICE
// Gọi các endpoint admin/users của auth-service
// Map theo AdminController.java
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  User,
  SpringPage,
  UpdateUserRequest,
  AssignRoleRequest,
  AdminResetPasswordRequest,
  ChangePasswordRequest,
} from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = '/api/admin/users';

  constructor(private http: HttpClient) {}

  // ── LIST ─────────────────────────────────────────────────
  // Backend AdminController.getUsers hỗ trợ filter:
  //   page, size, keyword (search username/email), isActive, isLocked
  getUsers(
    page: number = 0,
    size: number = 10,
    keyword?: string,
    isActive?: boolean,
    isLocked?: boolean,
  ): Observable<SpringPage<User>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    if (isActive !== undefined && isActive !== null) {
      params = params.set('isActive', String(isActive));
    }
    if (isLocked !== undefined && isLocked !== null) {
      params = params.set('isLocked', String(isLocked));
    }

    return this.http.get<SpringPage<User>>(this.apiUrl, { params });
  }

  // ── DETAIL ───────────────────────────────────────────────
  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  // ── UPDATE (email, timezone, active) ────────────────────
  // Lưu ý: BE field là "active" không phải "isActive"
  updateUser(id: string, body: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, body);
  }

  // ── DEACTIVATE ───────────────────────────────────────────
  // Soft delete: set isActive=false
  deactivateUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ── ASSIGN ROLE ──────────────────────────────────────────
  // BE: POST /api/admin/users/{id}/roles  body: { roleCode }
  // Lưu ý: BE chỉ ADD role vào set hiện tại, KHÔNG replace
  // → FE muốn "thay thế" thì phải gọi deactivate các role cũ thủ công
  assignRole(id: string, roleCode: string): Observable<User> {
    const body: AssignRoleRequest = { roleCode };
    return this.http.post<User>(`${this.apiUrl}/${id}/roles`, body);
  }

  // ── ADMIN RESET PASSWORD ─────────────────────────────────
  // BE: PUT /api/admin/users/{id}/password body: { newPassword }
  // Trả về { message: "Password reset successfully" }
  resetPassword(id: string, newPassword: string): Observable<{ message: string }> {
    const body: AdminResetPasswordRequest = { newPassword };
    return this.http.put<{ message: string }>(`${this.apiUrl}/${id}/password`, body);
  }

  // ── REVOKE ALL SESSIONS ──────────────────────────────────
  // BE: DELETE /api/admin/users/{userId}/revoke
  // Kick user ra khỏi tất cả thiết bị
  revokeAllSessions(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}/revoke`);
  }

  // ── REVOKE 1 SESSION (theo jti) ──────────────────────────
  // BE: DELETE /api/admin/sessions/{jti}
  revokeSession(jti: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/sessions/${jti}`);
  }

  // ── PROFILE (cá nhân user) ───────────────────────────────
  // BE: GET /api/users/me
  getMyProfile(): Observable<User> {
    return this.http.get<User>('/api/users/me');
  }

  // BE: PUT /api/users/me body: { email, timezone }
  updateMyProfile(body: { email?: string; timezone?: string }): Observable<User> {
    return this.http.put<User>('/api/users/me', body);
  }

  // BE: PUT /api/users/me/password body: { currentPassword, newPassword }
  changeMyPassword(body: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>('/api/users/me/password', body);
  }
}
