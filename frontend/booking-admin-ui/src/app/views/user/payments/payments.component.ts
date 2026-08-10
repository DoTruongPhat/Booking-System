import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { BookingService } from '../../../core/services/booking.service';
import { PaymentResponse, PaymentService } from '../../../core/services/payment.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-user-payments',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
    NzSelectModule,
    NzEmptyModule,
    NzSpinModule,
    NzPaginationModule,
    NzTooltipModule,
    NzCardModule,
    NavbarComponent,
  ],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.scss',
})
export class UserPaymentsComponent implements OnInit {
  private paymentService = inject(PaymentService);
  private bookingService = inject(BookingService);
  private router = inject(Router);
  private message = inject(NzMessageService);

  payments: PaymentResponse[] = [];
  loading = false;
  pageIndex = 1;
  pageSize = 10;
  total = 0;

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.loading = true;

    this.bookingService.getMyBookings({ page: this.pageIndex - 1, size: this.pageSize }).subscribe({
      next: (data) => {
        this.total = data.totalElements;
        const bookings = data.content;
        this.payments = [];
        let loaded = 0;

        if (bookings.length === 0) {
          this.loading = false;
          return;
        }

        for (const booking of bookings) {
          this.paymentService.getPaymentByBooking(booking.id).subscribe({
            next: (payment) => {
              this.payments.push(payment);
              loaded++;
              if (loaded === bookings.length) {
                this.sortPayments();
                this.loading = false;
              }
            },
            error: () => {
              loaded++;
              if (loaded === bookings.length) {
                this.loading = false;
              }
            },
          });
        }
      },
      error: () => {
        this.message.error('Không thể tải lịch sử thanh toán');
        this.loading = false;
      },
    });
  }

  onPageChange(page: number): void {
    this.pageIndex = page;
    this.loadPayments();
  }

  viewBooking(bookingId: string): void {
    this.router.navigate(['/user/bookings'], { queryParams: { bookingId } });
  }

  cancelPayment(payment: PaymentResponse): void {
    if (payment.status !== 'PENDING') return;

    this.paymentService.cancelPayment(payment.id, 'User cancelled').subscribe({
      next: () => {
        this.message.success('Đã hủy thanh toán');
        this.loadPayments();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể hủy');
      },
    });
  }

  retryPayment(payment: PaymentResponse): void {
    this.router.navigate(['/booking/checkout', payment.bookingId]);
  }

  getStatusLabel(status: string): string {
    return this.paymentService.getStatusLabel(status);
  }

  getStatusColor(status: string): string {
    return this.paymentService.getStatusColor(status);
  }

  getMethodLabel(method: string): string {
    return this.paymentService.getMethodLabel(method);
  }

  formatVND(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
  }

  formatDateTime(date: string): string {
    return date ? new Date(date).toLocaleString('vi-VN') : '-';
  }

  private sortPayments(): void {
    this.payments.sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }
}
