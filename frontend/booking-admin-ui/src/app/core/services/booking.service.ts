// ═══════════════════════════════════════════════════════════
// BOOKING SERVICE — Phase E (API Integration)
// BỎ localStorage mock → gọi /api/user/bookings thật
//
// POST /api/user/bookings         — tạo booking
// GET  /api/user/bookings         — list bookings của user (pagination)
// GET  /api/user/bookings/{id}    — chi tiết booking
// POST /api/user/bookings/{id}/cancel — hủy booking
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import {
  Booking,
  BookingStatus,
  CreateBookingRequest,
  CancelBookingRequest,
} from '../models/booking.model';
import { ApiResponse, PaginatedData } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly baseUrl = '/api/user/bookings';

  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════════════════════
  // CREATE
  // ══════════════════════════════════════════════════════════

  /**
   * POST /api/user/bookings
   * Tạo booking mới. Backend tự tính price, tax, finalPrice.
   * Trả về Booking object đầy đủ (có id, status=PENDING, v.v.)
   */
  createBooking(request: CreateBookingRequest): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(this.baseUrl, request, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  // ══════════════════════════════════════════════════════════
  // READ
  // ══════════════════════════════════════════════════════════

  /**
   * GET /api/user/bookings — list bookings của user hiện tại
   * Hỗ trợ pagination + filter status (optional)
   */
  getMyBookings(
    params: {
      page?: number;
      size?: number;
      status?: BookingStatus;
    } = {},
  ): Observable<PaginatedData<Booking>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.status) httpParams = httpParams.set('status', params.status);

    return this.http
      .get<ApiResponse<PaginatedData<Booking>>>(this.baseUrl, {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  /**
   * GET /api/user/bookings/{id} — chi tiết 1 booking
   */
  getBookingById(id: string): Observable<Booking> {
    return this.http
      .get<ApiResponse<Booking>>(`${this.baseUrl}/${id}`, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  // ══════════════════════════════════════════════════════════
  // CANCEL
  // ══════════════════════════════════════════════════════════

  /**
   * POST /api/user/bookings/{id}/cancel
   * Hủy booking. Backend tự tính refund theo policy:
   *   ≥48h trước checkIn → 100% refund
   *   24-48h → 50%
   *   <24h → 0%
   *
   * Body: { reason: string }
   */
  cancelBooking(id: string, reason: string): Observable<Booking> {
    const body: CancelBookingRequest = { reason };
    return this.http
      .post<ApiResponse<Booking>>(`${this.baseUrl}/${id}/cancel`, body, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  // ══════════════════════════════════════════════════════════
  // HELPERS (pure frontend, không gọi API)
  // ══════════════════════════════════════════════════════════

  /** Tính số đêm giữa 2 ngày */
  calculateNights(checkIn: string, checkOut: string): number {
    const d1 = new Date(checkIn);
    const d2 = new Date(checkOut);
    const diff = d2.getTime() - d1.getTime();
    return Math.max(1, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  }

  /**
   * Tính thời gian còn lại trước khi PENDING booking hết hạn (15 phút).
   * Trả về milliseconds còn lại, hoặc 0 nếu đã hết.
   */
  getPendingTimeRemaining(createdAt: string): number {
    const created = new Date(createdAt).getTime();
    const expiresAt = created + 15 * 60 * 1000; // 15 minutes
    const remaining = expiresAt - Date.now();
    return Math.max(0, remaining);
  }

  /**
   * Tính phần trăm refund dự kiến khi user muốn cancel.
   * Chỉ để hiển thị warning — backend sẽ tính chính xác.
   */
  estimateRefundPercent(checkIn: string): number {
    const now = Date.now();
    const checkInTime = new Date(checkIn).getTime();
    const hoursUntilCheckIn = (checkInTime - now) / (1000 * 60 * 60);

    if (hoursUntilCheckIn >= 48) return 100;
    if (hoursUntilCheckIn >= 24) return 50;
    return 0;
  }
}
