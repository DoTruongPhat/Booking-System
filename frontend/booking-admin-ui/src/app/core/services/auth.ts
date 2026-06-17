// ═══════════════════════════════════════════════════════════
// AUTH SERVICE
// Login, logout, lưu token + user info vào localStorage
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { LoginResponse, AuthUser } from '../models/auth.model';

const TOKEN_KEY = 'token';
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

  // ── 1. LOGIN ─────────────────────────────────────────────
  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, {
      username,
      password,
    });
  }

  // ── 2. LOGOUT ────────────────────────────────────────────
  logout(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/logout`, {});
  }

  // ── 2.5 EXCHANGE CODE (Form B callback) ──────────────────
  exchangeCode(
    code: string,
    codeVerifier: string,
    redirectUri: string,
  ): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${this.apiUrl}/exchange`,
      { code, codeVerifier, redirectUri },
      { withCredentials: true },
    );
  }

  // ── 3. CHECK LOGGED IN ───────────────────────────────────
  isLoggedIn(): boolean {
    if (!hasStorage()) return false;
    return !!localStorage.getItem(TOKEN_KEY);
  }

  // ── 4. TOKEN HELPERS ─────────────────────────────────────
  getToken(): string | null {
    if (!hasStorage()) return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  saveToken(token: string): void {
    if (!hasStorage()) return;
    localStorage.setItem(TOKEN_KEY, token);
  }

  removeToken(): void {
    if (!hasStorage()) return;
    localStorage.removeItem(TOKEN_KEY);
  }

  // ── 5. USER HELPERS ──────────────────────────────────────
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

  // ── 6. ROLE HELPERS ──────────────────────────────────────
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

  // ── 6.5 PRIMARY ROLE ─────────────────────────────────────
  getPrimaryRole(): 'ADMIN' | 'MANAGER' | 'STAFF' | 'USER' {
    const roles = this.getRoles();
    if (roles.includes('ADMIN_ALL')) return 'ADMIN';
    if (roles.includes('MANAGER')) return 'MANAGER';
    if (roles.includes('STAFF')) return 'STAFF';
    return 'USER';
  }

  // ── 6.6 LANDING PATH (theo role) ─────────────────────────
  getLandingPath(): string {
    const role = this.getPrimaryRole();
    switch (role) {
      case 'ADMIN':
      case 'MANAGER':
      case 'STAFF':
        return '/admin/dashboard';
      case 'USER':
      default:
        return '/user/booking/my';
    }
  }

  // ── 7. CLEAR ALL (logout phía FE) ────────────────────────
  clearAll(): void {
    this.removeToken();
    this.removeUser();
  }
}
