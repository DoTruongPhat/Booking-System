// ═══════════════════════════════════════════════════════════
// HOTEL SEARCH COMPONENT — REFACTORED Phase E
// Bỏ mock data → gọi GET /api/rooms/search
// Đọc queryParams từ HomeComponent search bar
// Hỗ trợ pagination + server-side filtering
// ═══════════════════════════════════════════════════════════

import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSliderModule } from 'ng-zorro-antd/slider';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import {
  RoomService,
  RoomSearchResult,
  RoomSearchParams,
} from '../../../../app/core/services/rooms.service';

@Component({
  selector: 'app-hotel-search',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzIconModule,
    NzButtonModule,
    NzInputModule,
    NzSelectModule,
    NzSliderModule,
    NzSpinModule,
    NzPaginationModule,
    NavbarComponent,
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss',
})
export class HotelSearchComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private roomService = inject(RoomService);
  private message = inject(NzMessageService);

  private destroy$ = new Subject<void>();

  // ── Search state ──────────────────────────────────────
  searchQuery = '';
  selectedCity = '';
  priceRange: number[] = [0, 20000000];
  selectedStars: number[] = [];
  checkIn = '';
  checkOut = '';
  guests = 2;

  // ── Results ───────────────────────────────────────────
  rooms: RoomSearchResult[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 1;
  pageSize = 10;

  // ── Filter options ────────────────────────────────────
  cities = [
    { label: 'Tất cả', value: '' },
    { label: 'Hà Nội', value: 'Hà Nội' },
    { label: 'TP. Hồ Chí Minh', value: 'TP. Hồ Chí Minh' },
    { label: 'Đà Nẵng', value: 'Đà Nẵng' },
    { label: 'Nha Trang', value: 'Nha Trang' },
    { label: 'Hội An', value: 'Hội An' },
    { label: 'Phú Quốc', value: 'Phú Quốc' },
    { label: 'Sa Pa', value: 'Sa Pa' },
  ];

  amenityLabels: Record<string, string> = {
    wifi: 'WiFi',
    pool: 'Hồ bơi',
    breakfast: 'Bữa sáng',
    parking: 'Đỗ xe',
    gym: 'Gym',
    spa: 'Spa',
    'air-conditioning': 'Điều hòa',
    tv: 'TV',
    minibar: 'Minibar',
    safe: 'Két sắt',
    bathtub: 'Bồn tắm',
    balcony: 'Ban công',
    'sea-view': 'View biển',
    'city-view': 'View TP',
  };

  ngOnInit(): void {
    // Đọc query params từ URL (HomeComponent truyền sang)
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      if (params['city']) this.selectedCity = params['city'];
      if (params['q']) this.searchQuery = params['q'];
      if (params['checkIn']) this.checkIn = params['checkIn'];
      if (params['checkOut']) this.checkOut = params['checkOut'];
      if (params['guests']) this.guests = +params['guests'];
      if (params['page']) this.currentPage = +params['page'];
      this.search();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ══════════════════════════════════════════════════════
  // SEARCH (gọi API thật)
  // ══════════════════════════════════════════════════════

  search(): void {
    this.loading = true;

    const params: RoomSearchParams = {
      page: this.currentPage - 1, // backend 0-based
      size: this.pageSize,
    };

    // City: từ sidebar filter hoặc search query
    const city = this.selectedCity || this.searchQuery;
    if (city?.trim()) params.city = city.trim();
    if (this.checkIn) params.checkIn = this.checkIn;
    if (this.checkOut) params.checkOut = this.checkOut;
    if (this.guests > 0) params.guests = this.guests;

    // Price filter
    if (this.priceRange[0] > 0) params.minPrice = this.priceRange[0];
    if (this.priceRange[1] < 20000000) params.maxPrice = this.priceRange[1];

    // Star rating → minRating (lấy min trong selectedStars)
    if (this.selectedStars.length > 0) {
      params.minRating = Math.min(...this.selectedStars);
    }

    this.roomService.search(params).subscribe({
      next: (data) => {
        this.rooms = data.content;
        this.totalElements = data.totalElements;
        this.currentPage = data.number + 1;
        this.loading = false;
      },
      error: (err) => {
        console.error('Search error:', err);
        this.message.error('Không thể tìm kiếm. Vui lòng thử lại.');
        this.rooms = [];
        this.totalElements = 0;
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.currentPage = 1;
    this.updateUrlParams();
    this.search();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.updateUrlParams();
    this.search();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ══════════════════════════════════════════════════════
  // FILTERS
  // ══════════════════════════════════════════════════════

  selectCity(city: string): void {
    this.selectedCity = city;
    this.onSearch();
  }

  toggleStar(rating: number): void {
    const idx = this.selectedStars.indexOf(rating);
    if (idx >= 0) {
      this.selectedStars.splice(idx, 1);
    } else {
      this.selectedStars.push(rating);
    }
    this.onSearch();
  }

  resetFilters(): void {
    this.searchQuery = '';
    this.selectedCity = '';
    this.priceRange = [0, 20000000];
    this.selectedStars = [];
    this.checkIn = '';
    this.checkOut = '';
    this.guests = 2;
    this.onSearch();
  }

  // ══════════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════════

  viewRoom(roomId: string): void {
    this.router.navigate(['/hotels', roomId]);
  }

  // ══════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price) + 'đ';
  }

  getAmenityLabel(key: string): string {
    return this.amenityLabels[key] || key;
  }

  getRatingLabel(rating: number): string {
    if (rating >= 9) return 'Xuất sắc';
    if (rating >= 8) return 'Rất tốt';
    if (rating >= 7) return 'Tốt';
    return '';
  }

  private updateUrlParams(): void {
    const qp: Record<string, string> = {};
    if (this.selectedCity) qp['city'] = this.selectedCity;
    if (this.searchQuery) qp['q'] = this.searchQuery;
    if (this.checkIn) qp['checkIn'] = this.checkIn;
    if (this.checkOut) qp['checkOut'] = this.checkOut;
    if (this.guests && this.guests !== 2) qp['guests'] = String(this.guests);
    if (this.currentPage > 1) qp['page'] = String(this.currentPage);

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: qp,
      queryParamsHandling: 'replace',
      replaceUrl: true,
    });
  }
}
