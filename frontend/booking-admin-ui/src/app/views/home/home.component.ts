import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFloatButtonModule } from 'ng-zorro-antd/float-button';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { Auth } from '../../core/services/auth';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { Hotel } from '../../core/models/hotel.model';
import { RoomSearchResult, RoomService } from '../../core/services/rooms.service';

interface Destination {
  name: string;
  image: string;
  count: number;
}

interface FeaturedHotel extends Partial<Hotel> {
  roomId: string;
  hotelId: string;
  roomName: string;
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
export class HomeComponent implements AfterViewInit, OnInit, OnDestroy {
  @ViewChild('heroVideo') private heroVideo?: ElementRef<HTMLVideoElement>;

  private destroy$ = new Subject<void>();

  user: any;
  heroVideoReady = false;
  featuredHotelsLoading = false;

  searchCity = '';
  searchCheckIn = '';
  searchCheckOut = '';
  searchGuests = 2;
  searchRooms = 1;
  searchAdults = 2;
  searchChildren = 0;
  guestPickerOpen = false;

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

  featuredHotels: FeaturedHotel[] = [];

  constructor(
    private auth: Auth,
    private router: Router,
    private roomService: RoomService,
  ) {
    this.user = this.auth.getUser();
  }

  ngOnInit(): void {
    this.loadFeaturedHotels();
  }

  ngAfterViewInit(): void {
    queueMicrotask(() => {
      const video = this.heroVideo?.nativeElement;
      if (!video) return;

      video.muted = true;
      video.playsInline = true;
      video.load();
      video.play().catch(() => {
        // Browser autoplay policies can defer playback until the tab is active.
      });
    });
  }

  onHeroVideoReady(): void {
    this.heroVideoReady = true;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

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

  viewHotel(hotel: FeaturedHotel | undefined) {
    if (hotel?.roomId) {
      this.router.navigate(['/hotels', hotel.roomId]);
    }
  }

  getStars(rating: number | undefined): number[] {
    if (!rating) return [];
    return Array(Math.floor(rating)).fill(0);
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByDestination(_index: number, destination: Destination): string {
    return destination.name;
  }

  trackByHotel(_index: number, hotel: FeaturedHotel): string {
    return hotel.hotelId || hotel.roomId || hotel.name || String(_index);
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
    if (this.totalGuests) queryParams['guests'] = String(this.totalGuests);
    if (this.searchRooms > 1) queryParams['rooms'] = String(this.searchRooms);

    this.router.navigate(['/hotels'], { queryParams });
  }

  @HostListener('document:click')
  closeGuestPicker(): void {
    this.guestPickerOpen = false;
  }

  toggleGuestPicker(event: MouseEvent): void {
    event.stopPropagation();
    this.guestPickerOpen = !this.guestPickerOpen;
  }

  keepGuestPickerOpen(event: MouseEvent): void {
    event.stopPropagation();
  }

  adjustGuestCounter(type: 'rooms' | 'adults' | 'children', delta: number): void {
    if (type === 'rooms') {
      this.searchRooms = this.clamp(this.searchRooms + delta, 1, 10);
      return;
    }

    if (type === 'adults') {
      this.searchAdults = this.clamp(this.searchAdults + delta, 1, 20);
      this.searchGuests = this.totalGuests;
      return;
    }

    this.searchChildren = this.clamp(this.searchChildren + delta, 0, 10);
    this.searchGuests = this.totalGuests;
  }

  get totalGuests(): number {
    return this.searchAdults + this.searchChildren;
  }

  get guestSummary(): string {
    const guestText = `${this.totalGuests} khách`;
    return this.searchRooms > 1 ? `${guestText} · ${this.searchRooms} phòng` : guestText;
  }

  private loadFeaturedHotels(): void {
    this.featuredHotelsLoading = true;

    this.roomService
      .search({
        page: 0,
        size: 12,
        guests: Math.max(this.totalGuests, 1),
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page) => {
          this.featuredHotels = this.toFeaturedHotels(page.content).slice(0, 4);
          this.featuredHotelsLoading = false;
        },
        error: (err) => {
          console.error('Load featured hotels failed:', err);
          this.featuredHotels = [];
          this.featuredHotelsLoading = false;
        },
      });
  }

  private toFeaturedHotels(rooms: RoomSearchResult[]): FeaturedHotel[] {
    const byHotel = new Map<string, FeaturedHotel>();

    for (const room of rooms) {
      if (!room.hotelId || byHotel.has(room.hotelId)) continue;

      const rating = Number(room.hotelRating || 0);
      byHotel.set(room.hotelId, {
        id: room.hotelId,
        hotelId: room.hotelId,
        roomId: room.id,
        roomName: room.name,
        name: room.hotelName,
        city: room.hotelCity,
        starRating: this.ratingToStars(rating),
        userRating: rating,
        reviewCount: 0,
        thumbnail: (room.images && room.images[0]) || room.hotelThumbnail || '',
        pricePerNight: room.basePrice,
      });
    }

    return Array.from(byHotel.values());
  }

  private ratingToStars(rating: number): number {
    if (!Number.isFinite(rating) || rating <= 0) return 0;
    return Math.min(5, Math.max(1, Math.round(rating / 2)));
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
