import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { Auth } from '../../../core/services/auth';
import { UserService } from '../../../core/services/user';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    NzIconModule,
    NzButtonModule,
    NzAlertModule,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(Auth);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);

  user: any = null;
  isEditing = false;
  isSaving = false;
  is2FAEnabled = false;
  isToggling2FA = false;
  isOnboarding = signal(false); // Phase 7: true khi user cần bổ sung phone

  // Phone regex (đồng bộ với pattern register)
  phonePattern = /^[0-9+\-\s]{10,15}$/;

  profileForm = this.fb.nonNullable.group({
    username: [{ value: '', disabled: true }],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern(this.phonePattern)]],
    timezone: ['Asia/Ho_Chi_Minh'],
    currentPassword: [''],
    newPassword: [''],
    confirmPassword: [''],
    firstName: [''],
    lastName: [''],
  });

  ngOnInit(): void {
    this.user = this.auth.getUser();

    // Phase 7: check queryParam onboarding
    this.route.queryParamMap.subscribe((params) => {
      const onboarding = params.get('onboarding') === 'true';
      this.isOnboarding.set(onboarding);
      // Nếu onboarding → mở form edit luôn
      if (onboarding) this.isEditing = true;
    });

    // Gọi API lấy thông tin mới nhất
    this.userService.getMyProfile().subscribe({
      next: (profile) => {
        const currentRoles = this.auth.getUser()?.roles || [];
        this.user = { ...profile, roles: currentRoles };
        this.user = profile;
        this.is2FAEnabled = profile.twoFactorEnabled || false;
        this.profileForm.patchValue({
          username: profile.username || '',
          email: profile.email || '',
          firstName: profile.firstName || '',
          lastName: profile.lastName || '',
          phone: profile.phone || '',
          timezone: profile.timezone || 'Asia/Ho_Chi_Minh',
        });
      },
      error: (err) => {
        console.error('[Profile] Load failed:', err);
      },
    });
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    if (!this.isEditing) {
      this.profileForm.patchValue({
        email: this.user?.email || '',
        phone: this.user?.phone || '',
        firstName: this.user?.firstName || '',
        lastName: this.user?.lastName || '',
      });
    }
  }

  /**
   * Phase 7: Nếu user mới sync từ KC, phone thường rỗng.
   * Sau khi điền phone hợp lệ → disable onboarding flag.
   */
  hasValidPhone(): boolean {
    const phone = this.profileForm.get('phone')?.value as string | undefined;
    return !!phone && this.phonePattern.test(phone);
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }
    this.isSaving = true;

    const { email, firstName, lastName, phone, timezone } = this.profileForm.getRawValue();

    // Phase 7: gọi BE PUT /api/users/me để persist vào DB
    this.userService.updateMyProfile({ email, firstName, lastName, phone, timezone }).subscribe({
      next: (updatedUser) => {
        // Update local user info từ response BE (đảm bảo đồng bộ)
        this.auth.saveUser({
          ...this.user,
          username: updatedUser.username || this.user?.username,
          email: updatedUser.email || email,
          firstName: updatedUser.firstName || firstName,
          lastName: updatedUser.lastName || lastName,
          phone: updatedUser.phone || phone,
          timezone: updatedUser.timezone || timezone,
          roles: this.auth.getUser()?.roles || [],
        });

        this.isSaving = false;
        this.isEditing = false;
        this.isOnboarding.set(false);

        // Reload user object cho UI
        this.user = this.auth.getUser();
        if (this.user) {
          this.profileForm.patchValue({
            username: this.user.username,
            email: this.user.email,
            firstName: this.user.firstName || '',
            lastName: this.user.lastName || '',
            phone: this.user.phone,
            timezone: this.user.timezone,
          });
        }
      },
      error: (err) => {
        this.isSaving = false;
        console.error('[Profile] Save failed:', err);
        alert(extractErrorMessage(err, 'Không thể lưu thông tin. Vui lòng thử lại.'));
      },
    });
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL')) return 'Quản trị viên';
    if (roles.includes('ADMIN')) return 'Quản lý';
    if (roles.includes('HOST')) return 'Nhân viên';
    return 'Khách hàng';
  }

  get userInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() || '?';
  }

  toggle2FA(): void {
    this.isToggling2FA = true;
    const newState = !this.is2FAEnabled;

    this.userService.toggle2FA(newState).subscribe({
      next: () => {
        this.is2FAEnabled = newState;
        this.isToggling2FA = false;
      },
      error: (err) => {
        this.isToggling2FA = false;
        console.error('[Profile] 2FA toggle failed:', err);
        alert(extractErrorMessage(err, 'Không thể thay đổi 2FA. Vui lòng thử lại.'));
      },
    });
  }
}
