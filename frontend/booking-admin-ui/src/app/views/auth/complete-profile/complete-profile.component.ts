import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router } from '@angular/router';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-complete-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzAlertModule,
    NzIconModule,
    NzProgressModule,
  ],
  templateUrl: './complete-profile.component.html',
  styleUrl: './complete-profile.component.scss',
})
export class CompleteProfileComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private auth = inject(Auth);

  user: any = null;
  isLoading = true;

  ngOnInit(): void {
    this.auth.getProfile().subscribe({
      next: (profile: any) => {
        this.user = profile;
        this.isLoading = false;
        // Không pre-fill nếu username là email (chứa @)
        if (profile.username && !profile.username.includes('@')) {
          this.form.patchValue({ username: profile.username });
        }
      },
      error: () => {
        this.router.navigateByUrl('/auth/login');
      },
    });
  }
  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  passwordStrength = signal(0);

  form = this.fb.nonNullable.group(
    {
      username: [
        this.user?.username || '',
        [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_.@]+$/)],
      ],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: this.matchValidator },
  );

  private matchValidator(group: AbstractControl): ValidationErrors | null {
    const p = group.get('newPassword')?.value;
    const c = group.get('confirmPassword')?.value;
    return p && c && p !== c ? { passwordMismatch: true } : null;
  }

  onPasswordChange() {
    const pw = this.form.get('newPassword')?.value || '';
    let s = 0;
    if (pw.length >= 8) s += 25;
    if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) s += 25;
    if (/[0-9]/.test(pw)) s += 25;
    if (/[^a-zA-Z0-9]/.test(pw)) s += 25;
    this.passwordStrength.set(s);
  }

  get strengthColor(): string {
    const s = this.passwordStrength();
    return s < 50 ? '#E53935' : s < 75 ? '#FFA500' : '#6FBF73';
  }

  get strengthLabel(): string {
    const s = this.passwordStrength();
    return s < 50 ? 'Yếu' : s < 75 ? 'Trung bình' : 'Mạnh';
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const { username, newPassword } = this.form.getRawValue();
    const usernameChanged = username !== this.user?.username ? username : null;

    this.auth.completeProfile(usernameChanged, newPassword).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.successMessage.set('Hoàn tất! Đang chuyển hướng...');
        // Update local user info
        if (usernameChanged && this.user) {
          this.user.username = usernameChanged;
          this.auth.saveUser(this.user);
        }
        setTimeout(() => this.router.navigateByUrl(this.auth.getLandingPath()), 1500);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const details = err?.error?.details;
        if (details && typeof details === 'object') {
          this.errorMessage.set(Object.values(details).join('. '));
        } else {
          this.errorMessage.set(err?.error?.message || 'Thất bại. Vui lòng thử lại.');
        }
      },
    });
  }

  skip() {
    this.router.navigateByUrl(this.auth.getLandingPath());
  }
}
