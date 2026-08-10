import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PaginatedData } from '../models/api-response.model';
import { Auth } from './auth';

export type DiscountType = 'PERCENT' | 'FIXED';

export interface RoomTypeItem {
  id: string;
  hotelId?: string | null;
  hotelName?: string | null;
  code: string;
  name: string;
  description?: string;
  defaultCapacity?: number;
  defaultAmenities?: string[];
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PromotionItem {
  id: string;
  hotelId?: string | null;
  hotelName?: string | null;
  title: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  startDate: string;
  endDate: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface VoucherItem {
  id: string;
  hotelId?: string | null;
  hotelName?: string | null;
  code: string;
  description?: string;
  discountType: DiscountType;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscountAmount?: number;
  usageLimit?: number;
  usedCount: number;
  startDate: string;
  endDate: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type CatalogKind = 'room-types' | 'promotions' | 'vouchers';

@Injectable({ providedIn: 'root' })
export class CatalogManagementService {
  private http = inject(HttpClient);
  private auth = inject(Auth);

  list<T>(
    kind: CatalogKind,
    params: { hotelId?: string; active?: boolean; page?: number; size?: number } = {},
  ): Observable<PaginatedData<T>> {
    let httpParams = new HttpParams();
    if (params.hotelId) httpParams = httpParams.set('hotelId', params.hotelId);
    if (params.active !== undefined) httpParams = httpParams.set('active', params.active);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<T>>>(this.baseUrl(kind), {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => this.normalizePage(res.data)));
  }

  create<T>(kind: CatalogKind, body: Partial<T>): Observable<T> {
    return this.http
      .post<ApiResponse<T>>(this.baseUrl(kind), body, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  update<T extends { id: string }>(kind: CatalogKind, id: string, body: Partial<T>): Observable<T> {
    return this.http
      .put<ApiResponse<T>>(`${this.baseUrl(kind)}/${id}`, body, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  delete(kind: CatalogKind, id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.baseUrl(kind)}/${id}`, { withCredentials: true })
      .pipe(map(() => void 0));
  }

  private baseUrl(kind: CatalogKind): string {
    const role = this.auth.getPrimaryRole();
    const prefix = role === 'HOST' ? '/api/host' : '/api/admin';
    return `${prefix}/${kind}`;
  }

  private normalizePage<T>(raw: PaginatedData<T> | null | undefined): PaginatedData<T> {
    return {
      content: raw?.content ?? [],
      totalElements: raw?.totalElements ?? 0,
      totalPages: raw?.totalPages ?? 0,
      number: raw?.number ?? 0,
      size: raw?.size ?? 20,
    };
  }
}
