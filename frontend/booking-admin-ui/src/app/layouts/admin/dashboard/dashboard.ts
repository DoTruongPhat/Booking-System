// ═══════════════════════════════════════════════════════════
// DASHBOARD COMPONENT — /admin/dashboard
// Khớp với HTML template hiện có (stats cards + grid layout)
// Gọi GET /api/admin/dashboard → stats
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { NzCardModule } from 'ng-zorro-antd/card';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';

import { Auth } from '../../../core/services/auth';
import { Dashboard, DashboardStats } from '../../../core/services/dashboard';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    NzCardModule,
    NzGridModule,
    NzStatisticModule,
    NzSkeletonModule,
    NzAlertModule,
    NzButtonModule,
    NzIconModule,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  private auth = inject(Auth);
  private dashboardService = inject(Dashboard);

  // Template bindings
  currentUser = this.auth.getUser();
  role = this.auth.getPrimaryRole();
  stats: DashboardStats | null = null;
  loading = true;
  loadError = false;
  errorMessage = '';

  get isHost(): boolean {
    return this.role === 'HOST';
  }

  get firstCardTitle(): string {
    return this.isHost ? 'Khach san cua toi' : 'Total Users';
  }

  get bookingCardTitle(): string {
    return this.isHost ? 'Bookings cua toi' : 'Total Bookings';
  }

  get roomCardTitle(): string {
    return this.isHost ? 'Tong so phong' : 'Total Rooms';
  }

  get revenueCardTitle(): string {
    return this.isHost ? 'Doanh thu da thanh toan' : 'Revenue (VND)';
  }

  get dashboardSubtitle(): string {
    return this.isHost
      ? 'Tong quan khach san, phong va booking cua ban'
      : 'Welcome back, ' + (this.currentUser?.username || '');
  }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.loadError = false;
    this.errorMessage = '';

    this.dashboardService.getStats(this.role).subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.loadError = true;
        this.errorMessage = err?.error?.message || 'Không tải được dữ liệu dashboard';
      },
    });
  }

  reload(): void {
    this.loadStats();
  }
}
