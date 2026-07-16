// ═══════════════════════════════════════════════════════════
// HOTEL DETAIL COMPONENT — REFACTORED Phase E
// Route: /hotels/:id (thực tế là room detail)
//
// Bỏ toàn bộ mock data → gọi:
//   GET /api/rooms/{roomId}  → room + hotel info + 30 ngày availability
//   POST /api/user/bookings  → tạo booking
// ═══════════════════════════════════════════════════════════

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import {
  RoomService,
  RoomDetail,
  DayAvailability,
} from '../../../../app/core/services/rooms.service';
import { BookingService } from '../../../core/services/booking.service';
import { CreateBookingRequest } from '../../../core/models/booking.model';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-hotel-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzIconModule,
    NzButtonModule,
    NzSpinModule,
    NzTagModule,
    NavbarComponent,
  ],
  templateUrl: './detail.component.html',
  styleUrl: './detail.component.scss',
})
export class HotelDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private roomService = inject(RoomService);
  private bookingService = inject(BookingService);
  private auth = inject(Auth);
  private message = inject(NzMessageService);
  private modal = inject(NzModalService);

  // ── State ─────────────────────────────────────────────
  roomId: string | null = null;
  room: RoomDetail | null = null;
  loading = true;
  booking = false; // loading state cho nút đặt phòng
  activeImageIndex = 0;

  // ── Booking form ──────────────────────────────────────
  checkIn = '';
  checkOut = '';
  guests = { adults: 2, children: 0, rooms: 1 };
  specialRequests = '';

  // Guest info
  guestName = '';
  guestEmail = '';
  guestPhone = '';

  // ── Availability calendar ─────────────────────────────
  availability: DayAvailability[] = [];

  // ── Amenity labels ────────────────────────────────────
  amenityLabels: Record<string, string> = {
    wifi: 'WiFi miễn phí',
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
    shower: 'Vòi sen',
    hairdryer: 'Máy sấy',
    desk: 'Bàn làm việc',
    kitchen: 'Bếp',
    'washing-machine': 'Máy giặt',
    'pool-view': 'View hồ bơi',
  };

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('id');

    // Set default dates (tomorrow → +2 days)
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfter = new Date(today);
    dayAfter.setDate(dayAfter.getDate() + 2);
    this.checkIn = this.formatDateISO(tomorrow);
    this.checkOut = this.formatDateISO(dayAfter);

    // Đọc dates từ query params nếu có (từ search page)
    const qp = this.route.snapshot.queryParams;
    if (qp['checkIn']) this.checkIn = qp['checkIn'];
    if (qp['checkOut']) this.checkOut = qp['checkOut'];
    if (qp['guests']) this.guests.adults = +qp['guests'];

    this.loadRoom();
  }

  // ══════════════════════════════════════════════════════
  // LOAD DATA
  // ══════════════════════════════════════════════════════

  private loadRoom(): void {
    if (!this.roomId) return;
    this.loading = true;

    this.roomService.getById(this.roomId).subscribe({
      next: (data) => {
        this.room = data;
        this.availability = data.availability || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Load room error:', err);
        this.message.error('Không thể tải thông tin phòng.');
        this.loading = false;
      },
    });
  }

  // ══════════════════════════════════════════════════════
  // IMAGE GALLERY
  // ══════════════════════════════════════════════════════

  selectImage(index: number): void {
    this.activeImageIndex = index;
  }

  get allImages(): string[] {
    return this.room?.images || [];
  }

  // ══════════════════════════════════════════════════════
  // BOOKING
  // ══════════════════════════════════════════════════════

  get nights(): number {
    if (!this.checkIn || !this.checkOut) return 0;
    return this.bookingService.calculateNights(this.checkIn, this.checkOut);
  }

  get totalPrice(): number {
    if (!this.room || this.nights <= 0) return 0;
    return this.room.basePrice * this.nights * this.guests.rooms;
  }

  get taxAmount(): number {
    return Math.round(this.totalPrice * 0.1);
  }

  get finalPrice(): number {
    return this.totalPrice + this.taxAmount;
  }

  bookNow(): void {
    // Check login
    if (!this.auth.isLoggedIn()) {
      this.message.warning('Vui lòng đăng nhập để đặt phòng.');
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: this.router.url },
      });
      return;
    }

    // Validate
    if (!this.checkIn || !this.checkOut) {
      this.message.warning('Vui lòng chọn ngày nhận và trả phòng.');
      return;
    }
    if (this.nights <= 0) {
      this.message.warning('Ngày trả phòng phải sau ngày nhận phòng.');
      return;
    }
    if (!this.roomId) return;

    if (!this.guestName || !this.guestEmail) {
      this.message.warning('Vui lòng nhập tên và email khách hàng.');
      return;
    }

    // Confirm
    this.modal.confirm({
      nzTitle: 'Xác nhận đặt phòng',
      nzContent: `
        <p><strong>${this.room?.name}</strong> tại ${this.room?.hotel?.name}</p>
        <p>${this.checkIn} → ${this.checkOut} (${this.nights} đêm)</p>
        <p>Tổng: <strong>${this.formatPrice(this.finalPrice)}</strong></p>
      `,
      nzOkText: 'Đặt phòng',
      nzCancelText: 'Hủy',
      nzOnOk: () => this.submitBooking(),
    });
  }

  private submitBooking(): void {
    if (!this.roomId) return;
    this.booking = true;

    const request: CreateBookingRequest = {
      roomId: this.roomId,
      checkInDate: this.checkIn,
      checkOutDate: this.checkOut,
      numGuests: this.guests.adults + this.guests.children,
      numRooms: this.guests.rooms,
      specialRequest: this.specialRequests || undefined,
      guestName: this.guestName,
      guestEmail: this.guestEmail,
      guestPhone: this.guestPhone || undefined,
    };

    this.bookingService.createBooking(request).subscribe({
      next: (booking) => {
        this.booking = false;
        this.message.success('Đặt phòng thành công!');
        this.router.navigate(['/booking/success', booking.id]);
      },
      error: (err) => {
        this.booking = false;
        const msg = err?.error?.message || 'Không thể đặt phòng. Vui lòng thử lại.';
        this.message.error(msg);
      },
    });
  }

  // ══════════════════════════════════════════════════════
  // AVAILABILITY CALENDAR HELPERS
  // ══════════════════════════════════════════════════════

  getDayStatus(day: DayAvailability): 'available' | 'limited' | 'blocked' {
    if (day.blocked || day.available <= 0) return 'blocked';
    if (day.available <= 3) return 'limited';
    return 'available';
  }

  getDayLabel(dateStr: string): string {
    const d = new Date(dateStr);
    return d.getDate().toString();
  }

  getDayOfWeek(dateStr: string): string {
    const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    return days[new Date(dateStr).getDay()];
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

  getStars(rating: number): number[] {
    return Array(Math.floor(rating)).fill(0);
  }

  private formatDateISO(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
