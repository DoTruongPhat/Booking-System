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
  VoucherValidation,
} from '../models/booking.model';
import { ApiResponse, PaginatedData } from '../models/api-response.model';
import { withIdempotencyHeader } from '../http/idempotency';
import { environment } from '../../../environments/environment';

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
  createBooking(request: CreateBookingRequest, idempotencyKey?: string): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(
        this.baseUrl,
        request,
        withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      )
      .pipe(map((res) => this.normalizeBooking(res.data)));
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
      .pipe(map((res) => this.normalizePage(res.data)));
  }

  /**
   * GET /api/user/bookings/{id} — chi tiết 1 booking
   */
  getBookingById(id: string): Observable<Booking> {
    return this.http
      .get<ApiResponse<Booking>>(`${this.baseUrl}/${id}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

  validateVoucher(params: {
    code: string;
    hotelId?: string;
    amount: number;
  }): Observable<VoucherValidation> {
    let httpParams = new HttpParams()
      .set('code', params.code.trim())
      .set('amount', params.amount);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);

    return this.http
      .get<ApiResponse<VoucherValidation>>('/api/vouchers/validate', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeVoucherValidation(res.data)));
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
  cancelBooking(id: string, reason: string, idempotencyKey?: string): Observable<Booking> {
    const body: CancelBookingRequest = { reason };
    return this.http
      .post<ApiResponse<Booking>>(`${this.baseUrl}/${id}/cancel`, body, {
        ...withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
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
   * Tính thời gian còn lại trước khi PENDING booking hết hạn theo cấu hình.
   * Trả về milliseconds còn lại, hoặc 0 nếu đã hết.
   */
  getPendingTimeRemaining(createdAt: string, paymentExpiresAt?: string | null): number {
    const expiresAt = paymentExpiresAt
      ? new Date(paymentExpiresAt).getTime()
      : new Date(createdAt).getTime() + environment.pendingPaymentMinutes * 60 * 1000;
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

  private normalizePage(page: PaginatedData<Booking> | null | undefined): PaginatedData<Booking> {
    const safePage = page ?? ({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
    } as PaginatedData<Booking>);

    return {
      ...safePage,
      content: (safePage.content || []).map((booking) => this.normalizeBooking(booking)),
    };
  }

  private normalizeBooking(raw: Booking | null | undefined): Booking {
    if (!raw) {
      return null as unknown as Booking;
    }

    const data = raw as any;
    const totalPrice = Number(data.totalPrice ?? data.finalPrice ?? 0);
    const taxAmount = Number(data.taxAmount ?? 0);
    const finalPrice = Number(data.finalPrice ?? totalPrice);
    const discountAmount = Number(data.discountAmount ?? 0);
    const numGuests = Number(data.numGuests ?? data.guests?.adults ?? 1);
    const roomImages = Array.isArray(data.roomImages)
      ? data.roomImages
      : Array.isArray(data.room?.images)
        ? data.room.images
        : [];
    const hotelImages = Array.isArray(data.hotelImages)
      ? data.hotelImages
      : Array.isArray(data.hotel?.images)
        ? data.hotel.images
        : [];
    const imageUrl =
      data.imageUrl ??
      data.thumbnail ??
      roomImages[0] ??
      hotelImages[0] ??
      '/images/Logo.jpg';

    return {
      ...raw,
      checkIn: data.checkIn ?? data.checkInDate ?? '',
      checkOut: data.checkOut ?? data.checkOutDate ?? '',
      nights: Number(data.nights ?? data.numNights ?? 0),
      rooms: Number(data.rooms ?? data.numRooms ?? 1),
      pricePerNight: Number(data.pricePerNight ?? data.unitPrice ?? 0),
      discountAmount,
      voucherCode: data.voucherCode,
      totalPrice,
      taxAmount,
      finalPrice,
      paymentExpiresAt: data.paymentExpiresAt ?? null,
      guests: data.guests ?? {
        adults: numGuests,
        children: 0,
      },
      guestInfo: data.guestInfo ?? {
        fullName: data.guestName ?? 'Khách hàng',
        email: data.guestEmail ?? '',
        phone: data.guestPhone ?? '',
      },
      hotelName: data.hotelName ?? '',
      hotelAddress: data.hotelAddress ?? '',
      roomName: data.roomName ?? '',
      roomImages,
      hotelImages,
      imageUrl,
    };
  }

  private normalizeVoucherValidation(raw: VoucherValidation): VoucherValidation {
    const data = raw as any;
    return {
      ...raw,
      valid: Boolean(data.valid),
      message: data.message ?? '',
      voucherId: data.voucherId,
      code: data.code ?? data.voucherCode,
      discountType: data.discountType,
      discountValue: Number(data.discountValue ?? 0),
      discountAmount: Number(data.discountAmount ?? 0),
    };
  }
}
