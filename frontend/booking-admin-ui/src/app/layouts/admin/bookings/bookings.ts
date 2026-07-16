// ═══════════════════════════════════════════════════════════
// ADMIN BOOKINGS PAGE — Phase E (API Integration)
// Admin: GET /api/admin/bookings — tất cả bookings
// Host:  GET /api/host/bookings — bookings hotels của mình
//        POST /api/host/bookings/{id}/force-cancel
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzDividerModule } from 'ng-zorro-antd/divider';

import { AdminBookingService } from '../../../core/services/admin-booking.service';
import { HotelService } from '../../../core/services/hotel.service';
import { Auth } from '../../../core/services/auth';
import { Booking, BookingStatus, PaymentStatus } from '../../../core/models/booking.model';
import { Hotel } from '../../../core/models/hotel.model';

@Component({
  selector: 'app-admin-bookings',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzPaginationModule,
    NzPopconfirmModule,
    NzSpaceModule,
    NzTooltipModule,
    NzCardModule,
    NzAvatarModule,
    NzDatePickerModule,
    NzEmptyModule,
    NzAlertModule,
    NzDividerModule,
  ],
  templateUrl: './bookings.html',
  styleUrl: './bookings.scss',
})
export class AdminBookings implements OnInit {
  private adminBookingService = inject(AdminBookingService);
  private hotelService = inject(HotelService);
  private auth = inject(Auth);
  private message = inject(NzMessageService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  // ── Data ────────────────────────────────────────────────
  bookings: Booking[] = [];
  isLoading = false;
  isHost = false;

  // ── Host: hotel list ────────────────────────────────────
  hotels: Hotel[] = [];
  selectedHotelId = '';

  // ── Paging (server-side) ────────────────────────────────
  pageIndex = 1;
  pageSize = 10;
  total = 0;

  // ── Filters ─────────────────────────────────────────────
  filterStatus: BookingStatus | 'all' = 'all';
  filterPayment: PaymentStatus | 'all' = 'all';
  filterHotel: string | 'all' = 'all';
  keyword = '';
  dateRange: [Date, Date] | null = null;

  // ── Modals ──────────────────────────────────────────────
  isStatusModalOpen = false;
  isCancelModalOpen = false;
  isRefundModalOpen = false;
  isPayModalOpen = false;
  selectedBooking: Booking | null = null;
  isSubmitting = false;

  statusForm = this.fb.nonNullable.group({ newStatus: ['', Validators.required], note: [''] });
  cancelForm2 = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });
  refundForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  // ── Status config (aligned with backend — 5 statuses) ──
  readonly allStatuses: BookingStatus[] = [
    'PENDING',
    'CONFIRMED',
    'COMPLETED',
    'CANCELLED',
    'NO_SHOW',
  ];
  readonly allPaymentStatuses: PaymentStatus[] = [
    'UNPAID',
    'PAID',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
  ];

  readonly statusConfig: Record<BookingStatus, { label: string; color: string }> = {
    PENDING: { label: 'Chờ xác nhận', color: 'orange' },
    CONFIRMED: { label: 'Đã xác nhận', color: 'green' },
    COMPLETED: { label: 'Hoàn thành', color: 'blue' },
    CANCELLED: { label: 'Đã hủy', color: 'red' },
    NO_SHOW: { label: 'Không đến', color: 'red' },
  };

  readonly paymentConfig: Record<PaymentStatus, { label: string; color: string }> = {
    UNPAID: { label: 'Chưa thanh toán', color: 'orange' },
    PAID: { label: 'Đã thanh toán', color: 'green' },
    REFUNDED: { label: 'Đã hoàn tiền', color: 'default' },
    PARTIALLY_REFUNDED: { label: 'Hoàn 1 phần', color: 'blue' },
  };

  ngOnInit() {
    const role = this.auth.getPrimaryRole();
    this.isHost = role === 'HOST';

    if (this.isHost) {
      this.hotelService.getMyHotels().subscribe({
        next: (data) => {
          this.hotels = data;
          if (data.length > 0) this.selectedHotelId = data[0].id;
          this.loadBookings();
        },
      });
    } else {
      this.loadBookings();
    }
  }

  // ── LOAD (server-side) ──────────────────────────────────
  loadBookings() {
    this.isLoading = true;
    const status = this.filterStatus !== 'all' ? this.filterStatus : undefined;

    if (this.isHost) {
      this.adminBookingService
        .getHostBookings({
          hotelId: this.selectedHotelId || undefined,
          page: this.pageIndex - 1,
          size: this.pageSize,
          status,
        })
        .subscribe({
          next: (data) => {
            this.bookings = data.content;
            this.total = data.totalElements;
            this.pageIndex = data.number + 1;
            this.isLoading = false;
          },
          error: () => {
            this.message.error('Không thể tải bookings');
            this.isLoading = false;
          },
        });
    } else {
      this.adminBookingService
        .getAdminBookings({
          page: this.pageIndex - 1,
          size: this.pageSize,
          status,
        })
        .subscribe({
          next: (data) => {
            this.bookings = data.content;
            this.total = data.totalElements;
            this.pageIndex = data.number + 1;
            this.isLoading = false;
          },
          error: () => {
            this.message.error('Không thể tải bookings');
            this.isLoading = false;
          },
        });
    }
  }

  // ── Stats (tính từ page hiện tại) ───────────────────────
  get totalRevenue(): number {
    return this.bookings
      .filter((b) => b.paymentStatus === 'PAID')
      .reduce((sum, b) => sum + b.finalPrice, 0);
  }

  countByStatus(status: BookingStatus): number {
    return this.bookings.filter((b) => b.status === status).length;
  }

  get uniqueHotels(): Array<{ id: string; name: string }> {
    const map = new Map<string, string>();
    for (const b of this.bookings) {
      if (!map.has(b.hotelId)) map.set(b.hotelId, b.hotelName);
    }
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }

  // ── Paging ──────────────────────────────────────────────
  onPageChange(page: number) {
    this.pageIndex = page;
    this.loadBookings();
  }
  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadBookings();
  }

  // ── Filters ─────────────────────────────────────────────
  onSearch() {
    this.pageIndex = 1;
    this.loadBookings();
  }
  resetFilter() {
    this.filterStatus = 'all';
    this.filterPayment = 'all';
    this.filterHotel = 'all';
    this.keyword = '';
    this.dateRange = null;
    this.pageIndex = 1;
    this.loadBookings();
  }
  onFilterChange() {
    this.pageIndex = 1;
    this.loadBookings();
  }

  // ── Status actions ──────────────────────────────────────
  /** Backend handles workflow — frontend chỉ có force-cancel cho HOST */
  availableNextStatusesForBooking(b: Booking): BookingStatus[] {
    if (this.isHost && (b.status === 'PENDING' || b.status === 'CONFIRMED')) {
      return ['CANCELLED'];
    }
    return [];
  }

  get availableNextStatuses(): BookingStatus[] {
    if (!this.selectedBooking) return [];
    return this.availableNextStatusesForBooking(this.selectedBooking);
  }

  openStatusChangeModal(b: Booking) {
    // HOST chỉ có thể cancel → mở cancel modal
    this.openCancelModal(b);
  }

  // ── Cancel (HOST force-cancel) ──────────────────────────
  openCancelModal(booking: Booking) {
    this.selectedBooking = booking;
    this.cancelForm2.reset({ reason: '' });
    this.isCancelModalOpen = true;
  }

  submitCancel() {
    if (this.cancelForm2.invalid || !this.selectedBooking) return;
    this.isSubmitting = true;
    const reason = this.cancelForm2.value.reason || '';

    this.adminBookingService.forceCancel(this.selectedBooking.id, reason).subscribe({
      next: () => {
        this.message.success(`Đã hủy booking ${this.selectedBooking!.id}`);
        this.isCancelModalOpen = false;
        this.isSubmitting = false;
        this.loadBookings();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể hủy');
        this.isSubmitting = false;
      },
    });
  }

  // ── Pay / Refund (giữ modal nhưng chưa có API) ─────────
  openPayModal(b: Booking) {
    this.selectedBooking = b;
    this.isPayModalOpen = true;
  }
  confirmPay() {
    this.message.info('Chức năng đánh dấu thanh toán sẽ được thêm sau');
    this.isPayModalOpen = false;
  }

  openRefundModal(b: Booking) {
    this.selectedBooking = b;
    this.refundForm.reset({ reason: '' });
    this.isRefundModalOpen = true;
  }
  submitRefund() {
    this.message.info('Chức năng hoàn tiền sẽ được thêm sau');
    this.isRefundModalOpen = false;
  }

  submitStatusChange() {
    // Redirect to cancel
    if (this.selectedBooking) this.openCancelModal(this.selectedBooking);
    this.isStatusModalOpen = false;
  }

  // ── Navigate ────────────────────────────────────────────
  viewDetail(b: Booking) {
    this.router.navigate(['/admin/bookings', b.id]);
  }

  // ── Helpers ─────────────────────────────────────────────
  isAdmin(): boolean {
    return this.auth.getPrimaryRole() === 'ADMIN';
  }

  formatVND(n: number): string {
    return new Intl.NumberFormat('vi-VN').format(n) + ' đ';
  }
  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('vi-VN');
  }
  formatDateTime(iso: string): string {
    return new Date(iso).toLocaleString('vi-VN');
  }

  onExportExcel() {
    const from = this.dateRange?.[0]
      ? this.dateRange[0].toISOString().split('T')[0]
      : new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const to = this.dateRange?.[1]
      ? this.dateRange[1].toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0];

    this.adminBookingService.exportBookings({
      format: 'XLSX',
      from,
      to,
      status: this.filterStatus !== 'all' ? this.filterStatus : undefined,
    });
    this.message.success('Đang tải file Excel...');
  }

  onExportRevenuePdf() {
    const from = this.dateRange?.[0]
      ? this.dateRange[0].toISOString().split('T')[0]
      : new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const to = this.dateRange?.[1]
      ? this.dateRange[1].toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0];

    this.adminBookingService.exportRevenue({ from, to, format: 'PDF' });
    this.message.success('Đang tải Revenue PDF...');
  }

  onExportRevenueExcel() {
    const from = this.dateRange?.[0]
      ? this.dateRange[0].toISOString().split('T')[0]
      : new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];
    const to = this.dateRange?.[1]
      ? this.dateRange[1].toISOString().split('T')[0]
      : new Date().toISOString().split('T')[0];

    this.adminBookingService.exportRevenue({ from, to, format: 'XLSX' });
    this.message.success('Đang tải Revenue Excel...');
  }

  onConfirmBooking(b: Booking) {
    this.adminBookingService.confirmBooking(b.id).subscribe({
      next: () => {
        this.message.success(`Booking ${b.id} đã xác nhận`);
        this.loadBookings();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể xác nhận');
      },
    });
  }

  onDownloadPdf(b: Booking) {
    this.adminBookingService.downloadConfirmation(b.id);
    this.message.success('Đang tải PDF...');
  }

  onDownloadReceipt(b: Booking) {
    this.adminBookingService.downloadReceipt(b.id);
    this.message.success('Đang tải Receipt...');
  }
}
