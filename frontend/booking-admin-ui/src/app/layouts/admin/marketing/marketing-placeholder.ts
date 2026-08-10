import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { Hotel } from '../../../core/models/hotel.model';
import { Auth } from '../../../core/services/auth';
import {
  CatalogKind,
  CatalogManagementService,
  DiscountType,
  PromotionItem,
  VoucherItem,
} from '../../../core/services/catalog-management.service';
import { HotelService } from '../../../core/services/hotel.service';

type MarketingItem = PromotionItem | VoucherItem;

type MarketingForm = {
  id?: string;
  hotelId?: string | null;
  code: string;
  title: string;
  description: string;
  discountType: DiscountType;
  discountValue: number | null;
  minOrderAmount: number | null;
  maxDiscountAmount: number | null;
  usageLimit: number | null;
  startDate: string;
  endDate: string;
  active: boolean;
};

type MarketingPageData = {
  title: string;
  subtitle: string;
  icon: string;
};

@Component({
  selector: 'app-marketing-placeholder',
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzCardModule,
    NzEmptyModule,
    NzFormModule,
    NzIconModule,
    NzInputModule,
    NzModalModule,
    NzPopconfirmModule,
    NzSelectModule,
    NzTableModule,
    NzTagModule,
  ],
  templateUrl: './marketing-placeholder.html',
  styleUrl: './marketing-placeholder.scss',
})
export class MarketingPlaceholder implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(Auth);
  private catalog = inject(CatalogManagementService);
  private hotelService = inject(HotelService);
  private message = inject(NzMessageService);

  isHost = false;
  isLoading = false;
  isSubmitting = false;
  isModalOpen = false;
  hotels: Hotel[] = [];
  items: MarketingItem[] = [];
  form: MarketingForm = this.emptyForm();

  readonly discountTypes: Array<{ label: string; value: DiscountType }> = [
    { label: 'Phần trăm', value: 'PERCENT' },
    { label: 'Số tiền cố định', value: 'FIXED' },
  ];

  ngOnInit(): void {
    this.isHost = this.auth.getPrimaryRole() === 'HOST';
    this.loadHotels();
    this.loadItems();
  }

  get page(): MarketingPageData {
    return this.route.snapshot.data as MarketingPageData;
  }

  get kind(): CatalogKind {
    return this.router.url.includes('vouchers') ? 'vouchers' : 'promotions';
  }

  get isVoucherPage(): boolean {
    return this.kind === 'vouchers';
  }

  get activeCount(): number {
    return this.items.filter((item) => item.active).length;
  }

  get globalCount(): number {
    return this.items.filter((item) => !item.hotelId).length;
  }

  loadItems(): void {
    this.isLoading = true;
    this.catalog.list<MarketingItem>(this.kind, { page: 0, size: 100 }).subscribe({
      next: (data) => {
        this.items = data.content;
        this.isLoading = false;
      },
      error: () => {
        this.message.error(`Không thể tải ${this.page.title.toLowerCase()}`);
        this.items = [];
        this.isLoading = false;
      },
    });
  }

  openCreateModal(): void {
    this.form = this.emptyForm();
    if (this.isHost && this.hotels.length) {
      this.form.hotelId = this.hotels[0].id;
    }
    this.isModalOpen = true;
  }

  openEditModal(item: MarketingItem): void {
    const voucher = item as VoucherItem;
    const promotion = item as PromotionItem;
    this.form = {
      id: item.id,
      hotelId: item.hotelId,
      code: voucher.code || '',
      title: promotion.title || '',
      description: item.description || '',
      discountType: item.discountType,
      discountValue: Number(item.discountValue || 0),
      minOrderAmount: voucher.minOrderAmount ?? null,
      maxDiscountAmount: voucher.maxDiscountAmount ?? null,
      usageLimit: voucher.usageLimit ?? null,
      startDate: item.startDate,
      endDate: item.endDate,
      active: item.active,
    };
    this.isModalOpen = true;
  }

  submit(): void {
    if (this.isHost && !this.form.hotelId) {
      this.message.warning('Host cần chọn khách sạn');
      return;
    }
    if (!this.form.discountValue || !this.form.startDate || !this.form.endDate) {
      this.message.warning('Vui lòng nhập đủ giảm giá và ngày hiệu lực');
      return;
    }
    if (this.isVoucherPage && !this.form.code.trim()) {
      this.message.warning('Vui lòng nhập mã giảm giá');
      return;
    }
    if (!this.isVoucherPage && !this.form.title.trim()) {
      this.message.warning('Vui lòng nhập tên khuyến mãi');
      return;
    }

    const payload = this.isVoucherPage
      ? {
          hotelId: this.form.hotelId || null,
          code: this.form.code.trim().toUpperCase(),
          description: this.form.description || undefined,
          discountType: this.form.discountType,
          discountValue: this.form.discountValue,
          minOrderAmount: this.form.minOrderAmount ?? 0,
          maxDiscountAmount: this.form.maxDiscountAmount || undefined,
          usageLimit: this.form.usageLimit || undefined,
          startDate: this.form.startDate,
          endDate: this.form.endDate,
          active: this.form.active,
        }
      : {
          hotelId: this.form.hotelId || null,
          title: this.form.title.trim(),
          description: this.form.description || undefined,
          discountType: this.form.discountType,
          discountValue: this.form.discountValue,
          startDate: this.form.startDate,
          endDate: this.form.endDate,
          active: this.form.active,
        };

    this.isSubmitting = true;
    const request = this.form.id
      ? this.catalog.update<any>(this.kind, this.form.id, payload)
      : this.catalog.create<any>(this.kind, payload);

    request.subscribe({
      next: () => {
        this.message.success(this.form.id ? 'Đã cập nhật' : 'Đã tạo mới');
        this.isSubmitting = false;
        this.isModalOpen = false;
        this.loadItems();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể lưu dữ liệu');
        this.isSubmitting = false;
      },
    });
  }

  delete(item: MarketingItem): void {
    this.catalog.delete(this.kind, item.id).subscribe({
      next: () => {
        this.message.success('Đã xóa');
        this.loadItems();
      },
      error: (err) => this.message.error(err?.error?.message || 'Không thể xóa'),
    });
  }

  itemName(item: MarketingItem): string {
    return this.isVoucherPage ? (item as VoucherItem).code : (item as PromotionItem).title;
  }

  discountLabel(item: MarketingItem): string {
    const value = Number(item.discountValue || 0);
    return item.discountType === 'PERCENT'
      ? `${value}%`
      : new Intl.NumberFormat('vi-VN').format(value) + ' đ';
  }

  usedCount(item: MarketingItem): number {
    return Number((item as VoucherItem).usedCount || 0);
  }

  usageLimit(item: MarketingItem): number | string {
    return (item as VoucherItem).usageLimit || '∞';
  }

  private loadHotels(): void {
    if (this.isHost) {
      this.hotelService.getMyHotels().subscribe({
        next: (data) => {
          this.hotels = data;
        },
        error: () => {
          this.hotels = [];
        },
      });
      return;
    }

    this.hotelService.getAdminHotels({ page: 0, size: 100 }).subscribe({
      next: (data) => {
        this.hotels = data.content;
      },
      error: () => {
        this.hotels = [];
      },
    });
  }

  private emptyForm(): MarketingForm {
    const today = new Date();
    const nextMonth = new Date(today);
    nextMonth.setMonth(nextMonth.getMonth() + 1);
    return {
      hotelId: null,
      code: '',
      title: '',
      description: '',
      discountType: 'PERCENT',
      discountValue: 10,
      minOrderAmount: 0,
      maxDiscountAmount: null,
      usageLimit: null,
      startDate: this.toDateInput(today),
      endDate: this.toDateInput(nextMonth),
      active: true,
    };
  }

  private toDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
