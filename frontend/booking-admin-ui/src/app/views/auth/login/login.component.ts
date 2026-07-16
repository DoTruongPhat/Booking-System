// ═══════════════════════════════════════════════════════════
// LOGIN COMPONENT — BFF Pattern
// Flow:
// 1. Click "Đăng nhập" → GET /api/auth/sso/login
// 2. BE redirect → KC login page (có Google + username/password)
// 3. KC callback → BE exchange → set cookies → redirect FE
//
// Local form đã comment lại, bật khi feature flag LOCAL mode
// ═══════════════════════════════════════════════════════════

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';

// ── LOCAL FORM IMPORTS (comment lại — bật khi fallback) ──
// import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
// import { Router } from '@angular/router';
// import { NzFormModule } from 'ng-zorro-antd/form';
// import { NzInputModule } from 'ng-zorro-antd/input';
// import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
// import { NzSpinModule } from 'ng-zorro-antd/spin';
// import { Auth } from '../../../core/services/auth';
// import { LoginResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    RouterLink,
    NzButtonModule,
    NzAlertModule,
    NzIconModule,
    // ── LOCAL FORM (comment lại) ──
    // ReactiveFormsModule,
    // NzFormModule,
    // NzInputModule,
    // NzCheckboxModule,
    // NzSpinModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private route = inject(ActivatedRoute);

  errorMessage = '';

  // ── LOCAL FORM STATE (comment lại) ──
  // private fb = inject(FormBuilder);
  // private router = inject(Router);
  // private auth = inject(Auth);
  // loginForm = this.fb.nonNullable.group({
  //   username: ['', [Validators.required, Validators.minLength(3)]],
  //   password: ['', [Validators.required]],
  //   remember: [true],
  // });
  // isSubmitting = false;

  ngOnInit(): void {
    // Handle SSO error redirect từ BE
    const error = this.route.snapshot.queryParamMap.get('error');
    if (error === 'sso_failed') {
      this.errorMessage = 'Đăng nhập thất bại. Vui lòng thử lại.';
    }
  }

  /**
   * Đăng nhập → KC login page (có Google + username/password)
   */
  loginWithKeycloak(): void {
    window.location.href = 'http://localhost:8081/api/auth/sso/login';
  }

  // ═══════════════════════════════════════════════════════
  // LOCAL LOGIN — COMMENT LẠI
  // Bật lại khi feature flag = LOCAL mode (KC chết)
  //
  // onSubmit() {
  //   if (this.loginForm.invalid) {
  //     this.loginForm.markAllAsTouched();
  //     return;
  //   }
  //   const { username, password } = this.loginForm.getRawValue();
  //   this.isSubmitting = true;
  //   this.errorMessage = '';
  //
  //   this.auth.login(username, password).subscribe({
  //     next: (response: LoginResponse) => {
  //       this.isSubmitting = false;
  //       if (response.twoFactorRequired) {
  //         sessionStorage.setItem('mfaSessionToken', response.mfaSessionToken!);
  //         sessionStorage.setItem('mfaUsername', username);
  //         this.router.navigate(['/auth/verify-2fa']);
  //         return;
  //       }
  //       this.auth.saveUser({
  //         username: response.username,
  //         email: response.email,
  //         roles: response.roles,
  //         timezone: response.timezone,
  //         phone: response.phone,
  //         firstName: response.firstName,
  //         lastName: response.lastName,
  //       });
  //       const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  //       if (returnUrl) { this.router.navigateByUrl(returnUrl); return; }
  //       const isStaff = response.roles?.some(r =>
  //         ['ADMIN_ALL', 'ADMIN', 'MANAGER', 'STAFF', 'HOST'].includes(r));
  //       this.router.navigateByUrl(isStaff ? '/admin/dashboard' : '/user/bookings');
  //     },
  //     error: (err) => {
  //       this.isSubmitting = false;
  //       this.errorMessage = err?.error?.message || 'Đăng nhập thất bại.';
  //     },
  //   });
  // }
  // ═══════════════════════════════════════════════════════
}
