import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSliderModule } from 'ng-zorro-antd/slider';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { Subject, takeUntil } from 'rxjs';
import { RoomSearchParams, RoomSearchResult, RoomService } from '../../../core/services/rooms.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

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

  searchQuery = '';
  selectedCity = '';
  priceRange: number[] = [0, 20000000];
  selectedStars: number[] = [];
  checkIn = '';
  checkOut = '';
  guests = 2;

  rooms: RoomSearchResult[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 1;
  pageSize = 10;

  readonly cities = [
    { label: 'Tất cả', value: '' },
    { label: 'Hà Nội', value: 'Hà Nội' },
    { label: 'TP. Hồ Chí Minh', value: 'TP. Hồ Chí Minh' },
    { label: 'Đà Nẵng', value: 'Đà Nẵng' },
    { label: 'Nha Trang', value: 'Nha Trang' },
    { label: 'Hội An', value: 'Hội An' },
    { label: 'Phú Quốc', value: 'Phú Quốc' },
    { label: 'Sa Pa', value: 'Sa Pa' },
  ];

  readonly amenityLabels: Record<string, string> = {
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
    'city-view': 'View thành phố',
  };

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.selectedCity = params['city'] || '';
      this.searchQuery = params['q'] || '';
      this.checkIn = params['checkIn'] || '';
      this.checkOut = params['checkOut'] || '';
      this.guests = params['guests'] ? Number(params['guests']) : 2;
      this.currentPage = params['page'] ? Number(params['page']) : 1;
      this.selectedStars = params['minRating'] ? [Number(params['minRating'])] : [];
      this.search();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  search(): void {
    this.loading = true;

    const params: RoomSearchParams = {
      page: this.currentPage - 1,
      size: this.pageSize,
    };

    const city = this.selectedCity || this.searchQuery;
    if (city?.trim()) params.city = city.trim();
    if (this.checkIn) params.checkIn = this.checkIn;
    if (this.checkOut) params.checkOut = this.checkOut;
    if (this.guests > 0) params.guests = this.guests;
    if (this.priceRange[0] > 0) params.minPrice = this.priceRange[0];
    if (this.priceRange[1] < 20000000) params.maxPrice = this.priceRange[1];
    if (this.selectedStars.length > 0) params.minRating = Math.min(...this.selectedStars);

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
    this.updateUrlParams().then((changed) => {
      if (!changed) this.search();
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.updateUrlParams().then((changed) => {
      if (!changed) this.search();
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  selectCity(city: string): void {
    this.selectedCity = city;
    this.onSearch();
  }

  toggleStar(rating: number): void {
    const idx = this.selectedStars.indexOf(rating);
    if (idx >= 0) {
      this.selectedStars.splice(idx, 1);
    } else {
      this.selectedStars = [rating];
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

  viewRoom(roomId: string): void {
    this.router.navigate(['/hotels', roomId], {
      queryParams: {
        checkIn: this.checkIn || undefined,
        checkOut: this.checkOut || undefined,
        guests: this.guests || undefined,
      },
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(Number.isFinite(price) ? price : 0) + 'đ';
  }

  getAmenityLabel(key: string): string {
    return this.amenityLabels[key] || key;
  }

  getRatingLabel(rating: number): string {
    if (rating >= 9) return 'Xuất sắc';
    if (rating >= 8) return 'Rất tốt';
    if (rating >= 7) return 'Tốt';
    return 'Mới';
  }

  private updateUrlParams(): Promise<boolean> {
    const qp: Record<string, string> = {};
    if (this.selectedCity) qp['city'] = this.selectedCity;
    if (this.searchQuery) qp['q'] = this.searchQuery;
    if (this.checkIn) qp['checkIn'] = this.checkIn;
    if (this.checkOut) qp['checkOut'] = this.checkOut;
    if (this.guests && this.guests !== 2) qp['guests'] = String(this.guests);
    if (this.currentPage > 1) qp['page'] = String(this.currentPage);
    if (this.selectedStars.length > 0) qp['minRating'] = String(Math.min(...this.selectedStars));

    return this.router.navigate([], {
      relativeTo: this.route,
      queryParams: qp,
      queryParamsHandling: 'replace',
      replaceUrl: true,
    });
  }
}
