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
import { Booking, BookingStatus, PaymentStatus } from '../models/booking.model';
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
      .pipe(map((res) => this.normalizePage(res.data)));
  }

  /** GET /api/admin/bookings/{id} — chi tiết booking (admin) */
  getAdminBookingById(id: string): Observable<Booking> {
    return this.http
      .get<ApiResponse<Booking>>(`/api/admin/bookings/${id}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeBooking(res.data)));
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
      .pipe(map((res) => this.normalizePage(res.data)));
  }

  /** GET /api/host/bookings/{id} — chi tiết booking thuộc hotel của host */
  getHostBookingById(id: string): Observable<Booking> {
    return this.http
      .get<ApiResponse<Booking>>(`/api/host/bookings/${id}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeBooking(res.data)));
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
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

  // ══════════════════════════════════════════════════════════
  checkIn(bookingId: string): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(`/api/host/bookings/${bookingId}/check-in`, {}, {
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

  checkOut(bookingId: string): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(`/api/host/bookings/${bookingId}/check-out`, {}, {
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

  markNoShow(bookingId: string, reason?: string): Observable<Booking> {
    return this.http
      .post<ApiResponse<Booking>>(`/api/host/bookings/${bookingId}/no-show`, { reason }, {
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

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
      .pipe(map((res) => this.normalizeBooking(res.data)));
  }

  /** GET /api/admin/bookings/export hoặc /api/host/bookings/export */
  exportBookings(params: {
    format?: string;
    from?: string;
    to?: string;
    hotelId?: string;
    status?: string;
    scope?: 'admin' | 'host';
  }): void {
    let httpParams = new HttpParams();
    if (params.format) httpParams = httpParams.set('format', params.format);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);
    if (params.status) httpParams = httpParams.set('status', params.status);

    const endpoint =
      params.scope === 'host' ? '/api/host/bookings/export' : '/api/admin/bookings/export';

    this.http
      .get(endpoint, {
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

  /** GET /api/admin/reports/revenue hoặc /api/host/reports/revenue */
  exportRevenue(params: {
    from: string;
    to: string;
    format?: string;
    scope?: 'admin' | 'host';
    hotelId?: string;
  }): void {
    let httpParams = new HttpParams().set('from', params.from).set('to', params.to);
    if (params.format) httpParams = httpParams.set('format', params.format);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);

    const endpoint =
      params.scope === 'host' ? '/api/host/reports/revenue' : '/api/admin/reports/revenue';

    this.http
      .get(endpoint, {
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

  /** GET /api/admin/reports/monthly-revenue hoặc /api/host/reports/monthly-revenue */
  exportMonthlyRevenue(params: {
    year: number;
    month: number;
    format?: string;
    scope?: 'admin' | 'host';
    hotelId?: string;
  }): void {
    let httpParams = new HttpParams()
      .set('year', params.year)
      .set('month', params.month);
    if (params.format) httpParams = httpParams.set('format', params.format);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);

    const endpoint =
      params.scope === 'host'
        ? '/api/host/reports/monthly-revenue'
        : '/api/admin/reports/monthly-revenue';

    this.http
      .get(endpoint, {
        params: httpParams,
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => {
          const ext = (params.format || 'pdf').toLowerCase();
          this.downloadBlob(blob, `monthly-revenue-report.${ext}`);
        },
        error: () => console.error('Monthly revenue export failed'),
      });
  }

  /** GET /api/admin/reports/commission hoặc /api/host/reports/commission */
  exportCommission(params: {
    from: string;
    to: string;
    format?: string;
    scope?: 'admin' | 'host';
    hotelId?: string;
    commissionRate?: number;
  }): void {
    let httpParams = new HttpParams().set('from', params.from).set('to', params.to);
    if (params.format) httpParams = httpParams.set('format', params.format);
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);
    if (params.commissionRate !== undefined) {
      httpParams = httpParams.set('commissionRate', params.commissionRate);
    }

    const endpoint =
      params.scope === 'host' ? '/api/host/reports/commission' : '/api/admin/reports/commission';

    this.http
      .get(endpoint, {
        params: httpParams,
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => {
          const ext = (params.format || 'pdf').toLowerCase();
          this.downloadBlob(blob, `commission-report.${ext}`);
        },
        error: () => console.error('Commission export failed'),
      });
  }

  /** Download single booking confirmation PDF */
  downloadConfirmation(bookingId: string, scope: 'user' | 'host' = 'user'): void {
    const endpoint =
      scope === 'host'
        ? `/api/host/bookings/${bookingId}/confirmation.pdf`
        : `/api/user/bookings/${bookingId}/confirmation.pdf`;

    this.http
      .get(endpoint, {
        responseType: 'blob',
        withCredentials: true,
      })
      .subscribe({
        next: (blob) => this.downloadBlob(blob, `booking-confirmation.pdf`),
        error: () => console.error('Download failed'),
      });
  }

  /** Download payment receipt PDF */
  downloadReceipt(bookingId: string, scope: 'user' | 'host' = 'user'): void {
    const endpoint =
      scope === 'host'
        ? `/api/host/bookings/${bookingId}/receipt.pdf`
        : `/api/user/bookings/${bookingId}/receipt.pdf`;

    this.http
      .get(endpoint, {
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

  private normalizePage(data: PaginatedData<Booking> | null | undefined): PaginatedData<Booking> {
    const page = data || {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 10,
    };

    return {
      ...page,
      content: Array.isArray(page.content)
        ? page.content.map((booking) => this.normalizeBooking(booking))
        : [],
      totalElements: Number(page.totalElements ?? 0),
      totalPages: Number(page.totalPages ?? 0),
      number: Number(page.number ?? 0),
      size: Number(page.size ?? 10),
    };
  }

  private normalizeBooking(raw: Booking | any): Booking {
    const numGuests = this.toNumber(raw?.numGuests ?? raw?.guests?.adults, 1);
    const numRooms = this.toNumber(raw?.numRooms ?? raw?.rooms, 1);
    const unitPrice = this.toNumber(raw?.unitPrice ?? raw?.pricePerNight, 0);
    const totalPrice = this.toNumber(raw?.totalPrice ?? raw?.finalPrice, 0);
    const status = this.normalizeStatus(raw?.status);
    const paymentStatus = this.normalizePaymentStatus(raw?.paymentStatus);
    const roomImages = Array.isArray(raw?.roomImages)
      ? raw.roomImages
      : Array.isArray(raw?.room?.images)
        ? raw.room.images
        : [];
    const hotelImages = Array.isArray(raw?.hotelImages)
      ? raw.hotelImages
      : Array.isArray(raw?.hotel?.images)
        ? raw.hotel.images
        : [];
    const imageUrl =
      raw?.imageUrl ??
      raw?.thumbnail ??
      roomImages[0] ??
      hotelImages[0] ??
      '/images/Logo.jpg';

    const guestInfo = {
      fullName:
        raw?.guestInfo?.fullName ||
        raw?.guestName ||
        raw?.customerName ||
        raw?.bookingCode ||
        'Khach hang',
      email: raw?.guestInfo?.email || raw?.guestEmail || raw?.customerEmail || '',
      phone: raw?.guestInfo?.phone || raw?.guestPhone || raw?.customerPhone || '',
      countryCode: raw?.guestInfo?.countryCode,
      estimatedArrivalTime: raw?.guestInfo?.estimatedArrivalTime,
    };

    return {
      ...raw,
      id: String(raw?.id ?? ''),
      userId: String(raw?.userId ?? ''),
      hotelId: String(raw?.hotelId ?? raw?.hotel?.id ?? ''),
      roomId: String(raw?.roomId ?? raw?.room?.id ?? ''),
      hotelName: raw?.hotelName || raw?.hotel?.name || raw?.hotelId || 'Khach san',
      hotelAddress: raw?.hotelAddress || raw?.hotel?.address || '',
      roomName: raw?.roomName || raw?.room?.name || raw?.roomId || 'Phong',
      roomImages,
      hotelImages,
      imageUrl,
      checkIn: raw?.checkIn || raw?.checkInDate || '',
      checkOut: raw?.checkOut || raw?.checkOutDate || '',
      nights: this.toNumber(raw?.nights ?? raw?.numNights, 0),
      guests: {
        adults: this.toNumber(raw?.guests?.adults, numGuests),
        children: this.toNumber(raw?.guests?.children, 0),
        childrenAges: raw?.guests?.childrenAges,
      },
      rooms: numRooms,
      pricePerNight: unitPrice,
      totalPrice,
      taxAmount: this.toNumber(raw?.taxAmount, 0),
      finalPrice: this.toNumber(raw?.finalPrice, totalPrice),
      status,
      paymentStatus,
      paymentMethod: raw?.paymentMethod,
      guestInfo,
      specialRequests: raw?.specialRequests ?? raw?.specialRequest,
      createdAt: raw?.createdAt || '',
      updatedAt: raw?.updatedAt || '',
    };
  }

  private normalizeStatus(value: unknown): BookingStatus {
    const status = String(value || 'PENDING') as BookingStatus;
    return ['PENDING', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(status)
      ? status
      : 'PENDING';
  }

  private normalizePaymentStatus(value: unknown): PaymentStatus {
    const paymentStatus = String(value || 'UNPAID') as PaymentStatus;
    return ['UNPAID', 'PAID', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(paymentStatus)
      ? paymentStatus
      : 'UNPAID';
  }

  private toNumber(value: unknown, fallback: number): number {
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
  }
}
