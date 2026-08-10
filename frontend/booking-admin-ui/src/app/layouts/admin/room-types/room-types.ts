import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
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

import { Auth } from '../../../core/services/auth';
import { HotelService } from '../../../core/services/hotel.service';
import {
  CatalogManagementService,
  RoomTypeItem,
} from '../../../core/services/catalog-management.service';
import { Hotel } from '../../../core/models/hotel.model';

type RoomTypeForm = {
  id?: string;
  hotelId?: string | null;
  code: string;
  name: string;
  description: string;
  defaultCapacity?: number | null;
  defaultAmenitiesText: string;
  active: boolean;
};

@Component({
  selector: 'app-room-types',
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
  templateUrl: './room-types.html',
  styleUrl: './room-types.scss',
})
export class RoomTypes implements OnInit {
  private auth = inject(Auth);
  private catalog = inject(CatalogManagementService);
  private hotelService = inject(HotelService);
  private message = inject(NzMessageService);

  isLoading = false;
  isSubmitting = false;
  isHost = false;
  isModalOpen = false;
  roomTypes: RoomTypeItem[] = [];
  hotels: Hotel[] = [];

  form: RoomTypeForm = this.emptyForm();

  ngOnInit(): void {
    this.isHost = this.auth.getPrimaryRole() === 'HOST';
    this.loadHotels();
    this.loadRoomTypes();
  }

  loadRoomTypes(): void {
    this.isLoading = true;
    this.catalog.list<RoomTypeItem>('room-types', { page: 0, size: 100 }).subscribe({
      next: (data) => {
        this.roomTypes = data.content;
        this.isLoading = false;
      },
      error: () => {
        this.message.error('Không thể tải loại phòng');
        this.roomTypes = [];
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

  openEditModal(item: RoomTypeItem): void {
    this.form = {
      id: item.id,
      hotelId: item.hotelId,
      code: item.code,
      name: item.name,
      description: item.description || '',
      defaultCapacity: item.defaultCapacity ?? null,
      defaultAmenitiesText: (item.defaultAmenities || []).join(', '),
      active: item.active,
    };
    this.isModalOpen = true;
  }

  submit(): void {
    if (!this.form.code.trim() || !this.form.name.trim()) {
      this.message.warning('Vui lòng nhập mã và tên loại phòng');
      return;
    }
    if (this.isHost && !this.form.hotelId) {
      this.message.warning('Host cần chọn khách sạn');
      return;
    }

    const payload = {
      hotelId: this.form.hotelId || null,
      code: this.form.code.trim().toUpperCase().replace(/\s+/g, '_'),
      name: this.form.name.trim(),
      description: this.form.description || undefined,
      defaultCapacity: this.form.defaultCapacity || undefined,
      defaultAmenities: this.toList(this.form.defaultAmenitiesText),
      active: this.form.active,
    };

    this.isSubmitting = true;
    const request = this.form.id
      ? this.catalog.update<RoomTypeItem>('room-types', this.form.id, payload)
      : this.catalog.create<RoomTypeItem>('room-types', payload);

    request.subscribe({
      next: () => {
        this.message.success(this.form.id ? 'Đã cập nhật loại phòng' : 'Đã tạo loại phòng');
        this.isSubmitting = false;
        this.isModalOpen = false;
        this.loadRoomTypes();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể lưu loại phòng');
        this.isSubmitting = false;
      },
    });
  }

  delete(item: RoomTypeItem): void {
    this.catalog.delete('room-types', item.id).subscribe({
      next: () => {
        this.message.success('Đã xóa loại phòng');
        this.loadRoomTypes();
      },
      error: (err) => this.message.error(err?.error?.message || 'Không thể xóa loại phòng'),
    });
  }

  get totalRoomTypes(): number {
    return this.roomTypes.length;
  }

  get activeRoomTypes(): number {
    return this.roomTypes.filter((item) => item.active).length;
  }

  get globalRoomTypes(): number {
    return this.roomTypes.filter((item) => !item.hotelId).length;
  }

  getRoomTypeColor(code: string): string {
    const map: Record<string, string> = {
      SINGLE: 'default',
      DOUBLE: 'green',
      SUITE: 'purple',
      FAMILY: 'cyan',
      STANDARD: 'green',
      DELUXE: 'gold',
      PRESIDENTIAL: 'magenta',
      VILLA: 'volcano',
    };
    return map[code] || 'processing';
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

  private emptyForm(): RoomTypeForm {
    return {
      hotelId: null,
      code: '',
      name: '',
      description: '',
      defaultCapacity: 1,
      defaultAmenitiesText: '',
      active: true,
    };
  }

  private toList(value: string): string[] {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }
}
