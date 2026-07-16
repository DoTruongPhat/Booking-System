// ═══════════════════════════════════════════════════════════
// ADMIN BOOKING MODEL — Phase E (Aligned with backend)
// Mở rộng Booking gốc thêm fields cho admin/host view
//
// LƯU Ý: SEED DATA ĐÃ BỎ — dùng API thật thay vì mock
// ═══════════════════════════════════════════════════════════

import { Booking, BookingStatus } from './booking.model';

export interface AdminBooking extends Booking {
  /** ID của host sở hữu hotel này — dùng để filter scope cho HOST role */
  hotelOwnerId?: string;
  /** Email khách hàng (denormalize) */
  userEmail?: string;
  /** SĐT khách hàng */
  userPhone?: string;
  /** Ghi chú của host */
  hostNote?: string;
  /** Lý do hủy (nếu status = CANCELLED) */
  cancelReason?: string;
  /** Lịch sử thay đổi status */
  timeline?: BookingTimelineEntry[];
}

export interface BookingTimelineEntry {
  id: string;
  fromStatus: BookingStatus | null;
  toStatus: BookingStatus;
  changedBy: string;
  changedByName: string;
  changedAt: string;
  note?: string;
}
