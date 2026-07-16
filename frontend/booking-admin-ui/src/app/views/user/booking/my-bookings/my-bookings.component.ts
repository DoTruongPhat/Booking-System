// ═══════════════════════════════════════════════════════════
// MY BOOKINGS COMPONENT — Phase E
// Route: /user/bookings (authGuard)
//
// Bỏ hardcode → gọi:
//   GET  /api/user/bookings?page=&size=&status=
//   POST /api/user/bookings/{id}/cancel
//
// Features:
//   - Filter tabs: All / Pending / Confirmed / Completed / Cancelled
//   - PENDING countdown timer (15 phút)
//   - Cancel modal với reason + refund warning
//   - Pagination
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, interval, takeUntil } from 'rxjs';

// NgZorro
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';

import { NavbarComponent } from '../../../../shared/components/navbar/navbar.component';
import { BookingService } from '../../../../core/services/booking.service';
import {
  Booking,
  BookingStatus,
  BOOKING_STATUS_LABELS,
  BOOKING_STATUS_COLORS,
  PAYMENT_STATUS_LABELS,
} from '../../../../core/models/booking.model';

interface StatusTab {
  label: string;
  value: BookingStatus | 'ALL';
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
    NzEmptyModule,
    NzSpinModule,
    NzPaginationModule,
    NzModalModule,
    NzInputModule,
  ],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.scss',
})
export class MyBookingsComponent implements OnInit, OnDestroy {
  private bookingService = inject(BookingService);
  private router = inject(Router);
  private modal = inject(NzModalService);
  private message = inject(NzMessageService);

  private destroy$ = new Subject<void>();

