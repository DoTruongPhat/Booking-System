import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-reset-password',
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
    NzProgressModule,
  ],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(Auth);

  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  passwordVisible = signal(false);
  confirmPasswordVisible = signal(false);
  passwordStrength = signal(0);
  redirectCountdown = signal(3);
  email = '';

  resetForm: FormGroup = this.fb.nonNullable.group(
    {
      otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.passwordMatchValidator },
  );

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') || '';
    if (!this.email) {
      this.router.navigate(['/auth/forgot-password']);
    }
  }

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  togglePasswordVisibility(): void {
    this.passwordVisible.update((v) => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.confirmPasswordVisible.update((v) => !v);
  }

  onPasswordChange(): void {
    const password = this.resetForm.get('newPassword')?.value || '';
    let strength = 0;
    if (password.length >= 8) strength += 25;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength += 25;
    if (/[0-9]/.test(password)) strength += 25;
    if (/[^a-zA-Z0-9]/.test(password)) strength += 25;
    this.passwordStrength.set(strength);
  }

  get passwordStrengthColor(): string {
    const s = this.passwordStrength();
    if (s < 50) return '#E53935';
    if (s < 75) return '#FFA500';
    return '#6FBF73';
  }

  get passwordStrengthLabel(): string {
    const s = this.passwordStrength();
    if (s < 50) return 'Yếu';
    if (s < 75) return 'Trung bình';
    return 'Mạnh';
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login'], { queryParams: { reset: 'success' } });
  }

  goToForgot(): void {
    this.router.navigate(['/auth/forgot-password']);
  }

  onSubmit(): void {
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      this.errorMessage.set('Vui lòng kiểm tra lại thông tin.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { otp, newPassword } = this.resetForm.getRawValue();

    this.auth.resetPassword(this.email, otp, newPassword).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Đặt lại mật khẩu thành công!');
        this.startRedirectCountdown();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const msg = err?.error?.message || err?.error?.details?.otp || 'Đặt lại mật khẩu thất bại.';
        this.errorMessage.set(msg);
      },
    });
  }

  private startRedirectCountdown(): void {
    const interval = setInterval(() => {
      this.redirectCountdown.update((v) => v - 1);
      if (this.redirectCountdown() <= 0) {
        clearInterval(interval);
        this.goToLogin();
      }
    }, 1000);
  }

  redirectNow(): void {
    this.goToLogin();
  }
}
