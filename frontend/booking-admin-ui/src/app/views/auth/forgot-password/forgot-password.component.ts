import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAlertModule,
    NzIconModule,
  ],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(Auth);

  // === STATE ===
  isSubmitting = signal(false);
  isSent = signal(false);
  errorMessage = signal('');
  resendCountdown = signal(0);

  forgotForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    // Pre-fill email từ queryParam nếu user back navigation
    const emailParam = this.route.snapshot.queryParamMap.get('email');
    if (emailParam) {
      this.forgotForm.patchValue({ email: emailParam });
    }
  }

  /**
   * Submit form gửi email reset password
   */
  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const email = this.forgotForm.get('email')?.value ?? '';

    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.handleSuccess();
      },
      error: () => {
        // SECURITY: dù lỗi gì (404 email không tồn tại, 500, timeout...)
        // vẫn hiển thị success để tránh email enumeration
        this.isSubmitting.set(false);
        this.handleSuccess();
      },
    });
  }

  /**
   * Xử lý khi gửi thành công (hoặc lỗi cố tình nuốt)
   */
  private handleSuccess(): void {
    this.isSent.set(true);
    this.startResendCountdown(60);
  }

  /**
   * Bắt đầu countdown cho nút "Gửi lại"
   */
  private startResendCountdown(seconds: number): void {
    this.resendCountdown.set(seconds);
    const interval = setInterval(() => {
      this.resendCountdown.update((v) => v - 1);
      if (this.resendCountdown() <= 0) {
        clearInterval(interval);
      }
    }, 1000);
  }

  /**
   * Gửi lại email reset password (sau khi countdown = 0)
   */
  resendEmail(): void {
    if (this.resendCountdown() > 0) return;
    if (this.forgotForm.invalid) return;

    this.isSubmitting.set(true);
    const email = this.forgotForm.get('email')?.value ?? '';

    this.auth.forgotPassword(email).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.startResendCountdown(60);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.startResendCountdown(60);
      },
    });
  }

  /**
   * Quay lại trang đăng nhập
   */
  backToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  goToResetPassword(): void {
    const email = this.forgotForm.get('email')?.value ?? '';
    this.router.navigate(['/auth/reset-password'], {
      queryParams: { email },
    });
  }
}
