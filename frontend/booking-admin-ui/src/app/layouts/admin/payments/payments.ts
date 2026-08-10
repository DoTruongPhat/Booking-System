import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { PaymentResponse, PaymentService, RefundResponse } from '../../../core/services/payment.service';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzFormModule,
    NzIconModule,
    NzInputModule,
    NzInputNumberModule,
    NzModalModule,
    NzSelectModule,
    NzSpinModule,
    NzTableModule,
    NzTagModule,
    NzTooltipModule,
  ],
  templateUrl: './payments.html',
  styleUrl: './payments.scss',
})
export class Payments implements OnInit {
  private paymentService = inject(PaymentService);
  private message = inject(NzMessageService);

  payments: PaymentResponse[] = [];
  refunds: RefundResponse[] = [];
  selectedPayment: PaymentResponse | null = null;

  loading = false;
  refundLoading = false;
  refundModalOpen = false;
  detailModalOpen = false;
  syncingPaymentIds = new Set<string>();

  pageIndex = 1;
  pageSize = 10;
  total = 0;
  status: string | null = null;

  refundAmount = 0;
  refundReason = '';

  readonly statuses = [
    'PENDING',
    'PROCESSING',
    'SUCCESS',
    'FAILED',
    'CANCELLED',
    'EXPIRED',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
  ];

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.loading = true;

    this.paymentService
      .getAllPayments({
        status: this.status || undefined,
        page: this.pageIndex - 1,
        size: this.pageSize,
      })
      .subscribe({
        next: (data) => {
          this.payments = data.content;
          this.total = data.totalElements;
          this.loading = false;
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Không thể tải thanh toán');
          this.loading = false;
        },
      });
  }

  onStatusChange(): void {
    this.pageIndex = 1;
    this.loadPayments();
  }

  onPageChange(page: number): void {
    this.pageIndex = page;
    this.loadPayments();
  }

  resetFilter(): void {
    this.status = null;
    this.pageIndex = 1;
    this.loadPayments();
  }

  openDetail(payment: PaymentResponse): void {
    this.selectedPayment = payment;
    this.refunds = [];
    this.detailModalOpen = true;

    this.paymentService.getRefundHistory(payment.id).subscribe({
      next: (refunds) => {
        this.refunds = refunds;
      },
      error: () => {
        this.refunds = [];
      },
    });
  }

  openRefund(payment: PaymentResponse): void {
    this.selectedPayment = payment;
    this.refundAmount = payment.amount;
    this.refundReason = '';
    this.refundModalOpen = true;
  }

  syncPayment(payment: PaymentResponse): void {
    if (!payment.gatewayTxnId || this.syncingPaymentIds.has(payment.id)) return;

    this.syncingPaymentIds.add(payment.id);
    this.paymentService.syncPayment(payment.id).subscribe({
      next: (res) => {
        const updated = res.data;
        if (updated?.status === 'SUCCESS') {
          this.message.success('Đã đồng bộ thanh toán thành công');
        } else {
          this.message.info(res.message || 'Gateway chưa xác nhận thanh toán thành công');
        }
        this.syncingPaymentIds.delete(payment.id);
        this.loadPayments();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể đồng bộ thanh toán');
        this.syncingPaymentIds.delete(payment.id);
      },
    });
  }

  submitRefund(): void {
    if (!this.selectedPayment || this.refundAmount <= 0) {
      this.message.warning('Nhập số tiền hoàn hợp lệ');
      return;
    }

    this.refundLoading = true;
    this.paymentService
      .refundPayment(
        this.selectedPayment.id,
        this.refundAmount,
        this.refundReason || 'Admin refund',
        crypto.randomUUID(),
      )
      .subscribe({
        next: () => {
          this.message.success('Đã tạo yêu cầu hoàn tiền');
          this.refundLoading = false;
          this.refundModalOpen = false;
          this.loadPayments();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Không thể hoàn tiền');
          this.refundLoading = false;
        },
      });
  }

  canRefund(payment: PaymentResponse): boolean {
    return payment.status === 'SUCCESS' || payment.status === 'PARTIALLY_REFUNDED';
  }

  canSync(payment: PaymentResponse): boolean {
    return !!payment.gatewayTxnId
      && !['SUCCESS', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED'].includes(payment.status);
  }

  isSyncing(payment: PaymentResponse): boolean {
    return this.syncingPaymentIds.has(payment.id);
  }

  formatVND(amount: number): string {
    return new Intl.NumberFormat('vi-VN').format(amount || 0) + ' đ';
  }

  formatDateTime(date?: string): string {
    return date ? new Date(date).toLocaleString('vi-VN') : '-';
  }

  statusLabel(status: string): string {
    return this.paymentService.getStatusLabel(status);
  }

  statusColor(status: string): string {
    return this.paymentService.getStatusColor(status);
  }

  methodLabel(method: string): string {
    return this.paymentService.getMethodLabel(method);
  }
}
