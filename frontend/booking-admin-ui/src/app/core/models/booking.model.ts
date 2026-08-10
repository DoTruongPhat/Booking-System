export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';
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
  roomImages?: string[];
  hotelImages?: string[];
  imageUrl?: string;
  checkIn: string;
  checkOut: string;
  nights: number;
  guests: {
    adults: number;
    children: number;
    childrenAges?: number[];
  };
  rooms: number;
  pricePerNight: number;
  discountAmount?: number;
  voucherCode?: string;
  totalPrice: number;
  taxAmount: number;
  finalPrice: number;
  status: BookingStatus;
  paymentStatus: PaymentStatus;
  paymentMethod?: PaymentMethod;
  paymentExpiresAt?: string | null;
  guestInfo: GuestInfo;
  specialRequests?: string;
  createdAt: string;
  updatedAt: string;
  confirmedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  refundPercent?: number;
  refundAmount?: number | null;
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
  voucherCode?: string;
}

export interface VoucherValidation {
  valid: boolean;
  message: string;
  voucherId?: string;
  code?: string;
  discountType?: 'PERCENT' | 'FIXED';
  discountValue?: number;
  discountAmount: number;
}

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

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  CHECKED_IN: 'Đã nhận phòng',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
};

export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  PENDING: 'orange',
  CONFIRMED: 'green',
  CHECKED_IN: 'blue',
  COMPLETED: 'green',
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
  MOMO: 'Vi MoMo',
  VNPAY: 'VNPay',
  PAY_AT_HOTEL: 'Thanh toán tại khách sạn',
};
