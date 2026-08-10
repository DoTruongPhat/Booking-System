// ═══════════════════════════════════════════════════════════
// HOTEL SERVICE — Phase E (API Integration)
// Thay mock data bằng HttpClient gọi core-service
//
// Public:  GET /api/hotels/{hotelId}
// Host:   POST/PUT/GET /api/host/hotels, GET /api/host/hotels/{id}
// Admin:  GET /api/admin/hotels, POST /api/admin/hotels/{id}/approve
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Hotel } from '../models/hotel.model';
import { ApiResponse, PaginatedData } from '../models/api-response.model';
import { withIdempotencyHeader } from '../http/idempotency';

/** Status của hotel trên admin (dùng cho approve flow) */
export type HotelStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'INACTIVE';

@Injectable({ providedIn: 'root' })
export class HotelService {
  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════════════════════
  // PUBLIC ENDPOINTS
  // ══════════════════════════════════════════════════════════

  /** GET /api/hotels/{hotelId} — chi tiết hotel (public) */
  getById(hotelId: string): Observable<Hotel> {
    return this.http
      .get<ApiResponse<Hotel>>(`/api/hotels/${hotelId}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  // ══════════════════════════════════════════════════════════
  // HOST ENDPOINTS (role HOST)
  // ══════════════════════════════════════════════════════════

  /** POST /api/host/hotels — tạo hotel mới */
  createHotel(body: Partial<Hotel>, idempotencyKey?: string): Observable<Hotel> {
    return this.http
      .post<ApiResponse<Hotel>>(
        '/api/host/hotels',
        body,
        withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      )
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  /** PUT /api/host/hotels/{id} — update hotel */
  updateHotel(id: string, body: Partial<Hotel>): Observable<Hotel> {
    return this.http
      .put<ApiResponse<Hotel>>(`/api/host/hotels/${id}`, body, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  /** GET /api/host/hotels — list hotels của host hiện tại */
  getMyHotels(): Observable<Hotel[]> {
    return this.http
      .get<ApiResponse<PaginatedData<Hotel>>>('/api/host/hotels', { withCredentials: true })
      .pipe(map((res) => res.data.content.map((hotel) => this.normalizeHotel(hotel))));
  }

  /** GET /api/host/hotels/{id} — chi tiết hotel của host */
  getMyHotelById(id: string): Observable<Hotel> {
    return this.http
      .get<ApiResponse<Hotel>>(`/api/host/hotels/${id}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  // ══════════════════════════════════════════════════════════
  // ADMIN ENDPOINTS (role ADMIN)
  // ══════════════════════════════════════════════════════════

  /**
   * GET /api/admin/hotels — list all hotels (có filter status + pagination)
   */
  getAdminHotels(
    params: {
      status?: HotelStatus;
      page?: number;
      size?: number;
    } = {},
  ): Observable<PaginatedData<Hotel>> {
    let httpParams = new HttpParams();
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<Hotel>>>('/api/admin/hotels', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(
        map((res) => ({
          ...res.data,
          content: res.data.content.map((hotel) => this.normalizeHotel(hotel)),
        })),
      );
  }

  /** GET /api/admin/hotels/{id} — chi tiết hotel (admin view) */
  getAdminHotelById(id: string): Observable<Hotel> {
    return this.http
      .get<ApiResponse<Hotel>>(`/api/admin/hotels/${id}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  /** POST /api/admin/hotels/{id}/approve — duyệt hotel */
  approveHotel(id: string): Observable<Hotel> {
    return this.http
      .post<ApiResponse<Hotel>>(`/api/admin/hotels/${id}/approve`, {}, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  deactivateAdminHotel(id: string): Observable<Hotel> {
    return this.http
      .post<ApiResponse<Hotel>>(`/api/admin/hotels/${id}/deactivate`, {}, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  deactivateMyHotel(id: string): Observable<Hotel> {
    return this.http
      .post<ApiResponse<Hotel>>(`/api/host/hotels/${id}/deactivate`, {}, { withCredentials: true })
      .pipe(map((res) => this.normalizeHotel(res.data)));
  }

  deleteAdminHotel(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`/api/admin/hotels/${id}`, { withCredentials: true })
      .pipe(map(() => undefined));
  }

  deleteMyHotel(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`/api/host/hotels/${id}`, { withCredentials: true })
      .pipe(map(() => undefined));
  }

  private normalizeHotel(raw: any): Hotel {
    const images = Array.isArray(raw?.images) ? raw.images : [];
    const rating = Number(raw?.rating ?? raw?.starRating ?? 0);

    return {
      ...raw,
      images,
      thumbnail: raw?.thumbnail || images[0] || 'assets/placeholder-room.jpg',
      starRating: raw?.starRating ?? Math.max(1, Math.round(rating || 4)),
      userRating: raw?.userRating ?? Number((rating ? rating * 2 : 8).toFixed(1)),
      reviewCount: raw?.reviewCount ?? 0,
      pricePerNight: raw?.pricePerNight ?? 0,
      currency: raw?.currency ?? 'VND',
      rooms: raw?.rooms ?? [],
      policies: raw?.policies ?? {
        checkIn: raw?.checkInTime ?? '14:00',
        checkOut: raw?.checkOutTime ?? '12:00',
        cancellation: '',
        children: '',
        pets: '',
        smoking: '',
      },
      location: raw?.location ?? {
        latitude: 0,
        longitude: 0,
        district: '',
      },
      ownerId: raw?.ownerId ?? raw?.ownerUserId,
    } as Hotel;
  }
}
