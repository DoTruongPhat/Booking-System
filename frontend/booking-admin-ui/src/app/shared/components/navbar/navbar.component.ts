import { Component, signal, HostListener, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { Auth } from '../../../core/services/auth';
import { LogoComponent } from '../logo/logo.component';

interface DropdownItem {
  icon: string;
  label: string;
  description?: string;
  link?: string;
  action?: string;
}

interface NavDropdown {
  label: string;
  sections: {
    title?: string;
    items: DropdownItem[];
  }[];
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    NzIconModule,
    NzDropDownModule,
    NzMenuModule,
    NzAvatarModule,
    LogoComponent,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  isScrolled = signal(false);
  openDropdown = signal<string | null>(null);
  user: any = null;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.user = this.auth.getUser();

      // BFF: localStorage có thể trống → fetch từ API
      if (!this.user) {
        this.auth.getProfile().subscribe({
          next: (profile: any) => {
            this.auth.saveUser({
              username: profile.username,
              email: profile.email,
              roles: profile.roles?.map((r: any) => (typeof r === 'string' ? r : r.code)) || [],
              timezone: profile.timezone,
              phone: profile.phone,
              firstName: profile.firstName,
              lastName: profile.lastName,
            });
            this.user = this.auth.getUser();
          },
          error: () => {
            // Không đăng nhập — OK, navbar hiện nút login
          },
        });
      }

      this.checkScroll();
    }
  }

  @HostListener('window:scroll')
  onScroll(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.checkScroll();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const target = event.target as HTMLElement;
    if (!target.closest('.nav-item-dropdown')) {
      this.openDropdown.set(null);
    }
  }

  toggleDropdown(label: string, event: MouseEvent): void {
    event.stopPropagation();
    this.openDropdown.update((current) => (current === label ? null : label));
  }

  closeDropdown(): void {
    this.openDropdown.set(null);
  }

  goToLogin(): void {
    window.location.href = '/api/auth/sso/login';
  }

  goToProfile(): void {
    if (this.isAdminOrHost) {
      this.router.navigate(['/admin/profile']);
    } else {
      this.router.navigate(['/user/profile']);
    }
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearAll();
        this.user = null;
        this.router.navigate(['/']);
      },
      error: () => {
        this.auth.clearAll();
        this.user = null;
        this.router.navigate(['/']);
      },
    });
  }

  handleItemAction(action: string | undefined): void {
    if (action === 'login') {
      this.goToLogin();
    } else if (action === 'profile') {
      this.goToProfile();
    }
  }

  get userInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() || 'U';
  }

  get isAdminOrHost(): boolean {
    const roles = this.user?.roles || [];
    return roles.some((r: string) => ['ADMIN_ALL', 'ADMIN', 'HOST'].includes(r));
  }

  private checkScroll(): void {
    if (typeof window === 'undefined') return;
    this.isScrolled.set(window.scrollY > 8);
  }

  readonly hotelsDropdown: NavDropdown = {
    label: 'Khách sạn',
    sections: [
      {
        title: 'Điểm đến nổi bật',
        items: [
          {
            icon: 'aim',
            label: 'Hà Nội',
            description: 'Thủ đô nghìn năm văn hiến',
            link: '/hotels/hanoi',
          },
          {
            icon: 'aim',
            label: 'TP. Hồ Chí Minh',
            description: 'Thành phố không ngủ',
            link: '/hotels/hcmc',
          },
          {
            icon: 'aim',
            label: 'Đà Nẵng',
            description: 'Thành phố đáng sống',
            link: '/hotels/danang',
          },
          {
            icon: 'aim',
            label: 'Nha Trang',
            description: 'Biển xanh cát trắng',
            link: '/hotels/nhatrang',
          },
        ],
      },
      {
        title: 'Loại hình lưu trú',
        items: [
          {
            icon: 'home',
            label: 'Khách sạn 5 sao',
            description: 'Sang trọng, đẳng cấp',
            link: '/hotels/5-star',
          },
          {
            icon: 'home',
            label: 'Khách sạn 3-4 sao',
            description: 'Tiện nghi, giá tốt',
            link: '/hotels/3-4-star',
          },
          {
            icon: 'home',
            label: 'Resort & Villa',
            description: 'Nghỉ dưỡng riêng tư',
            link: '/hotels/resort',
          },
        ],
      },
    ],
  };

  readonly moreDropdown: NavDropdown = {
    label: 'Xem thêm',
    sections: [
      {
        title: 'Tài khoản',
        items: [{ icon: 'key', label: 'Quên mật khẩu', link: '/auth/forgot-password' }],
      },
      {
        title: 'Giới thiệu & Liên hệ',
        items: [
          {
            icon: 'info-circle',
            label: 'Về SmartBooking',
            description: 'Câu chuyện của chúng tôi',
            link: '/about',
          },
          {
            icon: 'phone',
            label: 'Thông tin liên lạc',
            description: 'Hotline, email, chat',
            link: '/contact',
          },
          {
            icon: 'customer-service',
            label: 'Hỗ trợ 24/7',
            description: 'Đội ngũ tư vấn luôn sẵn sàng',
            link: '/support',
          },
        ],
      },
      {
        title: 'Hỗ trợ',
        items: [
          {
            icon: 'file-text',
            label: 'Tạo phiếu Ticket',
            description: 'Gửi yêu cầu hỗ trợ',
            link: '/support/ticket',
          },
          {
            icon: 'star',
            label: 'Đánh giá dịch vụ',
            description: 'Chia sẻ trải nghiệm của bạn',
            link: '/review',
          },
          {
            icon: 'question-circle',
            label: 'Câu hỏi thường gặp',
            description: 'FAQ - Giải đáp nhanh',
            link: '/faq',
          },
        ],
      },
    ],
  };
}
