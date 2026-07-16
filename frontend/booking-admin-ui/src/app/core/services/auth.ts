// ═══════════════════════════════════════════════════════════
// AUTH SERVICE
// ═══════════════════════════════════════════════════════════
// TOKEN STRATEGY (theo yêu cầu user):
// - access_token: lưu trong HttpOnly cookie (BE set qua Set-Cookie)
// - refresh_token: cũng lưu trong HttpOnly cookie (cùng Set-Cookie)
// - FE KHÔNG đọc được token từ cookie (HttpOnly)
// - Interceptor KHÔNG gắn Authorization header (browser tự gửi cookie)
// - Khi access_token hết hạn → FE gọi /api/auth/refresh
// → BE đọc refresh_token từ cookie → trả access_token mới
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LoginResponse,
  AuthUser,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
} from '../models/auth.model';

const USER_KEY = 'user';

function hasStorage(): boolean {
  return typeof window !== 'undefined' && typeof localStorage !== 'undefined';
}

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  // ── 1. LOGIN (Form A) ───────────────────────────────────
  // BE sẽ set HttpOnly cookie 'access_token' + 'refresh_token' qua Set-Cookie
  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/login`,
      { username, password },
      { withCredentials: true },
    );
  }

  // ── 2. LOGOUT ────────────────────────────────────────────
  // BE xoá HttpOnly cookie + invalidate session
  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true });
  }

  // ── 2.5 LOGIN WITH KEYCLOAK (SSO via PKCE) ──────────────
  loginWithKeycloak(): void {
    window.location.href = 'http://localhost:8081/api/auth/sso/login';
  }

  loginWithGoogle(): void {
    window.location.href = 'http://localhost:8081/api/auth/sso/login?provider=google';
  }

  // ── 2.7 REGISTER ─────────────────────────────────────────
  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register`, payload, {
      withCredentials: true,
    });
  }

  // ── 2.8 FORGOT PASSWORD ─────────────────────────────────
  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/forgot-password`,
      { email },
      { withCredentials: true },
    );
  }

  // ── 2.9 RESET PASSWORD ──────────────────────────────────
  resetPassword(email: string, otp: string, newPassword: string): Observable<void> {
    const body: ResetPasswordRequest = { email, otp, newPassword };
    return this.http.post<void>(`${this.apiUrl}/reset-password`, body, { withCredentials: true });
  }

  completeProfile(username: string | null, newPassword: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/complete-profile`,
      { username, newPassword },
      { withCredentials: true },
    );
  }

  // ── 2.10 REFRESH TOKEN ──────────────────────────────────
  // Gọi khi access_token hết hạn (401 response)
  // BE đọc refresh_token từ cookie → trả access_token mới
  refreshToken(): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true });
  }

  // ── 3. CHECK LOGGED IN ──────────────────────────────────
  // CHÚ Ý: Token HttpOnly nên FE không đọc được.
  // Cách check: dựa vào presence của user info trong localStorage.
  isLoggedIn(): boolean {
    return !!this.getUser();
  }

  // ── 4. TOKEN HELPERS (DEPRECATED) ───────────────────────
  // Token giờ do BE quản lý qua HttpOnly cookie.
  // Giữ method để tương thích ngược (no-op).
  getToken(): string | null {
    return null;
  }

  saveToken(_token: string): void {
    // no-op - BE tự lưu HttpOnly cookie
  }

  removeToken(): void {
    // no-op - BE tự xoá cookie ở logout endpoint
  }

  // ── 5. USER HELPERS ─────────────────────────────────────
  // User info KHÔNG nhạy cảm (username, email, roles) → lưu localStorage OK.
  saveUser(user: AuthUser): void {
    if (!hasStorage()) return;
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  getUser(): AuthUser | null {
    if (!hasStorage()) return null;
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  removeUser(): void {
    if (!hasStorage()) return;
    localStorage.removeItem(USER_KEY);
  }

  // ── 6. ROLE HELPERS ─────────────────────────────────────
  hasRole(role: string): boolean {
    const u = this.getUser();
    return u?.roles?.includes(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    const u = this.getUser();
    if (!u?.roles) return false;
    return roles.some((r) => u.roles!.includes(r));
  }

  getRoles(): string[] {
    return this.getUser()?.roles ?? [];
  }

  // ── 6.5 PRIMARY ROLE ────────────────────────────────────

  getPrimaryRole(): 'ADMIN' | 'HOST' | 'USER' {
    const roles = this.getRoles();
    if (roles.includes('ADMIN_ALL') || roles.includes('ADMIN')) return 'ADMIN';
    if (roles.includes('HOST')) return 'HOST';
    return 'USER';
  }

  getLandingPath(): string {
    const role = this.getPrimaryRole();
    switch (role) {
      case 'ADMIN':
      case 'HOST':
        return '/admin/dashboard';
      default:
        return '/user/bookings';
    }
  }

  getProfile(): Observable<any> {
    return this.http.get('/api/users/me', { withCredentials: true });
  }

  // ── 7. CLEAR ALL (logout phía FE) ───────────────────────
  // Chỉ xoá user info. Token cookie đã bị BE xoá qua /auth/logout.
  clearAll(): void {
    this.removeUser();
  }
}
