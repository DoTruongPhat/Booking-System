// ═══════════════════════════════════════════════════════════
// ROOMS PAGE — Phase E (API Integration)
// Host: GET /api/host/hotels → rooms, block dates
// Admin: read-only view
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzImageModule } from 'ng-zorro-antd/image';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';

import { RoomService, RoomDetail } from '../../../../app/core/services/rooms.service';
import { HotelService } from '../../../core/services/hotel.service';
import { Auth } from '../../../core/services/auth';
import { Hotel } from '../../../core/models/hotel.model';
import {
  AMENITY_OPTIONS,
  BED_TYPE_OPTIONS,
  RoomType,
  ROOM_TYPE_LABELS,
} from '../../../core/models/room-admin.model';

@Component({
  selector: 'app-rooms',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzSelectModule,
    NzSwitchModule,
    NzPopconfirmModule,
    NzSpaceModule,
    NzTooltipModule,
    NzCardModule,
    NzAvatarModule,
    NzCheckboxModule,
    NzUploadModule,
    NzDividerModule,
    NzEmptyModule,
    NzImageModule,
    NzAlertModule,
    NzProgressModule,
    NzDescriptionsModule,
    NzDatePickerModule,
  ],
  templateUrl: './rooms.html',
  styleUrl: './rooms.scss',
})
export class Rooms implements OnInit {
  private roomService = inject(RoomService);
  private hotelService = inject(HotelService);
  private auth = inject(Auth);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);

  // ── Data ────────────────────────────────────────────────
  rooms: RoomDetail[] = [];
  hotels: Hotel[] = [];
  isLoading = false;
  isHost = false;

  // ── Filter ──────────────────────────────────────────────
  filterHotel: string | 'all' = 'all';
  filterRoomType: RoomType | 'all' = 'all';
  filterActive: 'all' | 'active' | 'inactive' = 'all';
  keyword = '';
  selectedHotelId = '';

  // ── Modal ───────────────────────────────────────────────
  isFormModalOpen = false;
  isViewModalOpen = false;
  isSubmitting = false;
  editingRoom: RoomDetail | null = null;
  selectedRoom: RoomDetail | null = null;
  imagePreviews: string[] = [];

  // ── Block dates ─────────────────────────────────────────
  blockRoomId: string | null = null;
  blockDates: [Date, Date] | null = null;
  blocking = false;

  // ── Options ─────────────────────────────────────────────
  readonly allRoomTypes: RoomType[] = ['SINGLE', 'DOUBLE', 'SUITE', 'FAMILY'];
  readonly roomTypeLabels: Record<string, string> = ROOM_TYPE_LABELS;
  readonly amenityOptions = AMENITY_OPTIONS;
  readonly bedTypeOptions = BED_TYPE_OPTIONS;

  get hotelOptions(): Array<{ id: string; name: string }> {
    return this.hotels.map((h) => ({ id: h.id, name: h.name }));
  }

  // ── Form ────────────────────────────────────────────────
  form = this.fb.nonNullable.group({
    hotelId: ['', Validators.required],
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    roomType: ['SINGLE' as RoomType, Validators.required],
    bedType: ['1 King Bed', Validators.required],
    size: [30, [Validators.required, Validators.min(1)]],
    maxAdults: [2, [Validators.required, Validators.min(1)]],
    maxChildren: [0, [Validators.required, Validators.min(0)]],
    pricePerNight: [0, [Validators.required, Validators.min(0)]],
    originalPrice: [0],
    totalRooms: [1, [Validators.required, Validators.min(1)]],
    available: [1, [Validators.required, Validators.min(0)]],
    amenities: [[] as string[]],
    breakfastIncluded: [true],
    freeCancellation: [true],
    payLater: [false],
    active: [true],
  });

  ngOnInit() {
    this.isHost = this.auth.getPrimaryRole() === 'HOST';
    this.loadHotels();
  }

  // ── Load hotels → then rooms ────────────────────────────
  loadHotels() {
    if (this.isHost) {
      this.hotelService.getMyHotels().subscribe({
        next: (data) => {
          this.hotels = data;
          if (data.length > 0) {
            this.selectedHotelId = data[0].id;
          }
          this.loadRooms();
        },
        error: () => this.message.error('Không thể tải khách sạn'),
      });
    } else {
      this.hotelService.getAdminHotels({ page: 0, size: 100 }).subscribe({
        next: (data) => {
          this.hotels = data.content;
          if (data.content.length > 0) {
            this.selectedHotelId = data.content[0].id;
          }
          this.loadRooms();
        },
        error: () => this.message.error('Không thể tải khách sạn'),
      });
    }
  }

  loadRooms() {
    if (!this.hotels.length) {
      this.rooms = [];
      return;
    }

    const hotelIds =
      this.filterHotel === 'all'
        ? this.hotels.map((hotel) => hotel.id)
        : [this.filterHotel];

    this.selectedHotelId = hotelIds[0] ?? '';
    if (!this.selectedHotelId) return;

    this.isLoading = true;
    forkJoin(hotelIds.map((hotelId) => this.roomService.getHostRooms(hotelId, { page: 0, size: 100 }))).subscribe({
      next: (roomGroups) => {
        this.rooms = this.applyLocalFilters(roomGroups.flat());
        this.isLoading = false;
      },
      error: () => {
        this.message.error('Không thể tải phòng');
        this.isLoading = false;
      },
    });
  }

  onFilterChange() {
    this.loadRooms();
  }
  onHotelChange() {
    this.loadRooms();
  }
  resetFilter() {
    this.filterHotel = 'all';
    this.filterRoomType = 'all';
    this.filterActive = 'all';
    this.keyword = '';
    this.loadRooms();
  }

  // ── CRUD ────────────────────────────────────────────────
  openCreateModal() {
    this.editingRoom = null;
    this.imagePreviews = [];
    this.form.reset({
      hotelId: this.selectedHotelId,
      name: '',
      description: '',
      roomType: 'SINGLE',
      bedType: '1 King Bed',
      size: 30,
      maxAdults: 2,
      maxChildren: 0,
      pricePerNight: 0,
      originalPrice: 0,
      totalRooms: 1,
      available: 1,
      amenities: [],
      breakfastIncluded: true,
      freeCancellation: true,
      payLater: false,
      active: true,
    });
    this.isFormModalOpen = true;
  }

  openEditModal(room: RoomDetail) {
    this.editingRoom = room;
    this.imagePreviews = [...(room.images ?? [])];
    this.form.patchValue({
      hotelId: room.hotelId,
      name: room.name,
      description: room.description,
      roomType: room.roomType as RoomType,
      bedType: room.bedType,
      size: room.size,
      maxAdults: room.maxAdults,
      maxChildren: room.maxChildren,
      pricePerNight: room.basePrice,
      totalRooms: room.totalRooms,
      amenities: [...(room.amenities ?? [])],
    });
    this.isFormModalOpen = true;
  }

  submitForm() {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach((c) => c.markAsTouched());
      return;
    }
    const raw = this.form.getRawValue();
    this.isSubmitting = true;

    const payload: any = {
      name: raw.name.trim(),
      description: raw.description.trim(),
      roomType: raw.roomType,
      bedType: raw.bedType,
      size: raw.size,
      maxAdults: raw.maxAdults,
      maxChildren: raw.maxChildren,
      capacity: Math.max(1, raw.maxAdults + raw.maxChildren),
      basePrice: raw.pricePerNight,
      totalRooms: raw.totalRooms,
      amenities: raw.amenities,
      breakfastIncluded: raw.breakfastIncluded,
      freeCancellation: raw.freeCancellation,
      images: this.imagePreviews,
      status: raw.active ? 'AVAILABLE' : 'INACTIVE',
    };

    if (this.editingRoom) {
      this.roomService.updateRoom(this.editingRoom.id, payload).subscribe({
        next: () => {
          this.message.success(`Đã cập nhật phòng "${raw.name}"`);
          this.isFormModalOpen = false;
          this.isSubmitting = false;
          this.loadRooms();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi cập nhật');
          this.isSubmitting = false;
        },
      });
    } else {
      this.roomService.createRoom(raw.hotelId, payload).subscribe({
        next: () => {
          this.message.success(`Đã tạo phòng "${raw.name}"`);
          this.isFormModalOpen = false;
          this.isSubmitting = false;
          this.loadRooms();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi tạo phòng');
          this.isSubmitting = false;
        },
      });
    }
  }

  deleteRoom(room: RoomDetail) {
    this.message.info('Chức năng xóa phòng qua API sẽ được thêm sau');
  }

  // ── Block dates ─────────────────────────────────────────
  openBlockForm(room: RoomDetail) {
    this.blockRoomId = room.id;
    this.blockDates = null;
  }

  cancelBlock() {
    this.blockRoomId = null;
    this.blockDates = null;
  }

  submitBlock() {
    if (!this.blockRoomId || !this.blockDates?.[0] || !this.blockDates?.[1]) {
      this.message.warning('Vui lòng chọn khoảng ngày');
      return;
    }
    this.blocking = true;
    this.roomService
      .blockDates(this.blockRoomId, {
        startDate: this.toISO(this.blockDates[0]),
        endDate: this.toISO(this.blockDates[1]),
      })
      .subscribe({
        next: () => {
          this.message.success('Đã chặn ngày');
          this.blocking = false;
          this.blockRoomId = null;
          this.loadRooms();
        },
        error: () => {
          this.message.error('Không thể chặn ngày');
          this.blocking = false;
        },
      });
  }

  disabledDate = (current: Date): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return current < today;
  };

  // ── View modal ──────────────────────────────────────────
  openViewModal(room: RoomDetail) {
    this.selectedRoom = room;
    this.isViewModalOpen = true;
  }

  // ── Image upload (base64) ───────────────────────────────
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.message.error('Chọn file ảnh');
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      this.message.error('Ảnh tối đa 2MB');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreviews.push(reader.result as string);
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeImage(index: number) {
    this.imagePreviews.splice(index, 1);
  }

  // ── Helpers ─────────────────────────────────────────────
  getHotelName(hotelId: string): string {
    return this.hotels.find((h) => h.id === hotelId)?.name ?? hotelId;
  }

  getRoomTypeColor(type: string): string {
    const map: Record<string, string> = {
      SINGLE: 'default',
      DOUBLE: 'blue',
      SUITE: 'purple',
      FAMILY: 'cyan',
    };
    return map[type] || 'default';
  }

  formatVND(n: number): string {
    return new Intl.NumberFormat('vi-VN').format(n) + ' đ';
  }

  getDiscountPercent(room: RoomDetail): number {
    const orig = (room as any).originalPrice;
    if (!orig || orig <= room.basePrice) return 0;
    return Math.round(((orig - room.basePrice) / orig) * 100);
  }

  getAmenityLabel(value: string): string {
    return this.amenityOptions.find((a) => a.value === value)?.label ?? value;
  }

  toggleAmenity(value: string, checked: boolean) {
    const current = this.form.value.amenities ?? [];
    if (checked && !current.includes(value)) {
      this.form.patchValue({ amenities: [...current, value] });
    } else if (!checked) {
      this.form.patchValue({ amenities: current.filter((a: string) => a !== value) });
    }
  }

  totalAvailable(): number {
    return this.rooms.reduce((s, r) => s + (r as any).available || 0, 0);
  }
  totalRooms(): number {
    return this.rooms.reduce((s, r) => s + (r.totalRooms || 0), 0);
  }
  countActive(): number {
    return this.rooms.filter((r) => r.active).length;
  }

  private toISO(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  private applyLocalFilters(source: RoomDetail[]): RoomDetail[] {
    const keyword = this.keyword.trim().toLowerCase();

    return source.filter((room) => {
      const matchesKeyword =
        !keyword ||
        room.name.toLowerCase().includes(keyword) ||
        (room.description ?? '').toLowerCase().includes(keyword);
      const matchesType = this.filterRoomType === 'all' || room.roomType === this.filterRoomType;
      const matchesActive =
        this.filterActive === 'all' ||
        (this.filterActive === 'active' ? room.active : !room.active);

      return matchesKeyword && matchesType && matchesActive;
    });
  }
}
