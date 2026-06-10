// ═══════════════════════════════════════════════════════════
// VERIFY 2FA COMPONENT
// Nhập 6 số OTP từ Google Authenticator sau khi login bước 1
// ═══════════════════════════════════════════════════════════

import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NzMessageService } from 'ng-zorro-antd/message';
import { Auth } from '../../../core/services/auth';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { LoginResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-verify-2fa',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './verify-2fa.component.html',
  styleUrl: './verify-2fa.component.scss',
})
export class Verify2faComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);
  private auth = inject(Auth);
  private message = inject(NzMessageService);

  otpForm = this.fb.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  isSubmitting = false;
  username: string | null = null;

  constructor() {
    // Lấy username từ sessionStorage (lưu lúc login bước 1)
    this.username = sessionStorage.getItem('mfaUsername');

    // Nếu không có mfaSessionToken → đá về login
    if (!sessionStorage.getItem('mfaSessionToken')) {
      this.message.warning('Session expired. Please login again.');
      this.router.navigate(['/auth/login']);
    }
  }

  onSubmit() {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    const sessionToken = sessionStorage.getItem('mfaSessionToken');
    if (!sessionToken) {
      this.message.error('MFA session missing.');
      this.router.navigate(['/auth/login']);
      return;
    }

    const otp = this.otpForm.value.otp!;
    this.isSubmitting = true;

    // Gọi backend verify OTP
    this.http
      .post<LoginResponse>('/api/auth/2fa/verify', {
        sessionToken,
        otp,
      })
      .subscribe({
        next: (response) => {
          this.isSubmitting = false;

          if (!response.token) {
            this.message.error('Invalid OTP response.');
            return;
          }

          // Lưu token + thông tin user
          this.auth.saveToken(response.token);
          this.auth.saveUser({
            username: response.username,
            email: response.email,
            roles: response.roles,
            timezone: response.timezone,
          });

          // Dọn session storage
          sessionStorage.removeItem('mfaSessionToken');
          sessionStorage.removeItem('mfaUsername');

          const returnUrl =
            this.route.snapshot.queryParamMap.get('returnUrl') || '/admin/dashboard';
          this.router.navigateByUrl(returnUrl);
        },
        error: (err) => {
          this.isSubmitting = false;
          this.message.error(extractErrorMessage(err, 'Invalid OTP code.'));
        },
      });
  }

  cancel() {
    sessionStorage.removeItem('mfaSessionToken');
    sessionStorage.removeItem('mfaUsername');
    this.router.navigate(['/auth/login']);
  }
}
