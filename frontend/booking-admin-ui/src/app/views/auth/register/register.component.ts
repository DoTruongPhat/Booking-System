import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { Auth } from '../../../core/services/auth';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzCheckboxModule,
    NzAlertModule,
    NzIconModule,
    NzProgressModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(Auth);

  // === STATE ===
  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  passwordVisible = signal(false);
  confirmPasswordVisible = signal(false);
  passwordStrength = signal(0);

  registerForm: FormGroup = this.fb.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      username: [
        '',
        [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_.@]+$/)],
      ],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s]{10,15}$/)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      agreeTerms: [false, [Validators.requiredTrue]],
    },
    { validators: this.passwordMatchValidator },
  );

  // === VALIDATORS ===

  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (password && confirmPassword && password !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  // === ACTIONS ===

  togglePasswordVisibility(): void {
    this.passwordVisible.update((v) => !v);
  }

  toggleConfirmPasswordVisibility(): void {
    this.confirmPasswordVisible.update((v) => !v);
  }

  onPasswordChange(): void {
    const password = this.registerForm.get('password')?.value || '';
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

  /**
   * Submit form đăng ký local
   * POST /api/auth/register → save local + sync KC Admin API
   */
  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.errorMessage.set('Vui lòng kiểm tra lại các trường thông tin.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const { fullName, username, email, phone, password } = this.registerForm.getRawValue();

    this.auth
      .register({
        fullName,
        username,
        email,
        phone,
        password,
        timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      })
      .subscribe({
        next: (res: any) => {
          this.isSubmitting.set(false);
          this.successMessage.set('Đăng ký thành công! Đang chuyển đến trang đăng nhập...');
          setTimeout(() => {
            this.router.navigate(['/auth/login'], {
              queryParams: { registered: 'true', username: res.username },
            });
          }, 1500);
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const details = err?.error?.details;
          if (details && typeof details === 'object') {
            this.errorMessage.set(Object.values(details).join('. '));
            return;
          }
          this.errorMessage.set(
            err?.error?.message ||
              extractErrorMessage(err, 'Đăng ký thất bại. Vui lòng thử lại sau.'),
          );
        },
      });
  }

  /**
   * Đăng ký bằng Google → BE redirect đến Google trực tiếp
   * Google user mới → Case C → BE tạo user (username = email) + sync KC
   */
  registerWithGoogle(): void {
    window.location.href = 'http://localhost:8081/api/auth/sso/login?provider=google';
  }

  /**
   * Đăng nhập SSO → KC form (có Google + username/password)
   */
  loginWithKeycloak(): void {
    window.location.href = '/api/auth/sso/login';
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
