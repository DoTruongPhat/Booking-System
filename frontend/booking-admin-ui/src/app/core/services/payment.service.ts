import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse, PaginatedData } from '../models/api-response.model';
import { withIdempotencyHeader } from '../http/idempotency';

export interface InitPaymentRequest {
  bookingId: string;
  amount: number;
  method: 'VIETQR';
  currency?: string;
}

export interface InitPaymentResponse {
  paymentId: string;
  paymentCode: string;
  paymentUrl: string;
  expiresAt: string;
}

export interface PaymentResponse {
  id: string;
  paymentCode: string;
  bookingId: string;
  userId: string;
  amount: number;
  currency: string;
  method: string;
  status: string;
  gatewayTxnId: string;
  gatewayUrl: string;
  initiatedAt: string;
  completedAt: string;
  expiresAt: string;
  createdAt: string;
}

export interface RefundResponse {
  id: string;
  paymentId: string;
  amount: number;
  reason: string;
  status: string;
  gatewayRefundTxnId: string;
  requestedBy: string;
  requestedAt: string;
  completedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private http: HttpClient) {}

  initPayment(request: InitPaymentRequest, idempotencyKey?: string): Observable<InitPaymentResponse> {
    return this.http
      .post<ApiResponse<InitPaymentResponse>>('/api/user/payments/init', request, {
        ...withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      })
      .pipe(map((res) => res.data));
  }

  getPaymentByBooking(bookingId: string): Observable<PaymentResponse> {
    return this.http
      .get<ApiResponse<PaymentResponse>>('/api/user/payments', {
        params: new HttpParams().set('bookingId', bookingId),
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  cancelPayment(
    paymentId: string,
    reason?: string,
    idempotencyKey?: string,
  ): Observable<PaymentResponse> {
    return this.http
      .post<ApiResponse<PaymentResponse>>(
        `/api/user/payments/${paymentId}/cancel`,
        { reason },
        withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      )
      .pipe(map((res) => res.data));
  }

  getAllPayments(
    params: {
      status?: string;
      page?: number;
      size?: number;
    } = {},
  ): Observable<PaginatedData<PaymentResponse>> {
    let httpParams = new HttpParams();
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<PaginatedData<PaymentResponse>>>('/api/admin/payments', {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  getPaymentById(id: string): Observable<PaymentResponse> {
    return this.http
      .get<ApiResponse<PaymentResponse>>(`/api/admin/payments/${id}`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  syncPayment(id: string): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(
      `/api/admin/payments/${id}/sync`,
      {},
      { withCredentials: true },
    );
  }

  getRefundHistory(paymentId: string): Observable<RefundResponse[]> {
    return this.http
      .get<ApiResponse<RefundResponse[]>>(`/api/admin/payments/${paymentId}/refunds`, {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  refundPayment(
    paymentId: string,
    amount: number,
    reason: string,
    idempotencyKey?: string,
  ): Observable<RefundResponse> {
    return this.http
      .post<ApiResponse<RefundResponse>>(
        `/api/admin/payments/${paymentId}/refund`,
        { amount, reason },
        withIdempotencyHeader({ withCredentials: true }, idempotencyKey),
      )
      .pipe(map((res) => res.data));
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Chờ thanh toán',
      PROCESSING: 'Đang xử lý',
      SUCCESS: 'Thành công',
      FAILED: 'Thất bại',
      CANCELLED: 'Đã hủy',
      EXPIRED: 'Hết hạn',
      REFUNDED: 'Đã hoàn tiền',
      PARTIALLY_REFUNDED: 'Hoàn một phần',
    };

    return labels[status] || status;
  }

  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      PENDING: 'orange',
      PROCESSING: 'green',
      SUCCESS: 'green',
      FAILED: 'red',
      CANCELLED: 'default',
      EXPIRED: 'default',
      REFUNDED: 'purple',
      PARTIALLY_REFUNDED: 'purple',
    };

    return colors[status] || 'default';
  }

  getMethodLabel(method: string): string {
    const labels: Record<string, string> = {
      VNPAY: 'VNPay',
      VIETQR: 'VietQR',
    };

    return labels[method] || method;
  }
}
