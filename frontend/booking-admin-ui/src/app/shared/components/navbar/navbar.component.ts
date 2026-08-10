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
  href?: string;
  queryParams?: Record<string, string>;
  action?: string;
  visible?: 'guest' | 'auth' | 'adminOrHost';
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
      this.auth.hydrateUserFromProfile().subscribe({
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
            this.auth.clearAll();
            this.user = null;
            // Không đăng nhập — OK, navbar hiện nút login
          },
      });

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
    this.auth.loginWithKeycloak();
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

  get visibleMoreSections(): NavDropdown['sections'] {
    return this.moreDropdown.sections
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => this.isDropdownItemVisible(item)),
      }))
      .filter((section) => section.items.length > 0);
  }

  trackBySection(_index: number, section: NavDropdown['sections'][number]): string {
    return section.title || String(_index);
  }

  trackByDropdownItem(_index: number, item: DropdownItem): string {
    return item.label;
  }

  get userInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() || 'U';
  }

  get isAdminOrHost(): boolean {
    const roles = this.user?.roles || [];
    return roles.some((r: string) => ['ADMIN_ALL', 'ADMIN', 'HOST'].includes(r));
  }

  private isDropdownItemVisible(item: DropdownItem): boolean {
    if (!item.visible) return true;
    if (item.visible === 'guest') return !this.user;
    if (item.visible === 'auth') return !!this.user;
    if (item.visible === 'adminOrHost') return this.isAdminOrHost;
    return true;
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
            link: '/hotels',
            queryParams: { city: 'Hà Nội' },
          },
          {
            icon: 'aim',
            label: 'TP. Hồ Chí Minh',
            description: 'Thành phố không ngủ',
            link: '/hotels',
            queryParams: { city: 'TP. Hồ Chí Minh' },
          },
          {
            icon: 'aim',
            label: 'Đà Nẵng',
            description: 'Thành phố đáng sống',
            link: '/hotels',
            queryParams: { city: 'Đà Nẵng' },
          },
          {
            icon: 'aim',
            label: 'Nha Trang',
            description: 'Biển xanh cát trắng',
            link: '/hotels',
            queryParams: { city: 'Nha Trang' },
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
            link: '/hotels',
            queryParams: { minRating: '5' },
          },
          {
            icon: 'home',
            label: 'Khách sạn 3-4 sao',
            description: 'Tiện nghi, giá tốt',
            link: '/hotels',
            queryParams: { minRating: '3' },
          },
          {
            icon: 'home',
            label: 'Resort & Villa',
            description: 'Nghỉ dưỡng riêng tư',
            link: '/hotels',
            queryParams: { q: 'resort' },
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
        items: [
          {
            icon: 'user',
            label: 'Hồ sơ của tôi',
            description: 'Quản lý thông tin cá nhân',
            action: 'profile',
            visible: 'auth',
          },
          {
            icon: 'calendar',
            label: 'Đơn đặt phòng của tôi',
            description: 'Theo dõi booking đã tạo',
            link: '/user/bookings',
            visible: 'auth',
          },
          {
            icon: 'credit-card',
            label: 'Thanh toán của tôi',
            description: 'Lịch sử giao dịch',
            link: '/user/payments',
            visible: 'auth',
          },
          {
            icon: 'dashboard',
            label: 'Dashboard',
            description: 'Quản lý vận hành khách sạn',
            link: '/admin/dashboard',
            visible: 'adminOrHost',
          },
          {
            icon: 'key',
            label: 'Quên mật khẩu',
            description: 'Khôi phục quyền truy cập',
            link: '/auth/forgot-password',
            visible: 'guest',
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
            link: '/user/tickets',
            visible: 'auth',
          },
          {
            icon: 'phone',
            label: 'Hotline hỗ trợ',
            description: '1900-1234',
            href: 'tel:19001234',
          },
          {
            icon: 'mail',
            label: 'Email hỗ trợ',
            description: 'support@smartbooking.vn',
            href: 'mailto:support@smartbooking.vn',
          },
        ],
      },
    ],
  };
}
