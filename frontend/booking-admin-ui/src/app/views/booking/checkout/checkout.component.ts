import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { BookingService } from '../../../core/services/booking.service';
import { PaymentService } from '../../../core/services/payment.service';
import { Booking } from '../../../core/models/booking.model';
import { generateIdempotencyKey } from '../../../core/http/idempotency';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzButtonModule,
    NzIconModule,
    NzSpinModule,
    NzRadioModule,
    NzTagModule,
    NzAlertModule,
    NavbarComponent,
  ],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private bookingService = inject(BookingService);
  private paymentService = inject(PaymentService);
  private message = inject(NzMessageService);

  bookingId = '';
  booking: Booking | null = null;
  loading = true;
  submitting = false;
  private paymentIdempotencyKey: string | null = null;

  selectedMethod: 'VIETQR' = 'VIETQR';

  // Countdown timer
  remainingSeconds = 0;
  countdownText = '';
  private countdownInterval: any;

  ngOnInit(): void {
    this.bookingId = this.route.snapshot.paramMap.get('bookingId') || '';
    if (this.bookingId) {
      this.loadBooking();
    } else {
      this.message.error('Không tìm thấy booking');
      this.router.navigate(['/user/bookings']);
    }
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  private loadBooking(): void {
    this.loading = true;
    this.bookingService.getBookingById(this.bookingId).subscribe({
      next: (booking) => {
        this.booking = booking;
        this.loading = false;

        if (booking.status !== 'PENDING') {
          this.message.warning('Booking không ở trạng thái chờ thanh toán');
          this.router.navigate(['/user/bookings']);
          return;
        }

        this.startCountdown(booking.createdAt, booking.paymentExpiresAt);
      },
      error: () => {
        this.message.error('Không thể tải thông tin booking');
        this.loading = false;
      },
    });
  }

  private startCountdown(createdAt: string, paymentExpiresAt?: string | null): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }

    const expiresAt = paymentExpiresAt
      ? new Date(paymentExpiresAt).getTime()
      : new Date(createdAt).getTime() + environment.pendingPaymentMinutes * 60 * 1000;

    const tick = () => {
      const remaining = expiresAt - Date.now();
      if (remaining <= 0) {
        this.remainingSeconds = 0;
        this.countdownText = 'Hết hạn!';
        clearInterval(this.countdownInterval);
        this.message.error('Booking đã hết hạn thanh toán');
        this.router.navigate(['/user/bookings']);
        return;
      }

      this.remainingSeconds = Math.floor(remaining / 1000);
      this.countdownText = this.formatCountdown(this.remainingSeconds);
    };

    tick();
    this.countdownInterval = setInterval(tick, 1000);
  }

  private formatCountdown(totalSeconds: number): string {
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;

    if (days > 0) {
      return `${days} ngày ${hours} giờ ${mins} phút`;
    }
    if (hours > 0) {
      return `${hours}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  onSubmitPayment(): void {
    if (!this.booking || this.submitting) return;

    this.submitting = true;
    this.paymentIdempotencyKey = this.paymentIdempotencyKey ?? generateIdempotencyKey();

    this.paymentService
      .initPayment({
        bookingId: this.bookingId,
        amount: this.getPayableAmount(),
        method: this.selectedMethod,
      }, this.paymentIdempotencyKey)
      .subscribe({
        next: (result) => {
          this.submitting = false;
          this.paymentIdempotencyKey = null;

          if (result.paymentUrl) {
            window.location.href = result.paymentUrl;
          } else {
            this.message.success('Thanh toán thành công!');
            this.router.navigate(['/payment/success'], {
              queryParams: { bookingId: this.bookingId, paymentId: result.paymentId },
            });
          }
        },
        error: (err) => {
          this.submitting = false;
          if (err?.status !== 409) {
            this.paymentIdempotencyKey = null;
          }
          const msg = err?.error?.message || 'Không thể khởi tạo thanh toán';
          this.message.error(msg);
        },
      });
  }

  formatVND(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(Number.isFinite(amount) ? amount : 0) + ' đ';
  }

  getPayableAmount(): number {
    if (!this.booking) return 0;
    return Number(this.booking.finalPrice ?? this.booking.totalPrice ?? 0);
  }

  getDiscountAmount(): number {
    return Number(this.booking?.discountAmount ?? 0);
  }

  getOriginalAmount(): number {
    return this.getPayableAmount() + this.getDiscountAmount();
  }

  getVoucherLabel(): string {
    const code = this.booking?.voucherCode;
    return code ? `Mã ${code}` : 'Mã giảm giá';
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }
}
