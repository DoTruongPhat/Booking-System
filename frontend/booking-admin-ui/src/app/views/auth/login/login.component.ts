import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { Auth } from '../../../core/services/auth';
import { LoginResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzCheckboxModule,
    NzAlertModule,
    NzIconModule,
    NzSpinModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(Auth);

  loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    remember: [true],
  });

  isSubmitting = false;
  errorMessage = '';

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { username, password } = this.loginForm.getRawValue();
    this.isSubmitting = true;
    this.errorMessage = '';

    this.auth.login(username, password).subscribe({
      next: (response: LoginResponse) => {
        this.isSubmitting = false;

        // ── Branch 1: 2FA required ──
        if (response.twoFactorRequired) {
          if (!response.mfaSessionToken) {
            this.errorMessage = '2FA session missing. Please try again.';
            return;
          }
          sessionStorage.setItem('mfaSessionToken', response.mfaSessionToken);
          sessionStorage.setItem('mfaUsername', username);
          this.router.navigate(['/auth/verify-2fa']);
          return;
        }

        // ── Branch 2: Direct login (no 2FA) ──
        if (!response.token) {
          this.errorMessage = 'Invalid response from server.';
          return;
        }

        this.auth.saveToken(response.token);
        this.auth.saveUser({
          username: response.username,
          email: response.email,
          roles: response.roles,
          timezone: response.timezone,
        });

        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/admin/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMessage =
          err?.error?.message || err?.message || 'Login failed. Please check your credentials.';
      },
    });
  }
}
