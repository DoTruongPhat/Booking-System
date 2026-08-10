import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { PaymentService, PaymentResponse } from '../../../core/services/payment.service';
import { BookingService } from '../../../core/services/booking.service';
import { Booking } from '../../../core/models/booking.model';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzButtonModule,
    NzSpinModule,
    NavbarComponent,
  ],
  templateUrl: './success.component.html',
  styleUrl: './success.component.scss',
})
export class PaymentSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private bookingService = inject(BookingService);
  private message = inject(NzMessageService);

  loading = true;
  bookingId = '';
  paymentId = '';
  paymentCode = '';
  booking: Booking | null = null;
  payment: PaymentResponse | null = null;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.bookingId = params['bookingId'] || '';
    this.paymentId = params['paymentId'] || '';
    this.paymentCode = params['paymentCode'] || '';

    if (this.bookingId) {
      this.loadData();
    } else {
      this.loading = false;
    }
  }

  private loadData(): void {
    this.bookingService.getBookingById(this.bookingId).subscribe({
      next: (booking) => {
        this.booking = booking;
        this.loadPayment();
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  private loadPayment(): void {
    this.paymentService.getPaymentByBooking(this.bookingId).subscribe({
      next: (payment) => {
        this.payment = payment;
        this.paymentCode = payment.paymentCode;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  copyPaymentCode(): void {
    navigator.clipboard.writeText(this.paymentCode);
    this.message.success('Đã sao chép mã thanh toán!');
  }

  formatVND(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount) + 'đ';
  }

  formatDateTime(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleString('vi-VN');
  }
}
