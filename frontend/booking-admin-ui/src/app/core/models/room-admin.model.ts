// ═══════════════════════════════════════════════════════════
// ROOM ADMIN MODEL (A.5)
// Model cho trang quản lý Rooms (flat table)
// Cùng data với A.6 (Hotel Rooms master-detail)
// ═══════════════════════════════════════════════════════════

export type RoomType = string;

export const ROOM_TYPE_LABELS: Record<string, string> = {
  SINGLE: 'Single',
  DOUBLE: 'Double',
  STANDARD: 'Standard',
  DELUXE: 'Deluxe',
  SUITE: 'Suite',
  FAMILY: 'Family',
  PRESIDENTIAL: 'Presidential',
  VILLA: 'Villa',
};

export interface AdminRoom {
  id: string;
  hotelId: string; // FK tới AdminHotel
  name: string;
  description: string;
  roomType: RoomType;
  bedType: string; // '1 King Bed', '2 Queen Beds', ...
  size: number; // m²
  maxAdults: number;
  maxChildren: number;
  pricePerNight: number; // VND
  originalPrice?: number; // giá gốc (nếu có discount)
  available: number; // số phòng còn
  totalRooms: number; // tổng số phòng
  images: string[]; // base64 hoặc URL
  amenities: string[]; // ['wifi', 'ac', 'tv', ...]
  breakfastIncluded: boolean;
  freeCancellation: boolean;
  payLater: boolean; // trả tại khách sạn
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export const AMENITY_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'wifi', label: 'WiFi miễn phí' },
  { value: 'air-conditioning', label: 'Điều hòa' },
  { value: 'tv', label: 'TV' },
  { value: 'minibar', label: 'Minibar' },
  { value: 'safe', label: 'Két an toàn' },
  { value: 'bathtub', label: 'Bồn tắm' },
  { value: 'shower', label: 'Vòi sen' },
  { value: 'hairdryer', label: 'Máy sấy tóc' },
  { value: 'desk', label: 'Bàn làm việc' },
  { value: 'balcony', label: 'Ban công' },
  { value: 'sea-view', label: 'View biển' },
  { value: 'city-view', label: 'View thành phố' },
  { value: 'pool-view', label: 'View hồ bơi' },
  { value: 'kitchen', label: 'Bếp' },
  { value: 'washing-machine', label: 'Máy giặt' },
];

export const BED_TYPE_OPTIONS: string[] = [
  '1 King Bed',
  '2 Queen Beds',
  '1 King + 1 Single',
  '2 Single Beds',
  '1 Queen Bed',
  '3 Single Beds',
  '1 King Bed + Sofa',
];

