import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';

import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';

import { Auth } from '../../../core/services/auth';
import { HotelService, HotelStatus } from '../../../core/services/hotel.service';
import { Hotel } from '../../../core/models/hotel.model';

const STATUS_LABELS: Record<string, string> = {
  PENDING_APPROVAL: 'Cho duyet',
  ACTIVE: 'Dang hoat dong',
  INACTIVE: 'Ngung hoat dong',
};

const STATUS_COLORS: Record<string, string> = {
  PENDING_APPROVAL: 'orange',
  ACTIVE: 'green',
  INACTIVE: 'default',
};

@Component({
  selector: 'app-manage-hotels',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    NzTableModule,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
    NzSelectModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSpinModule,
    NzCardModule,
    NzEmptyModule,
    NzPaginationModule,
  ],
  templateUrl: './manage-hotels.component.html',
  styleUrl: './manage-hotels.component.scss',
})
export class ManageHotels implements OnInit {
  private auth = inject(Auth);
  private hotelService = inject(HotelService);
  private message = inject(NzMessageService);
  private modal = inject(NzModalService);
  private fb = inject(FormBuilder);

  role: 'ADMIN' | 'HOST' | 'USER' = 'USER';

  hotels: Hotel[] = [];
  loading = false;
  totalElements = 0;
  currentPage = 1;
  pageSize = 10;

  statusFilter: HotelStatus | '' = '';
  statusOptions: Array<{ label: string; value: HotelStatus | '' }> = [
    { label: 'Tat ca', value: '' },
    { label: 'Cho duyet', value: 'PENDING_APPROVAL' },
    { label: 'Dang hoat dong', value: 'ACTIVE' },
    { label: 'Ngung hoat dong', value: 'INACTIVE' },
  ];

  isFormModalOpen = false;
  isSubmitting = false;
  editingHotel: Hotel | null = null;

  hotelForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(255)]],
    description: [''],
    address: ['', [Validators.required, Validators.maxLength(500)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    country: ['Vietnam', [Validators.required, Validators.maxLength(100)]],
    checkInTime: ['14:00', Validators.required],
    checkOutTime: ['12:00', Validators.required],
    amenitiesText: [''],
    imagesText: [''],
  });

  get isAdmin(): boolean {
    return this.role === 'ADMIN';
  }

  get isHost(): boolean {
    return this.role === 'HOST';
  }

  ngOnInit(): void {
    this.role = this.auth.getPrimaryRole();
    this.loadHotels();
  }

  loadHotels(): void {
    this.loading = true;

    if (this.isAdmin) {
      const params: { status?: HotelStatus; page: number; size: number } = {
        page: this.currentPage - 1,
        size: this.pageSize,
      };
      if (this.statusFilter) params.status = this.statusFilter;

      this.hotelService.getAdminHotels(params).subscribe({
        next: (data) => {
          this.hotels = data.content;
          this.totalElements = data.totalElements;
          this.currentPage = data.number + 1;
          this.loading = false;
        },
        error: () => {
          this.message.error('Khong the tai khach san.');
          this.loading = false;
        },
      });
      return;
    }

    this.hotelService.getMyHotels().subscribe({
      next: (data) => {
        this.hotels = data;
        this.totalElements = data.length;
        this.loading = false;
      },
      error: () => {
        this.message.error('Khong the tai khach san.');
        this.loading = false;
      },
    });
  }

  onFilterChange(): void {
    this.currentPage = 1;
    this.loadHotels();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadHotels();
  }

  approveHotel(hotel: Hotel): void {
    this.modal.confirm({
      nzTitle: 'Duyet khach san',
      nzContent: `Xac nhan duyet <strong>${hotel.name}</strong>?`,
      nzOkText: 'Duyet',
      nzCancelText: 'Huy',
      nzOnOk: () => {
        this.hotelService.approveHotel(hotel.id).subscribe({
          next: () => {
            this.message.success('Da duyet.');
            this.loadHotels();
          },
          error: () => this.message.error('Khong the duyet.'),
        });
      },
    });
  }

  openCreateHotelModal(): void {
    this.editingHotel = null;
    this.hotelForm.reset({
      name: '',
      description: '',
      address: '',
      city: '',
      country: 'Vietnam',
      checkInTime: '14:00',
      checkOutTime: '12:00',
      amenitiesText: '',
      imagesText: '',
    });
    this.isFormModalOpen = true;
  }

  openEditHotelModal(hotel: Hotel): void {
    this.editingHotel = hotel;
    this.hotelForm.reset({
      name: hotel.name || '',
      description: hotel.description || '',
      address: hotel.address || '',
      city: hotel.city || '',
      country: hotel.country || 'Vietnam',
      checkInTime: this.toInputTime((hotel as any).checkInTime || hotel.policies?.checkIn),
      checkOutTime: this.toInputTime((hotel as any).checkOutTime || hotel.policies?.checkOut),
      amenitiesText: this.listToText(hotel.amenities),
      imagesText: this.listToText(hotel.images),
    });
    this.isFormModalOpen = true;
  }

  submitHotelForm(): void {
    if (this.hotelForm.invalid) {
      Object.values(this.hotelForm.controls).forEach((control) => {
        control.markAsDirty();
        control.updateValueAndValidity();
      });
      this.message.warning('Vui long nhap day du thong tin bat buoc.');
      return;
    }

    const payload = this.buildHotelPayload();
    this.isSubmitting = true;

    const request$ = this.editingHotel
      ? this.hotelService.updateHotel(this.editingHotel.id, payload)
      : this.hotelService.createHotel(payload);

    request$.subscribe({
      next: () => {
        this.message.success(this.editingHotel ? 'Da cap nhat khach san.' : 'Da tao khach san.');
        this.isSubmitting = false;
        this.isFormModalOpen = false;
        this.loadHotels();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Khong the luu khach san.');
        this.isSubmitting = false;
      },
    });
  }

  getStatusLabel(status: string): string {
    return STATUS_LABELS[status] || status;
  }

  getStatusColor(status: string): string {
    return STATUS_COLORS[status] || 'default';
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price || 0) + 'd';
  }

  private buildHotelPayload(): Partial<Hotel> {
    const raw = this.hotelForm.getRawValue();

    return {
      name: raw.name.trim(),
      description: raw.description.trim(),
      address: raw.address.trim(),
      city: raw.city.trim(),
      country: raw.country.trim(),
      amenities: this.parseList(raw.amenitiesText),
      images: this.parseList(raw.imagesText),
      checkInTime: this.toInputTime(raw.checkInTime),
      checkOutTime: this.toInputTime(raw.checkOutTime),
    } as Partial<Hotel>;
  }

  private parseList(value: string): string[] {
    return value
      .split(/[\n,]/)
      .map((item) => item.trim())
      .filter(Boolean);
  }

  private listToText(value: string[] | undefined): string {
    return Array.isArray(value) ? value.join('\n') : '';
  }

  private toInputTime(value: string | undefined | null): string {
    if (!value) return '14:00';
    return value.slice(0, 5);
  }
}
