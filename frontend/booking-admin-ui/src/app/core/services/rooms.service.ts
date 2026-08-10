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
import { withIdempotencyHeader } from '../http/idempotency';

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
    const checkIn = params.checkIn || this.formatDate(new Date());
    const checkOut = params.checkOut || this.formatDate(this.addDays(new Date(), 1));

    httpParams = httpParams.set('city', params.city?.trim() || '');
    httpParams = httpParams.set('checkIn', checkIn);
    httpParams = httpParams.set('checkOut', checkOut);
    httpParams = httpParams.set('guests', params.guests ?? 1);
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
      .pipe(map((res) => this.normalizePage(res.data)));
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
  createRoom(
    hotelId: string,
    body: Partial<RoomDetail>,
    idempotencyKey?: string,
  ): Observable<RoomDetail> {
    return this.http
      .post<ApiResponse<RoomDetail>>(`/api/host/hotels/${hotelId}/rooms`, body, {
        ...withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
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

  /** GET /api/admin/hotels/{hotelId}/rooms — admin read-only rooms by hotel */
  getAdminRooms(hotelId: string, params: { page?: number; size?: number } = {}): Observable<RoomDetail[]> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<RoomDetail>>>(`/api/admin/hotels/${hotelId}/rooms`, {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data.content.map((room) => this.normalizeRoom(room))));
  }

  /**
   * POST /api/host/rooms/{roomId}/availability/block
   * Block ngày cho room (host không muốn nhận booking trong khoảng này)
   */
  blockDates(roomId: string, body: BlockDatesRequest, idempotencyKey?: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`/api/host/rooms/${roomId}/availability/block`, body, {
        ...withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      })
      .pipe(map(() => void 0));
  }

  private normalizeRoom(raw: any): RoomDetail {
    const capacity = Number(raw?.capacity ?? raw?.maxAdults ?? 1);
    const totalRooms = Number(raw?.totalRooms ?? 0);
    const images = this.normalizeImages(raw?.images ?? raw?.roomImages);
    const hotelImages = this.normalizeImages(raw?.hotel?.images ?? raw?.hotelImages);
    const basePrice = Number(raw?.basePrice ?? raw?.pricePerNight ?? 0);
    const status = raw?.status ?? (raw?.active === false ? 'INACTIVE' : 'AVAILABLE');
    const roomStatus = raw?.roomStatus ?? status;
    const hotelId = raw?.hotelId ?? raw?.hotel?.id;
    const hotel = raw?.hotel ?? {
      id: hotelId,
      name: raw?.hotelName ?? '',
      address: raw?.hotelAddress ?? '',
      city: raw?.hotelCity ?? '',
      starRating: Number(raw?.starRating ?? raw?.hotelStarRating ?? 0),
      userRating: Number(raw?.userRating ?? raw?.hotelRating ?? 0),
      reviewCount: Number(raw?.reviewCount ?? 0),
      amenities: raw?.hotelAmenities ?? [],
      checkInTime: raw?.checkInTime ?? '',
      checkOutTime: raw?.checkOutTime ?? '',
      thumbnail: raw?.hotelThumbnail ?? hotelImages[0] ?? '',
      images: hotelImages,
    };

    return {
      ...raw,
      id: raw?.id ?? raw?.roomId,
      hotelId,
      name: raw?.name ?? raw?.roomName ?? 'Room',
      description: raw?.description ?? raw?.roomDescription ?? '',
      images,
      amenities: raw?.amenities ?? raw?.roomAmenities ?? [],
      basePrice,
      pricePerNight: raw?.pricePerNight ?? basePrice,
      originalPrice: raw?.originalPrice ?? basePrice,
      totalRooms,
      available: raw?.available ?? totalRooms,
      active: raw?.active ?? roomStatus === 'AVAILABLE',
      bedType: raw?.bedType ?? 'Standard bed',
      size: raw?.size ?? 30,
      maxAdults: raw?.maxAdults ?? capacity,
      maxChildren: raw?.maxChildren ?? 0,
      breakfastIncluded: raw?.breakfastIncluded ?? false,
      freeCancellation: raw?.freeCancellation ?? true,
      payLater: raw?.payLater ?? false,
      hotel,
      availability: this.normalizeAvailability(raw?.availability ?? raw?.availabilities),
    } as RoomDetail;
  }

  private normalizePage(raw: PaginatedData<any>): PaginatedData<RoomSearchResult> {
    return {
      ...raw,
      content: (raw?.content ?? []).map((room) => this.normalizeSearchRoom(room)),
      totalElements: raw?.totalElements ?? 0,
      totalPages: raw?.totalPages ?? 0,
      number: raw?.number ?? 0,
      size: raw?.size ?? 10,
    };
  }

  private normalizeSearchRoom(raw: any): RoomSearchResult {
    const capacity = Number(raw?.capacity ?? raw?.maxAdults ?? 1);
    const images = this.normalizeImages(raw?.images ?? raw?.roomImages);
    const basePrice = Number(raw?.minPrice ?? raw?.basePrice ?? raw?.pricePerNight ?? 0);

    return {
      ...raw,
      id: raw?.id ?? raw?.roomId,
      hotelId: raw?.hotelId,
      hotelName: raw?.hotelName ?? '',
      hotelCity: raw?.hotelCity ?? raw?.city ?? '',
      hotelRating: Number(raw?.hotelRating ?? raw?.rating ?? 0),
      hotelThumbnail: raw?.hotelThumbnail ?? raw?.thumbnail,
      name: raw?.name ?? raw?.roomName ?? 'Room',
      roomType: raw?.roomType ?? '',
      bedType: raw?.bedType ?? 'Standard bed',
      size: Number(raw?.size ?? 30),
      maxAdults: capacity,
      maxChildren: Number(raw?.maxChildren ?? 0),
      basePrice,
      amenities: raw?.amenities ?? raw?.roomAmenities ?? [],
      breakfastIncluded: raw?.breakfastIncluded ?? false,
      freeCancellation: raw?.freeCancellation ?? true,
      images,
      available: Number(raw?.available ?? raw?.totalRooms ?? 0),
    };
  }

  private normalizeAvailability(raw: any): DayAvailability[] {
    if (!Array.isArray(raw)) return [];
    return raw.map((day) => {
      const status = String(day?.status ?? '').toUpperCase();
      const available = Number(day?.available ?? day?.availableCount ?? 0);
      return {
        date: day?.date,
        available,
        price: Number(day?.price ?? day?.effectivePrice ?? 0),
        blocked: Boolean(day?.blocked) || status === 'BLOCKED' || available <= 0,
      };
    });
  }

  private normalizeImages(raw: any): string[] {
    return Array.isArray(raw)
      ? raw.filter((url): url is string => typeof url === 'string' && url.trim().length > 0)
      : [];
  }

  private addDays(date: Date, days: number): Date {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