// ═══ SEED DATA (8 phòng cho 3 hotel) ═══
export const ROOM_SEED: AdminRoom[] = [
  // Vinpearl Nha Trang (HT-001)
  {
    id: 'R-001',
    hotelId: 'HT-001',
    name: 'Deluxe Ocean View',
    description:
      'Phòng deluxe view biển, ban công riêng, diện tích 45m². Bao gồm bữa sáng và minibar miễn phí.',
    roomType: 'DELUXE',
    bedType: '1 King Bed',
    size: 45,
    maxAdults: 2,
    maxChildren: 1,
    pricePerNight: 4500000,
    originalPrice: 5500000,
    available: 5,
    totalRooms: 10,
    images: [
      'https://images.unsplash.com/photo-1582719508461-419cfe7c7b41?w=400',
      'https://images.unsplash.com/photo-1611892440504-42a792e24c32?w=400',
    ],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'balcony',
      'sea-view',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: false,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-002',
    hotelId: 'HT-001',
    name: 'Suite Ocean View',
    description:
      'Suite cao cấp view biển, phòng khách riêng, diện tích 80m². Bao gồm butler service và welcome drink.',
    roomType: 'SUITE',
    bedType: '1 King Bed + Sofa',
    size: 80,
    maxAdults: 3,
    maxChildren: 2,
    pricePerNight: 6500000,
    originalPrice: 8000000,
    available: 3,
    totalRooms: 5,
    images: ['https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=400'],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'balcony',
      'sea-view',
      'desk',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: false,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-003',
    hotelId: 'HT-001',
    name: 'Family Room',
    description:
      'Phòng gia đình rộng rãi, 2 giường queen, diện tích 60m². Phù hợp cho gia đình 4 người.',
    roomType: 'FAMILY',
    bedType: '2 Queen Beds',
    size: 60,
    maxAdults: 4,
    maxChildren: 2,
    pricePerNight: 5500000,
    originalPrice: 6500000,
    available: 4,
    totalRooms: 8,
    images: ['https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=400'],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'balcony',
      'sea-view',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: true,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-004',
    hotelId: 'HT-002',
    name: 'Beach Villa',
    description:
      'Biệt thự mặt biển với hồ bơi riêng, diện tích 200m². Bao gồm bữa sáng, butler, xe đưa đón sân bay.',
    roomType: 'VILLA',
    bedType: '2 King Beds',
    size: 200,
    maxAdults: 6,
    maxChildren: 3,
    pricePerNight: 12000000,
    originalPrice: 15000000,
    available: 2,
    totalRooms: 3,
    images: ['https://images.unsplash.com/photo-1568084680786-84f342ef3dc7?w=400'],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'balcony',
      'sea-view',
      'pool-view',
      'kitchen',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: false,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-005',
    hotelId: 'HT-002',
    name: 'Deluxe Sea View',
    description: 'Phòng deluxe view biển, ban công lớn, diện tích 50m².',
    roomType: 'DELUXE',
    bedType: '1 King Bed',
    size: 50,
    maxAdults: 2,
    maxChildren: 1,
    pricePerNight: 8000000,
    originalPrice: 9500000,
    available: 6,
    totalRooms: 12,
    images: ['https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=400'],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'balcony',
      'sea-view',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: false,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  // Hanoi La Siesta (HT-003)
  {
    id: 'R-006',
    hotelId: 'HT-003',
    name: 'Classic Room',
    description: 'Phòng classic phong cách Á Đông, diện tích 30m², nằm giữa lòng phố cổ Hà Nội.',
    roomType: 'STANDARD',
    bedType: '1 Queen Bed',
    size: 30,
    maxAdults: 2,
    maxChildren: 0,
    pricePerNight: 2500000,
    originalPrice: 3000000,
    available: 8,
    totalRooms: 15,
    images: ['https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=400'],
    amenities: ['wifi', 'air-conditioning', 'tv', 'safe', 'shower', 'desk'],
    breakfastIncluded: true,
    freeCancellation: false,
    payLater: true,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-007',
    hotelId: 'HT-003',
    name: 'Suite',
    description: 'Suite phong cách boutique, phòng khách riêng, diện tích 55m².',
    roomType: 'SUITE',
    bedType: '1 King Bed + Sofa',
    size: 55,
    maxAdults: 3,
    maxChildren: 1,
    pricePerNight: 4000000,
    originalPrice: 4800000,
    available: 3,
    totalRooms: 5,
    images: ['https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400'],
    amenities: [
      'wifi',
      'air-conditioning',
      'tv',
      'minibar',
      'safe',
      'bathtub',
      'desk',
      'city-view',
    ],
    breakfastIncluded: true,
    freeCancellation: true,
    payLater: true,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
  {
    id: 'R-008',
    hotelId: 'HT-001',
    name: 'Standard Garden View',
    description: 'Phòng standard view vườn, diện tích 32m². Phù hợp cho cặp đôi hoặc đi công tác.',
    roomType: 'STANDARD',
    bedType: '1 Queen Bed',
    size: 32,
    maxAdults: 2,
    maxChildren: 0,
    pricePerNight: 3200000,
    available: 7,
    totalRooms: 12,
    images: ['https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=400'],
    amenities: ['wifi', 'air-conditioning', 'tv', 'safe', 'shower', 'desk'],
    breakfastIncluded: false,
    freeCancellation: false,
    payLater: true,
    active: true,
    createdAt: '2026-01-15T00:00:00Z',
    updatedAt: '2026-01-15T00:00:00Z',
  },
];
