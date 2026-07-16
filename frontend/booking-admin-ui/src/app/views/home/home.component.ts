import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFloatButtonModule } from 'ng-zorro-antd/float-button';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { Auth } from '../../core/services/auth';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { Hotel } from '../../core/models/hotel.model';

interface Destination {
  name: string;
  image: string;
  count: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzIconModule,
    NzButtonModule,
    NzFloatButtonModule,
    NzTooltipModule,
    NavbarComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  user: any;

  // Search form state
  searchCity = '';
  searchCheckIn = '';
  searchCheckOut = '';
  searchGuests = 2;

  // === DEMO DATA (sau này gọi API) ===
  destinations: Destination[] = [
    {
      name: 'Hà Nội',
      image: '/images/Hanoi.jpg',
      count: 486,
    },
    {
      name: 'TP. Hồ Chí Minh',
      image: '/images/HoChiMinh.jpg',
      count: 521,
    },
    {
      name: 'Đà Nẵng',
      image: '/images/DaNang.jpg',
      count: 312,
    },
    {
      name: 'Nha Trang',
      image: '/images/NhaTrang.jpg',
      count: 248,
    },
  ];

  featuredHotels: Partial<Hotel>[] = [
    {
      id: 'h1',
      name: 'Vinpearl Resort Nha Trang',
      city: 'Nha Trang',
      starRating: 5,
      userRating: 9.2,
      reviewCount: 1248,
      thumbnail: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800',
      pricePerNight: 2400000,
      discountPercent: 20,
    },
    {
      id: 'h2',
      name: 'InterContinental Đà Nẵng',
      city: 'Đà Nẵng',
      starRating: 5,
      userRating: 9.5,
      reviewCount: 982,
      thumbnail: 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800',
      pricePerNight: 3200000,
    },
    {
      id: 'h3',
      name: 'Hanoi Old Quarter Hotel',
      city: 'Hà Nội',
      starRating: 4,
      userRating: 8.9,
      reviewCount: 654,
      thumbnail: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800',
      pricePerNight: 1200000,
      discountPercent: 15,
    },
    {
      id: 'h4',
      name: 'Saigon Riverside Boutique',
      city: 'TP. Hồ Chí Minh',
      starRating: 4,
      userRating: 9.0,
      reviewCount: 421,
      thumbnail: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800',
      pricePerNight: 1800000,
    },
  ];

  constructor(
    private auth: Auth,
    private router: Router,
  ) {
    this.user = this.auth.getUser();
  }

  // === ACTIONS ===
  goToDashboard() {
    if (this.user) {
      this.router.navigateByUrl(this.auth.getLandingPath());
    } else {
      this.router.navigate(['/auth/login']);
    }
  }

  goToLogin() {
    this.router.navigate(['/auth/login']);
  }

  logout() {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearAll();
        this.user = null;
        this.router.navigate(['/']);
      },
      error: () => {
        this.auth.clearAll();
        this.user = null;
      },
    });
  }

  onSearch() {
    this.router.navigate(['/hotels']);
  }

  viewHotel(id: string | undefined) {
    if (id) {
      this.router.navigate(['/hotels', id]);
    }
  }

  getStars(rating: number | undefined): number[] {
    if (!rating) return [];
    return Array(Math.floor(rating)).fill(0);
  }

  get userInitials(): string {
    return this.user?.username?.charAt(0).toUpperCase() || '?';
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL') || roles.includes('ADMIN')) return 'Quản trị viên';
    if (roles.includes('HOST')) return 'Chủ khách sạn';
    return 'Khách hàng';
  }

  onSearchSubmit(): void {
    const queryParams: Record<string, string> = {};

    if (this.searchCity) queryParams['city'] = this.searchCity;
    if (this.searchCheckIn) queryParams['checkIn'] = this.searchCheckIn;
    if (this.searchCheckOut) queryParams['checkOut'] = this.searchCheckOut;
    if (this.searchGuests) queryParams['guests'] = String(this.searchGuests);

    this.router.navigate(['/hotels'], { queryParams });
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
