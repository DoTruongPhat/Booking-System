import {
  Component,
  signal,
  HostListener,
  inject,
  OnInit,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
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
  imports: [CommonModule, RouterLink, RouterLinkActive, NzIconModule, LogoComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID);

  // Track scroll for sticky shadow
  isScrolled = signal(false);
  // Track open dropdown
  openDropdown = signal<string | null>(null);
  // Current user (snapshot)
  user: any = null;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.user = this.auth.getUser();
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

  private checkScroll(): void {
    if (typeof window === 'undefined') return;
    this.isScrolled.set(window.scrollY > 8);
  }

  toggleDropdown(label: string, event: MouseEvent): void {
    event.stopPropagation();
    this.openDropdown.update((current) => (current === label ? null : label));
  }

  closeDropdown(): void {
    this.openDropdown.set(null);
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
    this.closeDropdown();
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

  // === DROPDOWN DATA ===

  readonly hotelsDropdown: NavDropdown = {
    label: 'Khách sạn',
    sections: [
      {
        title: 'Điểm đến nổi bật',
        items: [
          { icon: 'environment', label: 'Hà Nội', description: 'Thủ đô nghìn năm văn hiến', link: '/hotels/hanoi' },
          { icon: 'environment', label: 'TP. Hồ Chí Minh', description: 'Thành phố không ngủ', link: '/hotels/hcmc' },
          { icon: 'environment', label: 'Đà Nẵng', description: 'Thành phố đáng sống', link: '/hotels/danang' },
          { icon: 'environment', label: 'Nha Trang', description: 'Biển xanh cát trắng', link: '/hotels/nhatrang' },
        ],
      },
      {
        title: 'Loại hình lưu trú',
        items: [
          { icon: 'home', label: 'Khách sạn 5 sao', description: 'Sang trọng, đẳng cấp', link: '/hotels/5-star' },
          { icon: 'home', label: 'Khách sạn 3-4 sao', description: 'Tiện nghi, giá tốt', link: '/hotels/3-4-star' },
          { icon: 'home', label: 'Resort & Villa', description: 'Nghỉ dưỡng riêng tư', link: '/hotels/resort' },
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
          { icon: 'user', label: 'Đăng nhập', action: 'login' },
          { icon: 'user-add', label: 'Đăng ký tài khoản', link: '/auth/register' },
          { icon: 'safety', label: 'Quên mật khẩu', link: '/auth/forgot-password' },
        ],
      },
      {
        title: 'Giới thiệu & Liên hệ',
        items: [
          { icon: 'info-circle', label: 'Về SmartBooking', description: 'Câu chuyện của chúng tôi', link: '/about' },
          { icon: 'phone', label: 'Thông tin liên lạc', description: 'Hotline, email, chat', link: '/contact' },
          { icon: 'customer-service', label: 'Hỗ trợ 24/7', description: 'Đội ngũ tư vấn luôn sẵn sàng', link: '/support' },
        ],
      },
      {
        title: 'Hỗ trợ',
        items: [
          { icon: 'file-text', label: 'Tạo phiếu Ticket', description: 'Gửi yêu cầu hỗ trợ', link: '/support/ticket' },
          { icon: 'star', label: 'Đánh giá dịch vụ', description: '