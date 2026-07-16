// ═══════════════════════════════════════════════════════════
// ADMIN BOOKING DETAIL — Phase E (API Integration)
// GET /api/admin/bookings/{id}
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzMessageService } from 'ng-zorro-antd/message';

import { AdminBookingService } from '../../../core/services/admin-booking.service';
import { Booking, BookingStatus, PaymentStatus } from '../../../core/models/booking.model';
import { AdminBooking } from '../../../core/models/admin-booking.model';

@Component({
  selector: 'app-admin-booking-detail',
  imports: [
    CommonModule,
    NzCardModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzDescriptionsModule,
    NzDividerModule,
    NzTimelineModule,
    NzAvatarModule,
    NzSpinModule,
    NzEmptyModule,
    NzAlertModule,
  ],
  templateUrl: './booking-detail.html',
  styleUrl: './booking-detail.scss',
})
export class AdminBookingDetail implements OnInit {
  private adminBookingService = inject(AdminBookingService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private message = inject(NzMessageService);

  booking: AdminBooking | null = null;
  isLoading = false;

  // Config aligned with backend 5 statuses
  readonly statusConfig: Record<string, { label: string; color: string }> = {
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
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadBooking(id);
  }

  loadBooking(id: string) {
    this.isLoading = true;
    this.adminBookingService.getAdminBookingById(id).subscribe({
      next: (data) => {
        // Cast to AdminBooking (backend may return extra fields)
        this.booking = data as AdminBooking;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.message.error('Không tìm thấy booking');
        this.router.navigate(['/admin/bookings']);
      },
    });
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

  goBack() {
    this.router.navigate(['/admin/bookings']);
  }

  get sortedTimeline() {
    return [...(this.booking?.timeline ?? [])].sort(
      (a, b) => new Date(b.changedAt).getTime() - new Date(a.changedAt).getTime(),
    );
  }
}
