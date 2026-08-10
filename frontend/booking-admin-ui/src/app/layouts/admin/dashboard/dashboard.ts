import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';

import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';

import { Booking, BookingStatus } from '../../../core/models/booking.model';
import { AdminBookingService } from '../../../core/services/admin-booking.service';
import { Auth } from '../../../core/services/auth';
import { Dashboard, DashboardStats } from '../../../core/services/dashboard';

interface MetricCard {
  title: string;
  value: string;
  trend: string;
  trendType: 'up' | 'down';
}

interface MonthPoint {
  label: string;
  value: number;
  x: number;
  y: number;
}

interface HeatCell {
  day: string;
  time: string;
  value: number;
  level: number;
}

interface CalendarDay {
  date: string;
  label: string;
  count: number;
  level: number;
  isToday: boolean;
}

interface ProgressItem {
  label: string;
  value: number;
  percent: number;
  color: 'pending' | 'done' | 'finish';
}

interface GaugeItem {
  label: string;
  value: number;
  icon: string;
  color: 'rose' | 'cyan';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, NzAlertModule, NzButtonModule, NzIconModule, NzSkeletonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent implements OnInit {
  private auth = inject(Auth);
  private dashboardService = inject(Dashboard);
  private adminBookingService = inject(AdminBookingService);

  currentUser = this.auth.getUser();
  role = this.auth.getPrimaryRole();
  stats: DashboardStats | null = null;
  bookings: Booking[] = [];
  bookingTotalElements = 0;
  loading = true;
  bookingsLoading = true;
  loadError = false;
  errorMessage = '';

  readonly months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  readonly heatDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  readonly heatTimes = ['1:00 PM', '2:00 PM', '3:00 PM', '4:00 PM', '5:00 PM', '6:00 PM', '7:00 PM'];

  get isHost(): boolean {
    return this.role === 'HOST';
  }

  get metricCards(): MetricCard[] {
    return [
      {
        title: 'Total Revenue',
        value: this.formatCompactCurrency(this.dashboardRevenue),
        trend: '+20.8%',
        trendType: 'up',
      },
      {
        title: 'Total Booking',
        value: this.formatNumber(this.dashboardTotalBookings),
        trend: this.pendingRate ? `-${this.pendingRate}%` : '0%',
        trendType: this.pendingRate > 25 ? 'down' : 'up',
      },
      {
        title: this.isHost ? 'Properties' : 'New Customers',
        value: this.formatNumber(this.primaryEntityValue),
        trend: '+20.8%',
        trendType: 'up',
      },
    ];
  }

  get visitorPoints(): MonthPoint[] {
    const values = this.months.map((_month, index) =>
      this.bookings.filter((booking) => {
        const date = this.bookingDate(booking);
        return date && date.getMonth() === index;
      }).length,
    );

    if (values.every((value) => value === 0) && this.number(this.stats?.totalBookings) > 0) {
      const total = this.number(this.stats?.totalBookings);
      return this.toMonthPoints(this.months.map((_month, index) => Math.round((total / 12) * (0.75 + (index % 4) * 0.18))));
    }

    return this.toMonthPoints(values);
  }

  get visitorLinePath(): string {
    return this.visitorPoints.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');
  }

  get visitorAreaPath(): string {
    const points = this.visitorPoints;
    if (!points.length) return '';
    return `${this.visitorLinePath} L ${points[points.length - 1].x} 180 L ${points[0].x} 180 Z`;
  }

  get highlightedVisitor(): MonthPoint {
    return this.visitorPoints.reduce((best, point) => (point.value > best.value ? point : best), this.visitorPoints[0]);
  }

  get heatCells(): HeatCell[] {
    const counts = new Map<string, number>();
    this.bookings.forEach((booking) => {
      const date = this.bookingDate(booking);
      if (!date) return;
      const dayIndex = (date.getDay() + 6) % 7;
      const hour = date.getHours();
      const timeIndex = this.clamp(hour - 13, 0, this.heatTimes.length - 1);
      const key = `${dayIndex}-${timeIndex}`;
      counts.set(key, (counts.get(key) || 0) + 1);
    });

    const max = Math.max(...Array.from(counts.values()), 1);
    return this.heatTimes.flatMap((time, timeIndex) =>
      this.heatDays.map((day, dayIndex) => {
        const value = counts.get(`${dayIndex}-${timeIndex}`) || 0;
        return {
          day,
          time,
          value,
          level: this.heatLevel(value, max),
        };
      }),
    );
  }

  get calendarWeeks(): CalendarDay[][] {
    const today = new Date();
    const start = this.startOfWeek(today);
    const counts = new Map<string, number>();

    this.bookings.forEach((booking) => {
      const date = this.bookingCheckInDate(booking);
      if (!date) return;
      const key = this.dateKey(date);
      counts.set(key, (counts.get(key) || 0) + 1);
    });

    const days = Array.from({ length: 35 }, (_item, index) => {
      const date = new Date(start);
      date.setDate(start.getDate() + index);
      const key = this.dateKey(date);
      return {
        date: key,
        label: String(date.getDate()),
        count: counts.get(key) || 0,
        level: 0,
        isToday: key === this.dateKey(today),
      };
    });

    const max = Math.max(...days.map((day) => day.count), 1);
    return Array.from({ length: 5 }, (_week, weekIndex) =>
      days.slice(weekIndex * 7, weekIndex * 7 + 7).map((day) => ({
        ...day,
        level: this.heatLevel(day.count, max),
      })),
    );
  }

  get calendarTotal(): number {
    return this.calendarWeeks.flat().reduce((total, day) => total + day.count, 0);
  }

  get progressItems(): ProgressItem[] {
    const pending = this.countStatus('PENDING') || this.number(this.stats?.pendingBookings);
    const done = this.countStatus('CONFIRMED');
    const finish = this.countStatus('COMPLETED');
    const max = Math.max(pending, done, finish, 1);
    return [
      { label: 'Pending', value: pending, percent: this.percent(pending, max), color: 'pending' },
      { label: 'Done', value: done, percent: this.percent(done, max), color: 'done' },
      { label: 'Finish', value: finish, percent: this.percent(finish, max), color: 'finish' },
    ];
  }

  get gauges(): GaugeItem[] {
    const active = Math.max(this.number(this.stats?.activeBookings), 1);
    const checkIn = this.percent(this.number(this.stats?.upcomingCheckIns), active);
    const checkOut = this.percent(this.countStatus('COMPLETED'), Math.max(this.number(this.stats?.totalBookings), 1));
    return [
      { label: 'Check In', value: checkIn, icon: 'login', color: 'rose' },
      { label: 'Check Out', value: checkOut, icon: 'logout', color: 'cyan' },
    ];
  }

  get currentBookings(): Booking[] {
    return [...this.bookings]
      .sort((a, b) => new Date(b.createdAt || b.checkIn || '').getTime() - new Date(a.createdAt || a.checkIn || '').getTime())
      .slice(0, 6);
  }

  get primaryEntityValue(): number {
    return this.isHost
      ? this.number(this.stats?.totalHotels ?? this.stats?.activeHotels)
      : this.number(this.stats?.totalUsers);
  }

  get dashboardTotalBookings(): number {
    return this.bookingTotalElements || this.bookings.length || this.number(this.stats?.totalBookings);
  }

  get dashboardRevenue(): number {
    const bookingRevenue = this.bookings
      .filter((booking) => booking.paymentStatus === 'PAID' && booking.status !== 'CANCELLED')
      .reduce((total, booking) => total + this.number(booking.finalPrice), 0);
    return bookingRevenue || this.number(this.stats?.totalRevenue);
  }

  get pendingRate(): number {
    const pendingBookings = this.countStatus('PENDING') || this.number(this.stats?.pendingBookings);
    return this.percent(pendingBookings, Math.max(this.dashboardTotalBookings, 1));
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadBookings();
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
        this.errorMessage = err?.error?.message || 'Khong tai duoc du lieu dashboard';
      },
    });
  }

  loadBookings(): void {
    this.bookingsLoading = true;
    const request =
      this.role === 'HOST'
        ? this.adminBookingService.getHostBookings({ page: 0, size: 200 })
        : this.adminBookingService.getAdminBookings({ page: 0, size: 200 });

    request.subscribe({
      next: (page) => {
        this.bookings = page.content || [];
        this.bookingTotalElements = Number(page.totalElements ?? this.bookings.length);
        this.bookingsLoading = false;
      },
      error: () => {
        this.bookings = [];
        this.bookingTotalElements = 0;
        this.bookingsLoading = false;
      },
    });
  }

  reload(): void {
    this.loadStats();
    this.loadBookings();
  }

  gaugeStyle(item: GaugeItem): Record<string, string> {
    const color = item.color === 'rose' ? '#f45f7c' : '#42b9df';
    return {
      background: `conic-gradient(${color} 0 ${item.value}%, #eef0ef ${item.value}% 100%)`,
    };
  }

  getHeatCell(rowIndex: number, dayIndex: number): HeatCell {
    return this.heatCells[rowIndex * this.heatDays.length + dayIndex];
  }

  trackByTitle(_index: number, item: MetricCard): string {
    return item.title;
  }

  trackByLabel(_index: number, item: { label?: string; day?: string; time?: string }): string {
    return item.label || `${item.day}-${item.time}` || String(_index);
  }

  trackByDate(_index: number, item: CalendarDay): string {
    return item.date;
  }

  formatPrice(value?: number | null): string {
    return this.formatCompactCurrency(value);
  }

  formatDate(value?: string | null): string {
    if (!value) return '-';
    return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
  }

  private toMonthPoints(values: number[]): MonthPoint[] {
    const max = Math.max(...values, 1);
    return values.map((value, index) => ({
      label: this.months[index],
      value,
      x: 20 + index * (520 / 11),
      y: 170 - (value / max) * 125,
    }));
  }

  private countStatus(status: BookingStatus): number {
    return this.bookings.filter((booking) => booking.status === status).length;
  }

  private bookingDate(booking: Booking): Date | null {
    const raw = booking.createdAt || booking.checkIn;
    return this.parseDate(raw);
  }

  private bookingCheckInDate(booking: Booking): Date | null {
    const raw = booking.checkIn || booking.createdAt;
    return this.parseDate(raw);
  }

  private parseDate(raw?: string | null): Date | null {
    if (!raw) return null;
    const date = new Date(raw);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private startOfWeek(date: Date): Date {
    const start = new Date(date);
    const dayIndex = (start.getDay() + 6) % 7;
    start.setDate(start.getDate() - dayIndex);
    start.setHours(0, 0, 0, 0);
    return start;
  }

  private dateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private heatLevel(count: number, max: number): number {
    if (!count) return 0;
    return this.clamp(Math.ceil((count / max) * 3), 1, 3);
  }

  private number(value: number | null | undefined): number {
    return Number(value ?? 0);
  }

  private percent(value: number | null | undefined, total: number | null | undefined): number {
    const denominator = this.number(total);
    if (!denominator) return 0;
    return this.clamp(Math.round((this.number(value) / denominator) * 100), 0, 100);
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }

  private formatNumber(value: number | null | undefined): string {
    return new Intl.NumberFormat('vi-VN').format(this.number(value));
  }

  private formatCompactCurrency(value: number | null | undefined): string {
    const amount = this.number(value);
    if (amount >= 1000000) return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 }).format(amount / 1000000)}M đ`;
    if (amount >= 1000) return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(amount / 1000)}K đ`;
    return `${new Intl.NumberFormat('vi-VN').format(amount)} đ`;
  }
}
