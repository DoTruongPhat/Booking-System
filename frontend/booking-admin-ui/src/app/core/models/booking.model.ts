// =====================================================
// BOOKING MODEL
// Cấu trúc đặt phòng
// =====================================================

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'CANCELLED' | 'NO_SHOW';
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
  checkIn: string;              // ISO date
  checkOut: string;
  nights: number;
  guests: {
    adults: number;
    children: number;
    childrenAges?: number[];
  };
  rooms: number;                // Số phòng đặt
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
}

export interface GuestInfo {
  fullName: string;
  email: string;
  phone: string;
  countryCode?: string;         // +84, +1, ...
  estimatedArrivalTime?: string; // '14:00 - 18:00'
}

export interface BookingFilter {
  city?: string;
  checkIn?: string;
  checkOut?: string;
  guests?: {
    adults: number;
    children: number;
  };
  priceMin?: number;
  priceMax?: number;
  starRatings?: number[];        // [3, 4, 5]
  amenities?: string[];          // ['wifi', 'pool', ...]
  sortBy?: string;               // 'recommended' | 'price_low' | 'rating_high'
}

export interface SearchResult {
  hotels: import('./hotel.model').Hotel[];
  total: number;
  page: number;
  pageSize: number;
  filters: BookingFilter;
}

// === STATUS LABELS ===
export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  CHECKED_IN: 'Đã nhận phòng',
  CHECKED_OUT: 'Đã trả phòng',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
};

export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  PENDING: 'orange',
  CONFIRMED: 'green',
  CHECKED_IN: 'blue',
  CHECKED_OUT: 'default',
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
