// =====================================================
// HOTEL MODEL
// Cấu trúc dữ liệu khách sạn (tương tự Booking.com)
// =====================================================

export interface Hotel {
  id: string;
  name: string;
  nameEn?: string;
  description: string;
  address: string;
  city: string;
  country: string;
  starRating: number;           // 1-5 sao
  userRating: number;           // 8.5, 9.2 (điểm đánh giá user)
  reviewCount: number;
  images: string[];             // Danh sách URL ảnh
  thumbnail: string;            // Ảnh đại diện
  amenities: string[];           // ['wifi', 'pool', 'breakfast', 'parking', ...]
  pricePerNight: number;        // Giá thấp nhất (VND)
  originalPrice?: number;       // Giá gốc (nếu có sale)
  currency: string;             // 'VND'
  rooms: Room[];                // Danh sách phòng
  policies: HotelPolicies;
  location: {
    latitude: number;
    longitude: number;
    district: string;           // Quận/Huyện
    nearbyAttractions?: string[];
  };
  isFeatured?: boolean;         // Khách sạn nổi bật
  discountPercent?: number;     // % giảm giá (nếu có)
}

export interface Room {
  id: string;
  hotelId: string;
  name: string;                 // 'Deluxe Room', 'Suite Ocean View', ...
  description: string;
  capacity: number;             // Số người tối đa
  bedType: string;              // '1 King Bed', '2 Queen Beds', ...
  size: number;                 // m²
  pricePerNight: number;
  originalPrice?: number;
  available: number;             // Số phòng còn trống
  images: string[];
  amenities: string[];           // ['air-conditioning', 'tv', 'minibar', 'safe', ...]
  maxAdults: number;
  maxChildren: number;
  breakfastIncluded: boolean;
  freeCancellation: boolean;
  payLater: boolean;            // Trả tại khách sạn
}

export interface HotelPolicies {
  checkIn: string;              // '14:00'
  checkOut: string;             // '12:00'
  cancellation: string;         // Mô tả
  children: string;             // Chính sách trẻ em
  pets: string;                 // Chính sách thú cư
  smoking: string;              // Chính sách hút thuốc
}

export interface Review {
  id: string;
  hotelId: string;
  userId: string;
  username: string;
  userAvatar?: string;
  rating: number;               // 1-10
  title: string;
  comment: string;
  createdAt: string;
  helpful: number;
  photos?: string[];
}

// === ENUMS / CONSTANTS ===
export enum AmenityType {
  WIFI = 'wifi',
  POOL = 'pool',
  BREAKFAST = 'breakfast',
  PARKING = 'parking',
  GYM = 'gym',
  SPA = 'spa',
  AIR_CONDITIONING = 'air-conditioning',
  TV = 'tv',
  MINIBAR = 'minibar',
  SAFE = 'safe',
  PETS_ALLOWED = 'pets-allowed',
}

export const AMENITY_LABELS: Record<AmenityType, string> = {
  [AmenityType.WIFI]: 'WiFi miễn phí',
  [AmenityType.POOL]: 'Hồ bơi',
  [AmenityType.BREAKFAST]: 'Bữa sáng',
  [AmenityType.PARKING]: 'Bãi đỗ xe',
  [AmenityType.GYM]: 'Phòng gym',
  [AmenityType.SPA]: 'Spa',
  [AmenityType.AIR_CONDITIONING]: 'Điều hòa',
  [AmenityType.TV]: 'TV',
  [AmenityType.MINIBAR]: 'Minibar',
  [AmenityType.SAFE]: 'Két an toàn',
  [AmenityType.PETS_ALLOWED]: 'Cho phép thú cư',
};

export enum HotelSortOption {
  RECOMMENDED = 'recommended',
  PRICE_LOW = 'price_low',
  PRICE_HIGH = 'price_high',
  RATING_HIGH = 'rating_high',
  STAR_RATING = 'star_rating',
}
