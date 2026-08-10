// ═══════════════════════════════════════════════════════════
// ROOMS PAGE — Phase E (API Integration)
// Host: GET /api/host/hotels → rooms, block dates
// Admin: read-only view
// ═══════════════════════════════════════════════════════════

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
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
  CatalogManagementService,
  RoomTypeItem,
} from '../../../core/services/catalog-management.service';
import {
  AMENITY_OPTIONS,
  BED_TYPE_OPTIONS,
  ROOM_TYPE_LABELS,
} from '../../../core/models/room-admin.model';
import { generateIdempotencyKey } from '../../../core/http/idempotency';

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
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Rooms implements OnInit {
  private roomService = inject(RoomService);
  private hotelService = inject(HotelService);
  private catalog = inject(CatalogManagementService);
  private auth = inject(Auth);
  private message = inject(NzMessageService);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  // ── Data ────────────────────────────────────────────────
  rooms: RoomDetail[] = [];
  hotels: Hotel[] = [];
  hotelOptions: Array<{ id: string; name: string }> = [];
  private hotelNameById = new Map<string, string>();
  roomTypes: RoomTypeItem[] = [];
  isLoading = false;
  isHost = false;

  // ── Filter ──────────────────────────────────────────────
  filterHotel: string | 'all' = 'all';
  filterRoomType: string | 'all' = 'all';
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
  private createRoomIdempotencyKey: string | null = null;
  private blockRoomIdempotencyKey: string | null = null;

  // ── Options ─────────────────────────────────────────────
  allRoomTypes: string[] = ['SINGLE', 'DOUBLE', 'SUITE', 'FAMILY'];
  roomTypeLabels: Record<string, string> = { ...ROOM_TYPE_LABELS };
  readonly amenityOptions = AMENITY_OPTIONS;
  readonly bedTypeOptions = BED_TYPE_OPTIONS;
  selectedAmenities = new Set<string>();
  roomStats = { available: 0, total: 0, active: 0 };

  // ── Form ────────────────────────────────────────────────
  form = this.fb.nonNullable.group({
    hotelId: ['', Validators.required],
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    roomType: ['SINGLE', Validators.required],
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
    this.loadRoomTypes();
    this.loadHotels();
  }

  // ── Load hotels → then rooms ────────────────────────────
  loadRoomTypes() {
    this.catalog.list<RoomTypeItem>('room-types', { active: true, page: 0, size: 100 }).subscribe({
      next: (data) => {
        this.roomTypes = data.content;
        const codes = data.content.map((item) => item.code);
        this.allRoomTypes = codes.length ? codes : this.allRoomTypes;
        for (const item of data.content) {
          this.roomTypeLabels[item.code] = item.name;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.roomTypes = [];
        this.cdr.markForCheck();
      },
    });
  }

  loadHotels() {
    if (this.isHost) {
      this.hotelService.getMyHotels().subscribe({
        next: (data) => {
          this.hotels = data;
          this.syncHotelLookups();
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
          this.syncHotelLookups();
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
      this.updateRoomStats();
      this.cdr.markForCheck();
      return;
    }

    const hotelIds =
      this.filterHotel === 'all'
        ? this.hotels.map((hotel) => hotel.id)
        : [this.filterHotel];

    this.selectedHotelId = hotelIds[0] ?? '';
    if (!this.selectedHotelId) return;

    this.isLoading = true;
    this.cdr.markForCheck();
    const roomRequests = hotelIds.map((hotelId) =>
      this.isHost
        ? this.roomService.getHostRooms(hotelId, { page: 0, size: 100 })
        : this.roomService.getAdminRooms(hotelId, { page: 0, size: 100 }),
    );

    forkJoin(roomRequests).subscribe({
      next: (roomGroups) => {
        this.rooms = this.applyLocalFilters(roomGroups.flat());
        this.updateRoomStats();
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.message.error('Không thể tải phòng');
        this.isLoading = false;
        this.cdr.markForCheck();
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
    this.createRoomIdempotencyKey = null;
    this.imagePreviews = [];
    this.selectedAmenities = new Set<string>();
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
    this.cdr.markForCheck();
  }

  openEditModal(room: RoomDetail) {
    this.editingRoom = room;
    this.imagePreviews = [...(room.images ?? [])];
    this.selectedAmenities = new Set(room.amenities ?? []);
    this.form.patchValue({
      hotelId: room.hotelId,
      name: room.name,
      description: room.description,
      roomType: room.roomType,
      bedType: room.bedType,
      size: room.size,
      maxAdults: room.maxAdults,
      maxChildren: room.maxChildren,
      pricePerNight: room.basePrice,
      totalRooms: room.totalRooms,
      amenities: [...(room.amenities ?? [])],
    });
    this.isFormModalOpen = true;
    this.cdr.markForCheck();
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
          this.cdr.markForCheck();
          this.loadRooms();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi cập nhật');
          this.isSubmitting = false;
          this.cdr.markForCheck();
        },
      });
    } else {
      this.createRoomIdempotencyKey = this.createRoomIdempotencyKey ?? generateIdempotencyKey();
      this.roomService.createRoom(raw.hotelId, payload, this.createRoomIdempotencyKey).subscribe({
        next: () => {
          this.message.success(`Đã tạo phòng "${raw.name}"`);
          this.createRoomIdempotencyKey = null;
          this.isFormModalOpen = false;
          this.isSubmitting = false;
          this.cdr.markForCheck();
          this.loadRooms();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi tạo phòng');
          if (err?.status !== 409) {
            this.createRoomIdempotencyKey = null;
          }
          this.isSubmitting = false;
          this.cdr.markForCheck();
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
    this.blockRoomIdempotencyKey = null;
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
    this.blockRoomIdempotencyKey = this.blockRoomIdempotencyKey ?? generateIdempotencyKey();
    this.roomService
      .blockDates(this.blockRoomId, {
        startDate: this.toISO(this.blockDates[0]),
        endDate: this.toISO(this.blockDates[1]),
      }, this.blockRoomIdempotencyKey)
      .subscribe({
        next: () => {
          this.message.success('Đã chặn ngày');
          this.blockRoomIdempotencyKey = null;
          this.blocking = false;
          this.blockRoomId = null;
          this.loadRooms();
        },
        error: () => {
          this.message.error('Không thể chặn ngày');
          this.blockRoomIdempotencyKey = null;
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
      this.cdr.markForCheck();
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeImage(index: number) {
    this.imagePreviews.splice(index, 1);
    this.cdr.markForCheck();
  }

  // ── Helpers ─────────────────────────────────────────────
  getHotelName(hotelId: string): string {
    return this.hotelNameById.get(hotelId) ?? hotelId;
  }

  getRoomTypeColor(type: string): string {
    const map: Record<string, string> = {
      SINGLE: 'default',
      DOUBLE: 'green',
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
    const next = new Set(this.selectedAmenities);
    if (checked) {
      next.add(value);
    } else {
      next.delete(value);
    }
    this.selectedAmenities = next;
    this.form.controls.amenities.setValue([...next], { emitEvent: false });
    this.cdr.markForCheck();
  }

  trackByRoom = (_: number, room: RoomDetail) => room.id;
  trackByHotel = (_: number, hotel: { id: string }) => hotel.id;
  trackByRoomType = (_: number, roomType: string) => roomType;
  trackByAmenity = (_: number, amenity: { value: string }) => amenity.value;
  trackByImage = (_: number, image: string) => image;

  private syncHotelLookups() {
    this.hotelOptions = this.hotels.map((h) => ({ id: h.id, name: h.name }));
    this.hotelNameById = new Map(this.hotelOptions.map((hotel) => [hotel.id, hotel.name]));
  }

  private updateRoomStats() {
    this.roomStats = this.rooms.reduce(
      (stats, room) => ({
        available: stats.available + ((room as any).available || 0),
        total: stats.total + (room.totalRooms || 0),
        active: stats.active + (room.active ? 1 : 0),
      }),
      { available: 0, total: 0, active: 0 },
    );
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
