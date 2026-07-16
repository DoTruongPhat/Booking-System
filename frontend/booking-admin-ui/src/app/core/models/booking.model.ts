// =====================================================
// BOOKING MODEL
// Cấu trúc đặt phòng — ALIGNED với backend Phase E
// =====================================================

// ── Backend chỉ có 5 status (không có CHECKED_IN, CHECKED_OUT) ──
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED' | 'PARTIALLY_REFUNDED';
export type PaymentMethod = 'CREDIT_CARD' | 'BANK_TRANSFER' | 'MOMO' | 'VNPAY' | 'PAY_AT_HOTEL';

export interface Booking {
  id: string;
  userId: string;
  hotelId: string;
  roomId: string;
  hotelName: string;
  hotelAddress: string;
  roomName: string;
  checkIn: string; // ISO date (checkInDate from backend)
  checkOut: string; // ISO date (checkOutDate from backend)
  nights: number;
  guests: {
    adults: number;
    children: number;
    childrenAges?: number[];
  };
  rooms: number; // numRooms
  pricePerNight: number;
  totalPrice: number;
  taxAmount: number;
  finalPrice: number;
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  guestInfo: GuestInfo;
  specialRequests?: string;
  createdAt: string;
  updatedAt: string;
  confirmedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  /** Phần trăm hoàn tiền (100%, 50%, 0%) — backend tính */
  refundPercent?: number;
  /** Số tiền hoàn — backend tính */
  refundAmount?: number;
}

export interface GuestInfo {
  fullName: string;
  email: string;
  phone: string;
  countryCode?: string;
  estimatedArrivalTime?: string;
}

export interface CreateBookingRequest {
  roomId: string;
  checkInDate: string;
  checkOutDate: string;
  numGuests: number;
  numRooms: number;
  specialRequest?: string;
  guestName: string;
  guestEmail: string;
  guestPhone?: string;
}

/** Request body cho POST /api/user/bookings/{id}/cancel */
export interface CancelBookingRequest {
  reason: string;
}

export interface BookingFilter {
  city?: string;
  checkIn?: string;
  checkOut?: string;
  guests?: number;
  priceMin?: number;
  priceMax?: number;
  minRating?: number;
  sortBy?: string;
  page?: number;
  size?: number;
}

// === STATUS LABELS (aligned với 5 status backend) ===
export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
};

export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  PENDING: 'orange',
  CONFIRMED: 'green',
  COMPLETED: 'blue',
  CANCELLED: 'red',
  NO_SHOW: 'red',
};

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  UNPAID: 'Chưa thanh toán',
  PAID: 'Đã thanh toán',
  REFUNDED: 'Đã hoàn tiền',
  PARTIALLY_REFUNDED: 'Hoàn tiền một phần',
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT_CARD: 'Thẻ tín dụng',
  BANK_TRANSFER: 'Chuyển khoản',
  MOMO: 'Ví MoMo',
  VNPAY: 'VNPay',
  PAY_AT_HOTEL: 'Thanh toán tại khách sạn',
};
