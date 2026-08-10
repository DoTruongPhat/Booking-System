import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { catchError, finalize, of, timeout } from 'rxjs';

import { BookingService } from '../../../../core/services/booking.service';
import {
  Booking,
  BOOKING_STATUS_COLORS,
  BOOKING_STATUS_LABELS,
  PAYMENT_METHOD_LABELS,
  PAYMENT_STATUS_LABELS,
  PaymentStatus,
} from '../../../../core/models/booking.model';

interface BookingProgressStep {
  label: string;
  icon: string;
  state: 'done' | 'current' | 'todo' | 'danger';
}

@Component({
  selector: 'app-user-booking-detail',
  standalone: true,
  imports: [
    CommonModule,
    NzButtonModule,
    NzEmptyModule,
    NzIconModule,
    NzSpinModule,
    NzTagModule,
  ],
  templateUrl: './booking-detail.component.html',
  styleUrl: './booking-detail.component.scss',
})
export class UserBookingDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly bookingService = inject(BookingService);
  private readonly message = inject(NzMessageService);

  booking: Booking | null = null;
  loading = false;
  loadTimedOut = false;
  currentBookingId = '';
  progressSteps: BookingProgressStep[] = [];

  readonly statusLabels = BOOKING_STATUS_LABELS;
  readonly statusColors = BOOKING_STATUS_COLORS;
  readonly paymentLabels = PAYMENT_STATUS_LABELS;
  readonly paymentMethodLabels = PAYMENT_METHOD_LABELS;
  readonly paymentColors: Record<PaymentStatus, string> = {
    UNPAID: 'orange',
    PAID: 'green',
    REFUNDED: 'green',
    PARTIALLY_REFUNDED: 'green',
  };

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/user/bookings']);
      return;
    }

    this.currentBookingId = id;
    this.loadBooking(id);
  }

  loadBooking(id: string): void {
    this.currentBookingId = id;
    this.loading = true;
    this.loadTimedOut = false;
    this.booking = null;
    this.progressSteps = [];

    this.bookingService
      .getBookingById(id)
      .pipe(
        timeout(8000),
        catchError((err) => {
          this.booking = null;
          this.progressSteps = [];
          this.loadTimedOut = err?.name === 'TimeoutError';
          this.message.error(
            this.loadTimedOut
              ? 'API đang chậm, vui lòng thử tải lại.'
              : 'Không thể tải chi tiết đặt phòng.',
          );
          return of(null);
        }),
        finalize(() => (this.loading = false)),
      )
      .subscribe({
        next: (booking) => {
          if (!booking?.id) {
            return;
          }

          this.booking = booking;
          this.progressSteps = this.buildProgressSteps(booking);
        },
      });
  }

  goBack(): void {
    this.router.navigate(['/user/bookings']);
  }

  payBooking(): void {
    if (!this.booking) return;
    this.router.navigate(['/booking/checkout', this.booking.id]);
  }

  copyBookingId(): void {
    if (!this.booking) return;
    navigator.clipboard.writeText(this.booking.id);
    this.message.success('Đã sao chép mã đặt phòng.');
  }

  canPay(): boolean {
    return (
      this.booking?.paymentStatus === 'UNPAID' &&
      (this.booking.status === 'PENDING' || this.booking.status === 'CONFIRMED')
    );
  }

  get heroImage(): string {
    return this.booking?.imageUrl || this.booking?.roomImages?.[0] || '/images/Logo.jpg';
  }

  get guestSummary(): string {
    const adults = this.booking?.guests?.adults ?? 0;
    const children = this.booking?.guests?.children ?? 0;
    return children > 0 ? `${adults} người lớn · ${children} trẻ em` : `${adults} người lớn`;
  }

  get paymentMethodText(): string {
    const method = this.booking?.paymentMethod;
    return method ? this.paymentMethodLabels[method] || method : 'Chưa chọn';
  }

  get discountAmount(): number {
    return Number(this.booking?.discountAmount ?? 0);
  }

  get originalAmount(): number {
    return Number(this.booking?.finalPrice ?? this.booking?.totalPrice ?? 0) + this.discountAmount;
  }

  get voucherLabel(): string {
    const code = this.booking?.voucherCode;
    return code ? `Mã ${code}` : 'Mã giảm giá';
  }

  trackByProgressStep(_: number, step: BookingProgressStep): string {
    return `${step.label}-${step.state}`;
  }

  private buildProgressSteps(booking: Booking | null): BookingProgressStep[] {
    if (!booking) return [];

    const paid = ['PAID', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(booking.paymentStatus);
    const confirmed =
      paid &&
      (booking.status === 'CONFIRMED' || booking.status === 'CHECKED_IN' || booking.status === 'COMPLETED');
    const checkedIn = booking.status === 'CHECKED_IN' || booking.status === 'COMPLETED';
    const completed = booking.status === 'COMPLETED';
    const waitingForPayment =
      !paid && (booking.status === 'PENDING' || booking.status === 'CONFIRMED');
    const hasRefund = Number(booking.refundAmount ?? 0) > 0;

    if (booking.status === 'CANCELLED') {
      return [
        { label: 'Đã đặt', icon: 'file-done', state: 'done' },
        { label: paid ? 'Đã thanh toán' : 'Chưa thanh toán', icon: 'credit-card', state: paid ? 'done' : 'todo' },
        { label: 'Đã hủy', icon: 'close-circle', state: 'danger' },
        { label: hasRefund ? 'Đã hoàn tiền' : 'Kết thúc', icon: hasRefund ? 'rollback' : 'stop', state: hasRefund ? 'done' : 'todo' },
      ];
    }

    if (booking.status === 'NO_SHOW') {
      return [
        { label: 'Đã đặt', icon: 'file-done', state: 'done' },
        { label: paid ? 'Đã thanh toán' : 'Chưa thanh toán', icon: 'credit-card', state: paid ? 'done' : 'todo' },
        { label: 'Đã xác nhận', icon: 'check-circle', state: confirmed ? 'done' : 'todo' },
        { label: 'Không đến', icon: 'warning', state: 'danger' },
      ];
    }

    const steps: BookingProgressStep[] = [
      { label: 'Đã đặt', icon: 'file-done', state: 'done' },
      { label: paid ? 'Đã thanh toán' : 'Chờ thanh toán', icon: 'credit-card', state: paid ? 'done' : 'current' },
      { label: confirmed ? 'Đã xác nhận' : 'Chờ xác nhận', icon: 'check-circle', state: confirmed ? 'done' : paid ? 'current' : 'todo' },
      { label: completed ? 'Đã trả phòng' : 'Chờ lưu trú', icon: 'home', state: completed ? 'done' : checkedIn ? 'current' : confirmed ? 'current' : 'todo' },
      { label: 'Hoàn tất', icon: 'flag', state: completed ? 'done' : 'todo' },
    ];

    return waitingForPayment
      ? steps.map((step, index) => (index > 1 ? { ...step, state: 'todo' as const } : step))
      : steps;
  }

  formatPrice(price?: number | null): string {
    return new Intl.NumberFormat('vi-VN').format(price || 0) + 'đ';
  }

  formatDate(date?: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  formatDateTime(date?: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
