// ═══════════════════════════════════════════════════════════
// ROOM SERVICE — Phase E (API Integration)
// Thay MockCacheService bằng HttpClient gọi core-service
//
// Public: GET /api/rooms/search, GET /api/rooms/{roomId}
// Host:  POST /api/host/hotels/{hotelId}/rooms
//        PUT /api/host/rooms/{roomId}
//        GET /api/host/hotels/{hotelId}/rooms
//        POST /api/host/rooms/{roomId}/availability/block
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PaginatedData } from '../models/api-response.model';

// ── Room search result (từ GET /api/rooms/search) ──────────
export interface RoomSearchResult {
  id: string;
  hotelId: string;
  hotelName: string;
  hotelCity: string;
  hotelRating: number;
  hotelThumbnail?: string;
  name: string;
  roomType: string;
  bedType: string;
  size: number;
  maxAdults: number;
  maxChildren: number;
  basePrice: number;
  amenities: string[];
  breakfastIncluded: boolean;
  freeCancellation: boolean;
  images?: string[];
  available: number;
}

// ── Room detail (từ GET /api/rooms/{roomId}) ───────────────
export interface RoomDetail {
  id: string;
  hotelId: string;
  name: string;
  description: string;
  roomType: string;
  bedType: string;
  size: number;
  maxAdults: number;
  maxChildren: number;
  basePrice: number;
  amenities: string[];
  breakfastIncluded: boolean;
  freeCancellation: boolean;
  images: string[];
  totalRooms: number;
  active: boolean;
  // Compat fields (HTML cũ dùng tên này)
  pricePerNight: number; // = basePrice
  originalPrice: number; // giá gốc trước discount
  available: number; // số phòng còn trống
  payLater: boolean; // trả tại khách sạn
  // Hotel info (denormalized từ backend, có thể null nếu API trả thiếu)
  hotel?: {
    id: string;
    name: string;
    address: string;
    city: string;
    starRating: number;
    userRating: number;
    reviewCount: number;
    amenities: string[];
    checkInTime: string;
    checkOutTime: string;
    thumbnail: string;
    images: string[];
  };
  // Availability 30 ngày
  availability: DayAvailability[];
}

export interface DayAvailability {
  date: string; // yyyy-MM-dd
  available: number; // số phòng còn trống
  price: number; // giá ngày đó
  blocked: boolean; // host block hay không
}

// ── Search params ──────────────────────────────────────────
export interface RoomSearchParams {
  city?: string;
  checkIn?: string;
  checkOut?: string;
  guests?: number;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  page?: number;
  size?: number;
}

// ── Block dates request ────────────────────────────────────
export interface BlockDatesRequest {
  startDate: string; // yyyy-MM-dd
  endDate: string; // yyyy-MM-dd
}

@Injectable({ providedIn: 'root' })
export class RoomService {
  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════════════════════
  // PUBLIC ENDPOINTS
  // ══════════════════════════════════════════════════════════

  /**
   * GET /api/rooms/search — tìm phòng (public, có pagination)
   * Backend sort mặc định: rating DESC
   */
  search(params: RoomSearchParams): Observable<PaginatedData<RoomSearchResult>> {
    let httpParams = new HttpParams();
    if (params.city) httpParams = httpParams.set('city', params.city);
    if (params.checkIn) httpParams = httpParams.set('checkIn', params.checkIn);
    if (params.checkOut) httpParams = httpParams.set('checkOut', params.checkOut);
    if (params.guests !== undefined) httpParams = httpParams.set('guests', params.guests);
    if (params.minPrice !== undefined) httpParams = httpParams.set('minPrice', params.minPrice);
    if (params.maxPrice !== undefined) httpParams = httpParams.set('maxPrice', params.maxPrice);
    if (params.minRating !== undefined) httpParams = httpParams.set('minRating', params.minRating);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<RoomSearchResult>>>('/api/rooms/search', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  /** GET /api/rooms/{roomId} — chi tiết room + hotel info + 30 ngày availability */
  getById(roomId: string): Observable<RoomDetail> {
    return this.http
      .get<ApiResponse<RoomDetail>>(`/api/rooms/${roomId}`, { withCredentials: true })
      .pipe(map((res) => this.normalizeRoom(res.data)));
  }

  // ══════════════════════════════════════════════════════════
  // HOST ENDPOINTS (role HOST)
  // ══════════════════════════════════════════════════════════

  /** POST /api/host/hotels/{hotelId}/rooms — tạo room mới */
  createRoom(hotelId: string, body: Partial<RoomDetail>): Observable<RoomDetail> {
    return this.http
      .post<ApiResponse<RoomDetail>>(`/api/host/hotels/${hotelId}/rooms`, body, {
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeRoom(res.data)));
  }

  /** PUT /api/host/rooms/{roomId} — update room */
  updateRoom(roomId: string, body: Partial<RoomDetail>): Observable<RoomDetail> {
    return this.http
      .put<ApiResponse<RoomDetail>>(`/api/host/rooms/${roomId}`, body, {
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizeRoom(res.data)));
  }

  /** GET /api/host/hotels/{hotelId}/rooms — list rooms của hotel (host) */
  getHostRooms(hotelId: string, params: { page?: number; size?: number } = {}): Observable<RoomDetail[]> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<RoomDetail>>>(`/api/host/hotels/${hotelId}/rooms`, {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data.content.map((room) => this.normalizeRoom(room))));
  }

  /**
   * POST /api/host/rooms/{roomId}/availability/block
   * Block ngày cho room (host không muốn nhận booking trong khoảng này)
   */
  blockDates(roomId: string, body: BlockDatesRequest): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`/api/host/rooms/${roomId}/availability/block`, body, {
        withCredentials: true,
      })
      .pipe(map(() => void 0));
  }

  private normalizeRoom(raw: any): RoomDetail {
    const capacity = Number(raw?.capacity ?? raw?.maxAdults ?? 1);
    const totalRooms = Number(raw?.totalRooms ?? 0);
    const images = Array.isArray(raw?.images) ? raw.images : [];
    const basePrice = Number(raw?.basePrice ?? raw?.pricePerNight ?? 0);
    const status = raw?.status ?? (raw?.active === false ? 'INACTIVE' : 'AVAILABLE');

    return {
      ...raw,
      images,
      basePrice,
      pricePerNight: raw?.pricePerNight ?? basePrice,
      originalPrice: raw?.originalPrice ?? basePrice,
      totalRooms,
      available: raw?.available ?? totalRooms,
      active: raw?.active ?? status === 'AVAILABLE',
      bedType: raw?.bedType ?? 'Standard bed',
      size: raw?.size ?? 30,
      maxAdults: raw?.maxAdults ?? capacity,
      maxChildren: raw?.maxChildren ?? 0,
      breakfastIncluded: raw?.breakfastIncluded ?? false,
      freeCancellation: raw?.freeCancellation ?? true,
      payLater: raw?.payLater ?? false,
      availability: raw?.availability ?? [],
    } as RoomDetail;
  }
}
