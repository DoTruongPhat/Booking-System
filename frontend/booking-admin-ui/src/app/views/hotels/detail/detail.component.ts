import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { generateIdempotencyKey } from '../../../core/http/idempotency';
import { CreateBookingRequest, VoucherValidation } from '../../../core/models/booking.model';
import { Auth } from '../../../core/services/auth';
import { BookingService } from '../../../core/services/booking.service';
import { DayAvailability, RoomDetail, RoomService } from '../../../core/services/rooms.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-room-detail',
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

  roomId: string | null = null;
  room: RoomDetail | null = null;
  loading = true;
  booking = false;
  activeImageIndex = 0;
  private bookingIdempotencyKey: string | null = null;

  checkIn = '';
  checkOut = '';
  guests = { adults: 2, children: 0, rooms: 1 };
  specialRequests = '';
  voucherCode = '';
  voucherApplying = false;
  appliedVoucher: VoucherValidation | null = null;
  voucherMessage = '';
  guestName = '';
  guestEmail = '';
  guestPhone = '';
  availability: DayAvailability[] = [];

  readonly amenityLabels: Record<string, string> = {
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
    'city-view': 'View thành phố',
    shower: 'Vòi sen',
    hairdryer: 'Máy sấy',
    desk: 'Bàn làm việc',
    kitchen: 'Bếp',
    'washing-machine': 'Máy giặt',
    'pool-view': 'View hồ bơi',
  };

  ngOnInit(): void {
    this.roomId = this.route.snapshot.paramMap.get('id');
    this.initDates();
    this.prefillGuestInfo();
    this.loadRoom();
  }

  private initDates(): void {
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfter = new Date(today);
    dayAfter.setDate(dayAfter.getDate() + 2);

    this.checkIn = this.formatDateISO(tomorrow);
    this.checkOut = this.formatDateISO(dayAfter);

    const qp = this.route.snapshot.queryParams;
    if (qp['checkIn']) this.checkIn = qp['checkIn'];
    if (qp['checkOut']) this.checkOut = qp['checkOut'];
    if (qp['guests']) this.guests.adults = Number(qp['guests']) || 2;
  }

  private prefillGuestInfo(): void {
    const user = this.auth.getUser();
    if (!user) return;

    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    this.guestName = fullName || user.username || '';
    this.guestEmail = user.email || '';
    this.guestPhone = user.phone || '';
  }

  private loadRoom(): void {
    if (!this.roomId) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.roomService.getById(this.roomId).subscribe({
      next: (data) => {
        this.room = data;
        this.availability = data.availability || [];
        this.activeImageIndex = 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('Load room error:', err);
        this.message.error('Không thể tải thông tin phòng.');
        this.loading = false;
      },
    });
  }

  get allImages(): string[] {
    const roomImages = this.room?.images ?? [];
    const hotelImages = this.room?.hotel?.images ?? [];
    return [...roomImages, ...hotelImages].filter((url, index, arr) => arr.indexOf(url) === index);
  }

  get selectedAvailableRooms(): number {
    const days = this.selectedAvailabilityDays;
    if (!days.length) return this.room?.available ?? this.room?.totalRooms ?? 0;
    return Math.min(...days.map((day) => day.available));
  }

  get selectedAvailabilityDays(): DayAvailability[] {
    if (!this.checkIn || !this.checkOut) return [];
    const start = new Date(this.checkIn).getTime();
    const end = new Date(this.checkOut).getTime();
    return this.availability.filter((day) => {
      const time = new Date(day.date).getTime();
      return time >= start && time < end;
    });
  }

  get nights(): number {
    if (!this.checkIn || !this.checkOut) return 0;
    return this.bookingService.calculateNights(this.checkIn, this.checkOut);
  }

  get totalGuests(): number {
    return this.guests.adults + this.guests.children;
  }

  get capacityLimit(): number {
    return (this.room?.maxAdults ?? this.room?.maxChildren ?? 0) + (this.room?.maxChildren ?? 0);
  }

  get totalPrice(): number {
    if (!this.room || this.nights <= 0) return 0;
    return this.room.basePrice * this.nights * this.guests.rooms;
  }

  get taxAmount(): number {
    return 0;
  }

  get discountAmount(): number {
    return Math.min(this.appliedVoucher?.discountAmount || 0, this.totalPrice);
  }

  get finalPrice(): number {
    return Math.max(this.totalPrice - this.discountAmount, 0);
  }

  get cannotBookReason(): string | null {
    if (!this.room?.active) return 'Phòng hiện không nhận đặt';
    if (!this.checkIn || !this.checkOut) return 'Vui lòng chọn ngày nhận và trả phòng';
    if (this.nights <= 0) return 'Ngày trả phòng phải sau ngày nhận phòng';
    if (this.totalGuests <= 0) return 'Vui lòng chọn số khách';
    if (this.totalGuests > this.capacityLimit) return `Phòng tối đa ${this.capacityLimit} khách`;
    if (this.guests.rooms <= 0) return 'Vui lòng chọn số phòng';
    if (this.selectedAvailabilityDays.some((day) => day.blocked || day.available <= 0)) {
      return 'Khoảng ngày đã chọn có ngày hết phòng';
    }
    if (this.selectedAvailableRooms > 0 && this.guests.rooms > this.selectedAvailableRooms) {
      return `Chỉ còn ${this.selectedAvailableRooms} phòng trong khoảng ngày đã chọn`;
    }
    if (!this.guestName.trim() || !this.guestEmail.trim()) {
      return 'Vui lòng nhập tên và email khách hàng';
    }
    return null;
  }

  selectImage(index: number): void {
    this.activeImageIndex = index;
  }

  selectAvailability(day: DayAvailability): void {
    if (day.blocked || day.available <= 0) return;

    const start = new Date(day.date);
    const end = new Date(start);
    end.setDate(start.getDate() + Math.max(1, this.nights || 1));
    this.checkIn = this.formatDateISO(start);
    this.checkOut = this.formatDateISO(end);
    this.onPricingInputChanged();
  }

  onPricingInputChanged(): void {
    if (this.appliedVoucher) {
      this.clearVoucher(false);
    }
  }

  applyVoucher(): void {
    const code = this.voucherCode.trim();
    if (!code) {
      this.message.warning('Vui lòng nhập mã giảm giá.');
      return;
    }
    if (!this.room) return;
    if (this.totalPrice <= 0) {
      this.message.warning('Vui lòng chọn ngày và số phòng trước khi áp dụng mã.');
      return;
    }

    this.voucherApplying = true;
    this.voucherMessage = '';
    this.bookingService
      .validateVoucher({
        code,
        hotelId: this.room.hotelId || this.room.hotel?.id,
        amount: this.totalPrice,
      })
      .subscribe({
        next: (voucher) => {
          this.voucherApplying = false;
          if (!voucher.valid) {
            this.appliedVoucher = null;
            this.voucherMessage = voucher.message || 'Mã giảm giá không hợp lệ.';
            this.message.warning(this.voucherMessage);
            return;
          }

          const appliedCode = voucher.code || code.toUpperCase();
          this.appliedVoucher = { ...voucher, code: appliedCode };
          this.voucherCode = appliedCode;
          this.voucherMessage = voucher.message || 'Mã giảm giá đã được áp dụng.';
          this.message.success(`Đã áp dụng mã ${this.voucherCode}.`);
        },
        error: (err) => {
          this.voucherApplying = false;
          this.appliedVoucher = null;
          this.voucherMessage = err?.error?.message || 'Không thể kiểm tra mã giảm giá.';
          this.message.error(this.voucherMessage);
        },
      });
  }

  clearVoucher(showMessage = true): void {
    this.appliedVoucher = null;
    this.voucherMessage = '';
    if (showMessage) {
      this.voucherCode = '';
    }
  }

  shareRoom(): void {
    const title = this.room?.name || 'SmartBooking';
    const text = this.room?.hotel?.name ? `${title} tại ${this.room.hotel.name}` : title;
    const url = typeof window !== 'undefined' ? window.location.href : '';

    if (typeof navigator !== 'undefined' && navigator.share) {
      navigator.share({ title, text, url }).catch(() => undefined);
      return;
    }

    if (typeof navigator !== 'undefined' && navigator.clipboard && url) {
      navigator.clipboard.writeText(url).then(() => this.message.success('Đã sao chép liên kết phòng'));
      return;
    }

    this.message.info('Trình duyệt không hỗ trợ chia sẻ tự động');
  }

  bookNow(): void {
    if (!this.auth.isLoggedIn()) {
      this.message.warning('Vui lòng đăng nhập để đặt phòng.');
      this.router.navigate(['/auth/login'], {
        queryParams: { returnUrl: this.router.url },
      });
      return;
    }

    const reason = this.cannotBookReason;
    if (reason) {
      this.message.warning(reason);
      return;
    }
    if (this.voucherCode.trim() && !this.appliedVoucher) {
      this.message.warning('Vui lòng bấm áp dụng mã giảm giá trước khi đặt phòng.');
      return;
    }

    this.modal.confirm({
      nzTitle: 'Xác nhận đặt phòng',
      nzContent: `
        <p><strong>${this.room?.name}</strong> tại ${this.room?.hotel?.name || 'khách sạn'}</p>
        <p>${this.checkIn} → ${this.checkOut} (${this.nights} đêm)</p>
        ${this.discountAmount > 0 ? `<p>Mã giảm: <strong>-${this.formatPrice(this.discountAmount)}</strong></p>` : ''}
        <p>Tổng: <strong>${this.formatPrice(this.finalPrice)}</strong></p>
      `,
      nzOkText: 'Đặt phòng',
      nzCancelText: 'Hủy',
      nzOnOk: () => this.submitBooking(),
    });
  }

  private submitBooking(): void {
    if (!this.roomId || this.booking) return;
    this.booking = true;

    const request: CreateBookingRequest = {
      roomId: this.roomId,
      checkInDate: this.checkIn,
      checkOutDate: this.checkOut,
      numGuests: this.totalGuests,
      numRooms: this.guests.rooms,
      specialRequest: this.specialRequests || undefined,
      guestName: this.guestName.trim(),
      guestEmail: this.guestEmail.trim(),
      guestPhone: this.guestPhone.trim() || undefined,
      voucherCode: this.appliedVoucher?.code || this.voucherCode.trim().toUpperCase() || undefined,
    };

    this.bookingIdempotencyKey = this.bookingIdempotencyKey ?? generateIdempotencyKey();

    this.bookingService.createBooking(request, this.bookingIdempotencyKey).subscribe({
      next: (booking) => {
        this.booking = false;
        this.bookingIdempotencyKey = null;
        this.message.success('Đặt phòng thành công!');
        this.router.navigate(['/booking/checkout', booking.id]);
      },
      error: (err) => {
        this.booking = false;
        if (err?.status !== 409) {
          this.bookingIdempotencyKey = null;
        }
        const msg = err?.error?.message || 'Không thể đặt phòng. Vui lòng thử lại.';
        this.message.error(msg);
      },
    });
  }

  getDayStatus(day: DayAvailability): 'available' | 'limited' | 'blocked' {
    if (day.blocked || day.available <= 0) return 'blocked';
    if (day.available <= 3) return 'limited';
    return 'available';
  }

  getDayLabel(dateStr: string): string {
    return new Date(dateStr).getDate().toString();
  }

  getDayOfWeek(dateStr: string): string {
    const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    return days[new Date(dateStr).getDay()];
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(Number.isFinite(price) ? price : 0) + 'đ';
  }

  getAmenityLabel(key: string): string {
    return this.amenityLabels[key] || key;
  }

  getStars(rating: number): number[] {
    return Array(Math.max(0, Math.floor(rating || 0))).fill(0);
  }

  private formatDateISO(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
