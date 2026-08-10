import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { PaymentResponse, PaymentService } from '../../../core/services/payment.service';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-payment-callback',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzSpinModule,
    NzIconModule,
    NzResultModule,
    NavbarComponent,
  ],
  templateUrl: './callback.component.html',
  styleUrl: './callback.component.scss',
})
export class PaymentCallbackComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private paymentService = inject(PaymentService);
  private http = inject(HttpClient);

  status: 'loading' | 'success' | 'failed' | 'error' = 'loading';
  message = 'Đang xác nhận thanh toán...';
  bookingId = '';
  paymentId = '';
  private pollInterval: ReturnType<typeof setInterval> | null = null;
  private pollCount = 0;
  private readonly maxPoll = 30;

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.bookingId = params['bookingId'] || params['vnp_OrderInfo'] || '';
    this.paymentId = params['paymentId'] || '';

    const orderCode = params['orderCode'];
    if (orderCode) {
      this.handlePayOSReturn(orderCode);
      return;
    }

    if (this.bookingId) {
      this.startPolling();
      return;
    }

    this.status = 'error';
        this.message = 'Không tìm thấy thông tin thanh toán';
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private handlePayOSReturn(orderCode: string): void {
    if (!this.bookingId) {
      this.status = 'error';
    this.message = 'Không tìm thấy thông tin thanh toán';
      return;
    }

    let httpParams = new HttpParams();
    Object.entries(this.route.snapshot.queryParams).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        httpParams = httpParams.set(key, String(value));
      }
    });

    this.http
      .get<{ success: boolean; status?: string }>(
        '/api/payments/callback/vietqr/check',
        { params: httpParams },
      )
      .subscribe({
        next: (result) => {
          if (result?.status === 'CANCELLED' || result?.status === 'EXPIRED') {
            this.router.navigate(['/payment/failed'], {
              queryParams: { bookingId: this.bookingId },
            });
            return;
          }
          this.startPolling();
        },
        error: () => this.startPolling(),
      });
  }

  private startPolling(): void {
    this.stopPolling();
    this.pollCount = 0;
    this.checkPaymentStatus();

    this.pollInterval = setInterval(() => {
      this.pollCount++;
      if (this.pollCount >= this.maxPoll) {
        this.stopPolling();
        this.status = 'error';
        this.message = 'Quá thời gian chờ xác nhận. Vui lòng kiểm tra lại sau.';
        return;
      }
      this.checkPaymentStatus();
    }, 2000);
  }

  private stopPolling(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  private checkPaymentStatus(): void {
    this.paymentService.getPaymentByBooking(this.bookingId).subscribe({
      next: (payment: PaymentResponse) => {
        if (payment.status === 'SUCCESS') {
          this.stopPolling();
          this.status = 'success';
          this.message = 'Thanh toán thành công!';
          this.paymentId = payment.id;

          setTimeout(() => {
            this.router.navigate(['/payment/success'], {
              queryParams: {
                bookingId: this.bookingId,
                paymentId: payment.id,
                paymentCode: payment.paymentCode,
              },
            });
          }, 1500);
          return;
        }

        if (payment.status === 'FAILED' || payment.status === 'CANCELLED') {
          this.stopPolling();
          this.status = 'failed';
          this.message = payment.status === 'CANCELLED'
            ? 'Thanh toán đã bị hủy'
            : 'Thanh toán thất bại';

          setTimeout(() => {
            this.router.navigate(['/payment/failed'], {
              queryParams: { bookingId: this.bookingId },
            });
          }, 1500);
          return;
        }

        if (payment.status === 'EXPIRED') {
          this.stopPolling();
          this.status = 'failed';
          this.message = 'Thanh toán đã hết hạn';
        }
      },
      error: () => {
        // Keep polling while the gateway callback/webhook is still settling.
      },
    });
  }
}