  // ── State ─────────────────────────────────────────────
  bookings: Booking[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 1;
  pageSize = 10;

  // ── Filter tabs ───────────────────────────────────────
  activeTab: BookingStatus | 'ALL' = 'ALL';
  tabs: StatusTab[] = [
    { label: 'Tất cả', value: 'ALL' },
    { label: 'Chờ xác nhận', value: 'PENDING' },
    { label: 'Đã xác nhận', value: 'CONFIRMED' },
    { label: 'Hoàn thành', value: 'COMPLETED' },
    { label: 'Đã hủy', value: 'CANCELLED' },
  ];

  // ── Cancel modal ──────────────────────────────────────
  cancelReason = '';
  cancelling = false;

  // ── Countdown timers (PENDING bookings) ───────────────
  countdowns: Record<string, string> = {};

  // ── Labels ────────────────────────────────────────────
  statusLabels = BOOKING_STATUS_LABELS;
  statusColors = BOOKING_STATUS_COLORS;
  paymentLabels = PAYMENT_STATUS_LABELS;

  ngOnInit(): void {
    this.loadBookings();
    this.startCountdownTicker();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ══════════════════════════════════════════════════════
  // LOAD
  // ══════════════════════════════════════════════════════

  loadBookings(): void {
    this.loading = true;

    const params: { page: number; size: number; status?: BookingStatus } = {
      page: this.currentPage - 1,
      size: this.pageSize,
    };
    if (this.activeTab !== 'ALL') {
      params.status = this.activeTab;
    }

    this.bookingService.getMyBookings(params).subscribe({
      next: (data) => {
        this.bookings = data.content;
        this.totalElements = data.totalElements;
        this.currentPage = data.number + 1;
        this.loading = false;
        this.updateCountdowns();
      },
      error: (err) => {
        console.error('Load bookings error:', err);
        this.message.error('Không thể tải danh sách đặt phòng.');
        this.bookings = [];
        this.totalElements = 0;
        this.loading = false;
      },
    });
  }

  onTabChange(index: number): void {
    this.activeTab = this.tabs[index].value;
    this.currentPage = 1;
    this.loadBookings();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadBookings();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ══════════════════════════════════════════════════════
  // CANCEL
  // ══════════════════════════════════════════════════════

  openCancelModal(booking: Booking): void {
    this.cancelReason = '';
    const refundPercent = this.bookingService.estimateRefundPercent(booking.checkIn);

    let warningHtml = '';
    if (refundPercent === 100) {
      warningHtml = '<p style="color:#52c41a"><strong>Hoàn tiền 100%</strong> (hủy trước 48h)</p>';
    } else if (refundPercent === 50) {
      warningHtml =
        '<p style="color:#faad14"><strong>Hoàn tiền 50%</strong> (hủy trong 24-48h)</p>';
    } else {
      warningHtml =
        '<p style="color:#ff4d4f"><strong>Không hoàn tiền</strong> (hủy dưới 24h trước check-in)</p>';
    }

    this.modal.confirm({
      nzTitle: 'Xác nhận hủy đặt phòng',
      nzContent: `
        <div>
          <p><strong>${booking.roomName}</strong> tại ${booking.hotelName}</p>
          <p>${this.formatDate(booking.checkIn)} → ${this.formatDate(booking.checkOut)}</p>
          <p>Tổng: <strong>${this.formatPrice(booking.finalPrice)}</strong></p>
          <hr/>
          ${warningHtml}
          <div style="margin-top:12px">
            <label style="font-weight:600;font-size:13px">Lý do hủy:</label>
            <textarea
              id="cancel-reason-input"
              rows="3"
              style="width:100%;margin-top:4px;padding:8px;border:1px solid #d9d9d9;border-radius:6px;font-size:14px"
              placeholder="Nhập lý do hủy..."
            ></textarea>
          </div>
        </div>
      `,
      nzOkText: 'Xác nhận hủy',
      nzOkDanger: true,
      nzCancelText: 'Giữ đặt phòng',
      nzOnOk: () => {
        const textarea = document.getElementById('cancel-reason-input') as HTMLTextAreaElement;
        const reason = textarea?.value?.trim() || 'Khách hàng hủy đặt phòng';
        return this.submitCancel(booking.id, reason);
      },
    });
  }

  private submitCancel(bookingId: string, reason: string): Promise<void> {
    this.cancelling = true;
    return new Promise((resolve, reject) => {
      this.bookingService.cancelBooking(bookingId, reason).subscribe({
        next: () => {
          this.cancelling = false;
          this.message.success('Đã hủy đặt phòng thành công.');
          this.loadBookings();
          resolve();
        },
        error: (err) => {
          this.cancelling = false;
          const msg = err?.error?.message || 'Không thể hủy. Vui lòng thử lại.';
          this.message.error(msg);
          reject();
        },
      });
    });
  }

  canCancel(booking: Booking): boolean {
    return booking.status === 'PENDING' || booking.status === 'CONFIRMED';
  }

  // ══════════════════════════════════════════════════════
  // COUNTDOWN (PENDING 15 phút)
  // ══════════════════════════════════════════════════════

  private startCountdownTicker(): void {
    interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateCountdowns());
  }

  private updateCountdowns(): void {
    for (const b of this.bookings) {
      if (b.status === 'PENDING') {
        const remaining = this.bookingService.getPendingTimeRemaining(b.createdAt);
        if (remaining > 0) {
          const mins = Math.floor(remaining / 60000);
          const secs = Math.floor((remaining % 60000) / 1000);
          this.countdowns[b.id] = `${mins}:${String(secs).padStart(2, '0')}`;
        } else {
          this.countdowns[b.id] = 'Hết hạn';
        }
      }
    }
  }

  getCountdown(bookingId: string): string | null {
    return this.countdowns[bookingId] || null;
  }

  isExpired(bookingId: string): boolean {
    return this.countdowns[bookingId] === 'Hết hạn';
  }

  // ══════════════════════════════════════════════════════
  // NAVIGATION
  // ══════════════════════════════════════════════════════

  viewDetail(bookingId: string): void {
    // Có thể mở modal hoặc navigate tùy UX
    // Hiện tại chưa có route /user/bookings/:id riêng
    // → dùng API getBookingById nếu cần
  }

  // ══════════════════════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════════════════════

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

  formatDateTime(date: string): string {
    return new Date(date).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  getTabIndex(): number {
    return this.tabs.findIndex((t) => t.value === this.activeTab);
  }
}
