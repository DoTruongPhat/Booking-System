import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { Auth } from '../../../core/services/auth';
import { BookingService } from '../../../core/services/booking.service';
import { Booking, BookingStatus } from '../../../core/models/booking.model';
import { generateIdempotencyKey } from '../../../core/http/idempotency';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzButtonModule,
    NzTagModule,
    NzSpinModule,
    NzEmptyModule,
    NzPaginationModule,
    NavbarComponent,
  ],
  templateUrl: './bookings.component.html',
  styleUrl: './bookings.component.scss',
})
export class MyBookingsComponent implements OnInit {
  private auth = inject(Auth);
  private router = inject(Router);
  private bookingService = inject(BookingService);
  private message = inject(NzMessageService);
  private http = inject(HttpClient);

  // Filter: aligned with backend 5 statuses
  filterStatus: 'ALL' | BookingStatus = 'ALL';

  // Data from API
  bookings: Booking[] = [];
  isLoading = false;

  // Pagination
  pageIndex = 1;
  pageSize = 10;
  total = 0;

  // Status config — aligned with backend (5 statuses, NO CHECKED_IN/CHECKED_OUT)
  readonly statusColors: Record<BookingStatus, string> = {
    PENDING: 'orange',
    CONFIRMED: 'green',
    CHECKED_IN: 'blue',
    COMPLETED: 'green',
    CANCELLED: 'red',
    NO_SHOW: 'default',
  };

  readonly statusLabels: Record<BookingStatus, string> = {
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Đã xác nhận',
    CHECKED_IN: 'Đã nhận phòng',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
    NO_SHOW: 'Không đến',
  };

  ngOnInit() {
    this.loadBookings();
  }

  loadBookings() {
    this.isLoading = true;
    const status = this.filterStatus !== 'ALL' ? this.filterStatus : undefined;

    this.bookingService
      .getMyBookings({ page: this.pageIndex - 1, size: this.pageSize, status })
      .subscribe({
        next: (data) => {
          this.bookings = data.content;
          this.total = data.totalElements;
          this.pageIndex = data.number + 1;
          this.isLoading = false;
        },
        error: () => {
          this.message.error('Không thể tải danh sách đặt phòng');
          this.isLoading = false;
        },
      });
  }

  get filteredBookings(): Booking[] {
    // Server-side filtering, just return all
    return this.bookings;
  }

  setFilter(status: 'ALL' | BookingStatus) {
    this.filterStatus = status;
    this.pageIndex = 1;
    this.loadBookings();
  }

  onPageChange(page: number) {
    this.pageIndex = page;
    this.loadBookings();
  }

  getStatusColor(status: BookingStatus): string {
    return this.statusColors[status] || 'default';
  }

  getStatusLabel(status: BookingStatus): string {
    return this.statusLabels[status] || status;
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price) + 'đ';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  viewDetail(id: string): void {
    this.router.navigate(['/user/bookings', id]);
  }

  cancelBooking(id: string): void {
    if (confirm('Bạn có chắc chắn muốn hủy đặt phòng này?')) {
      this.bookingService
        .cancelBooking(id, 'Hủy bởi khách hàng', generateIdempotencyKey())
        .subscribe({
          next: () => {
            this.message.success('Đã hủy đặt phòng');
            this.loadBookings();
          },
          error: (err) => {
            this.message.error(err?.error?.message || 'Không thể hủy đặt phòng');
          },
        });
    }
  }

  // ── Download & Payment ─────────────────────

  onDownloadPdf(b: any): void {
    this.http
      .get(`/api/user/bookings/${b.id}/confirmation.pdf`, {
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `booking-${b.bookingCode}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
          this.message.success('Đang tải PDF...');
        },
        error: () => this.message.error('Không thể tải PDF'),
      });
  }

  onDownloadReceipt(b: any): void {
    this.http
      .get(`/api/user/bookings/${b.id}/receipt.pdf`, {
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `receipt-${b.bookingCode}.pdf`;
          a.click();
          window.URL.revokeObjectURL(url);
          this.message.success('Đang tải biên lai...');
        },
        error: () => this.message.error('Không thể tải biên lai'),
      });
  }

  onPayment(b: any): void {
    this.router.navigate(['/booking/checkout', b.id]);
  }
}
