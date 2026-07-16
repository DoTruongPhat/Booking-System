// ═══════════════════════════════════════════════════════════
// ADMIN BOOKING SERVICE — Phase E (API Integration)
// BỎ MockCacheService → gọi API thật
//
// Admin: GET /api/admin/bookings, GET /api/admin/bookings/{id}
// Host:  GET /api/host/bookings?hotelId=
//        POST /api/host/bookings/{id}/force-cancel
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Booking, BookingStatus } from '../models/booking.model';
import { ApiResponse, PaginatedData } from '../models/api-response.model';

/** Request body cho POST /api/host/bookings/{id}/force-cancel */
export interface ForceCancelRequest {
  reason: string; // bắt buộc
}

@Injectable({ providedIn: 'root' })
export class AdminBookingService {
  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════════════════════
  // ADMIN ENDPOINTS (role ADMIN)
  // ══════════════════════════════════════════════════════════

  /**
   * GET /api/admin/bookings — list tất cả bookings (admin)
   * Hỗ trợ filter status + pagination
   */
  getAdminBookings(
    params: {
      status?: BookingStatus;
      page?: number;
      size?: number;
    } = {},
  ): Observable<PaginatedData<Booking>> {
    let httpParams = new HttpParams();
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<Booking>>>('/api/admin/bookings', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  /** GET /api/admin/bookings/{id} — chi tiết booking (admin) */
  getAdminBookingById(id: string): Observable<Booking> {
    return this.http
      .get<ApiResponse<Booking>>(`/api/admin/bookings/${id}`, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  // ══════════════════════════════════════════════════════════
  // HOST ENDPOINTS (role HOST)
  // ══════════════════════════════════════════════════════════

  /**
   * GET /api/host/bookings — list bookings của hotels mình
   * Filter theo hotelId (optional — nếu host có nhiều hotel)
   */
  getHostBookings(
    params: {
      hotelId?: string;
      page?: number;
      size?: number;
      status?: BookingStatus;
    } = {},
  ): Observable<PaginatedData<Booking>> {
    let httpParams = new HttpParams();
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<Booking>>>('/api/host/bookings', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  /**
   * POST /api/host/bookings/{id}/force-cancel
   * Host hủy booking (bắt buộc có reason)
   */
  forceCancel(bookingId: string, reason: string): Observable<Booking> {
    const body: ForceCancelRequest = { reason };
    return this.http
      .post<ApiResponse<Booking>>(`/api/host/bookings/${bookingId}/force-cancel`, body, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  // ══════════════════════════════════════════════════════════
  // REPORT & EXPORT ENDPOINTS
  // ══════════════════════════════════════════════════════════

  /** POST /api/admin/bookings/{id}/confirm — Admin confirm booking */
  confirmBooking(bookingId: string): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(
        `/api/admin/bookings/${bookingId}/confirm`,
        {},
        {
          withCredentials: true,
        },
      )
      .pipe(map((res) => res.data));
  }

  /** GET /api/admin/bookings/export — Export bookings Excel/CSV/PDF */
  exportBookings(params: {
    format?: string;
    from?: string;
    to?: string;
    hotelId?: string;
    status?: string;
  }): void {
    let httpParams = new HttpParams();
    if (params.format) httpParams = httpParams.set('format', params.format);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);
    if (params.status) httpParams = httpParams.set('status', params.status);

    this.http
      .get('/api/admin/bookings/export', {
        params: httpParams,
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) =>
          this.downloadBlob(blob, `bookings-export.${(params.format || 'xlsx').toLowerCase()}`),
        error: () => console.error('Export failed'),
      });
  }

  /** GET /api/admin/reports/revenue — Revenue report PDF/XLSX */
  exportRevenue(params: { from: string; to: string; format?: string }): void {
    let httpParams = new HttpParams().set('from', params.from).set('to', params.to);
    if (params.format) httpParams = httpParams.set('format', params.format);

    this.http
      .get('/api/admin/reports/revenue', {
        params: httpParams,
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => {
          const ext = (params.format || 'pdf').toLowerCase();
          this.downloadBlob(blob, `revenue-report.${ext}`);
        },
        error: () => console.error('Revenue export failed'),
      });
  }

  /** Download single booking confirmation PDF */
  downloadConfirmation(bookingId: string): void {
    this.http
      .get(`/api/user/bookings/${bookingId}/confirmation.pdf`, {
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, `booking-confirmation.pdf`),
        error: () => console.error('Download failed'),
      });
  }

  /** Download payment receipt PDF */
  downloadReceipt(bookingId: string): void {
    this.http
      .get(`/api/user/bookings/${bookingId}/receipt.pdf`, {
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, `payment-receipt.pdf`),
        error: () => console.error('Download failed'),
      });
  }

  /** Helper: trigger browser download */
  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
