import { Injectable } from '@angular/core';
import { Observable, of, delay, map } from 'rxjs';
import { Hotel, Room, Review, AmenityType } from '../models/hotel.model';
import { Booking, BookingFilter, SearchResult } from '../models/booking.model';

/**
 * HotelService - Mock service giả lập API
 * Sau này thay bằng HttpClient gọi BE thật
 */
@Injectable({ providedIn: 'root' })
export class HotelService {

  // === MOCK DATA - 8 khách sạn ===
  private mockHotels: Hotel[] = [
    {
      id: 'HT-001',
      name: 'Vinpearl Resort Nha Trang',
      nameEn: 'Vinpearl Resort Nha Trang',
      description: 'Khu nghỉ dưỡng 5 sao bên bờ biển Nha Trang với hồ bơi riêng, spa cao cấp và nhà hàng phục vụ ẩm thực địa phương.',
      address: 'Đảo Hòn Tre, Vinh Nguyên',
      city: 'Nha Trang',
      country: 'Việt Nam',
      starRating: 5,
      userRating: 9.2,
      reviewCount: 1234,
      images: [
        'https://images.unsplash.com/photo-1582719508461-419cfe7c7b41?w=800',
        'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=800',
        'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800',
        'https://images.unsplash.com/photo-1611892440504-42a792e24c32?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1582719508461-419cfe7c7b41?w=400',
      amenities: [AmenityType.WIFI, AmenityType.POOL, AmenityType.BREAKFAST, AmenityType.PARKING, AmenityType.GYM, AmenityType.SPA],
      pricePerNight: 4500000,
      originalPrice: 5500000,
      currency: 'VND',
      isFeatured: true,
      discountPercent: 18,
      rooms: this.createRooms('HT-001'),
      policies: {
        checkIn: '14:00',
        checkOut: '12:00',
        cancellation: 'Miễn phí hủy trước 24 giờ',
        children: 'Trẻ em dưới 6 tuổi miễn phí',
        pets: 'Không cho phép thú cư',
        smoking: 'Phòng không hút thuốc',
      },
      location: {
        latitude: 12.234,
        longitude: 109.197,
        district: 'Vinh Nguyên',
        nearbyAttractions: ['VinWonders', 'Hòn Tằm', 'Đảo Khỉ'],
      },
    },
    {
      id: 'HT-002',
      name: 'InterContinental Đà Nẵng Sun Peninsula Resort',
      description: 'Resort sang trọng với kiến trúc độc đáo và bãi biển riêng, lý tưởng cho kỳ nghỉ cao cấp.',
      address: 'Bãi Bắc, Sơn Trà',
      city: 'Đà Nẵng',
      country: 'Việt Nam',
      starRating: 5,
      userRating: 9.5,
      reviewCount: 892,
      images: [
        'https://images.unsplash.com/photo-1568084680786-84f342ef3dc7?w=800',
        'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800',
        'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1568084680786-84f342ef3dc7?w=400',
      amenities: [AmenityType.WIFI, AmenityType.POOL, AmenityType.BREAKFAST, AmenityType.SPA, AmenityType.GYM],
      pricePerNight: 6500000,
      currency: 'VND',
      isFeatured: true,
      rooms: this.createRooms('HT-002'),
      policies: {
        checkIn: '15:00',
        checkOut: '11:00',
        cancellation: 'Miễn phí hủy trước 48 giờ',
        children: 'Trẻ em từ 6-12 tuổi tính 50%',
        pets: 'Cho phép thú cư nhỏ',
        smoking: 'Có khu vực hút thuốc riêng',
      },
      location: {
        latitude: 16.117,
        longitude: 108.272,
        district: 'Sơn Trà',
        nearbyAttractions: ['Bãi Bắc', 'Núi Sơn Trà', 'Ngũ Hành Sơn'],
      },
    },
    {
      id: 'HT-003',
      name: 'Hanoi La Siesta Hotel & Spa',
      description: 'Khách sạn boutique phong cách Á Đông giữa lòng Hà Nội cổ kính, gần Hồ Gươm.',
      address: '94 Mã Mây, Hoàn Kiếm',
      city: 'Hà Nội',
      country: 'Việt Nam',
      starRating: 4,
      userRating: 8.9,
      reviewCount: 567,
      images: [
        'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800',
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=400',
      amenities: [AmenityType.WIFI, AmenityType.BREAKFAST, AmenityType.SPA, AmenityType.GYM],
      pricePerNight: 2500000,
      currency: 'VND',
      rooms: this.createRooms('HT-003'),
      policies: {
        checkIn: '14:00',
        checkOut: '12:00',
        cancellation: 'Miễn phí hủy trước 24 giờ',
        children: 'Trẻ em dưới 12 tuổi miễn phí',
        pets: 'Không cho phép thú cư',
        smoking: 'Phòng không hút thuốc',
      },
      location: {
        latitude: 21.035,
        longitude: 105.852,
        district: 'Hoàn Kiếm',
        nearbyAttractions: ['Hồ Gươm', 'Phố cổ', 'Đền Quán Thánh'],
      },
    },
    {
      id: 'HT-004',
      name: 'The Reverie Saigon',
      description: 'Khách sạn sang trọng nhất Sài Gòn với thiết kế Ý đương đại và tầm nhìn panorama thành phố.',
      address: '22-36 Nguyễn Huệ, Quận 1',
      city: 'TP. Hồ Chí Minh',
      country: 'Việt Nam',
      starRating: 5,
      userRating: 9.3,
      reviewCount: 1456,
      images: [
        'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=800',
        'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?w=400',
      amenities: [AmenityType.WIFI, AmenityType.POOL, AmenityType.BREAKFAST, AmenityType.SPA, AmenityType.GYM],
      pricePerNight: 5800000,
      originalPrice: 7000000,
      currency: 'VND',
      isFeatured: true,
      discountPercent: 17,
      rooms: this.createRooms('HT-004'),
      policies: {
        checkIn: '14:00',
        checkOut: '12:00',
        cancellation: 'Miễn phí hủy trước 48 giờ',
        children: 'Trẻ em dưới 6 tuổi miễn phí',
        pets: 'Không cho phép thú cư',
        smoking: 'Phòng không hút thuốc',
      },
      location: {
        latitude: 10.774,
        longitude: 106.701,
        district: 'Quận 1',
        nearbyAttractions: ['Nhà thờ Đức Bà', 'Bưu điện Trung tâm', 'Phố đi bộ Nguyễn Huệ'],
      },
    },
    {
      id: 'HT-005',
      name: 'Anantara Hoi An Resort',
      description: 'Resort bên sông Thu Bồn với kiến trúc pha trộn Việt-Pháp, gần phố cổ Hội An.',
      address: '1 Phạm Hồng Thái, Cẩm Châu',
      city: 'Hội An',
      country: 'Việt Nam',
      starRating: 5,
      userRating: 9.0,
      reviewCount: 678,
      images: [
        'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?w=800',
        'https://images.unsplash.com/photo-1571896349842-33c89424de2d?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?w=400',
      amenities: [AmenityType.WIFI, AmenityType.POOL, AmenityType.BREAKFAST, AmenityType.SPA],
      pricePerNight: 4200000,
      currency: 'VND',
      rooms: this.createRooms('HT-005'),
      policies: {
        checkIn: '14:00',
        checkOut: '12:00',
        cancellation: 'Miễn phí hủy trước 24 giờ',
        children: 'Trẻ em dưới 6 tuổi miễn phí',
        pets: 'Không cho phép thú cư',
        smoking: 'Phòng không hút thuốc',
      },
      location: {
        latitude: 15.877,
        longitude: 108.336,
        district: 'Cẩm Châu',
        nearbyAttractions: ['Phố cổ Hội An', 'Chùa Cầu', 'Bãi biển An Bàng'],
      },
    },
    {
      id: 'HT-006',
      name: 'JW Marriott Phu Quoc',
      description: 'Khu nghỉ dưỡng biển 5 sao với hồ bơi riêng, sân golf và spa đẳng cấp quốc tế.',
      address: 'Bãi Khem, An Thới',
      city: 'Phú Quốc',
      country: 'Việt Nam',
      starRating: 5,
      userRating: 9.4,
      reviewCount: 967,
      images: [
        'https://images.unsplash.com/photo-1540541338287-417002247dee?w=800',
        'https://images.unsplash.com/photo-1568084680786-84f342ef3dc7?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1540541338287-417002247dee?w=400',
      amenities: [AmenityType.WIFI, AmenityType.POOL, AmenityType.BREAKFAST, AmenityType.GYM, AmenityType.SPA, AmenityType.PARKING],
      pricePerNight: 7800000,
      originalPrice: 9500000,
      currency: 'VND',
      isFeatured: true,
      discountPercent: 18,
      rooms: this.createRooms('HT-006'),
      policies: {
        checkIn: '15:00',
        checkOut: '11:00',
        cancellation: 'Miễn phí hủy trước 72 giờ',
        children: 'Trẻ em dưới 12 tuổi miễn phí',
        pets: 'Cho phép thú cư',
        smoking: 'Có khu vực riêng',
      },
      location: {
        latitude: 10.012,
        longitude: 103.985,
        district: 'An Thới',
        nearbyAttractions: ['Bãi Khem', 'VinWonders Phú Quốc', 'Hòn Thơm'],
      },
    },
    {
      id: 'HT-007',
      name: 'M Boutique Hotel',
      description: 'Khách sạn boutique hiện đại, phù hợp cho cặp đôi hoặc khách công tác với giá hợp lý.',
      address: '15 Bùi Thị Xuân, Quận 1',
      city: 'TP. Hồ Chí Minh',
      country: 'Việt Nam',
      starRating: 3,
      userRating: 8.2,
      reviewCount: 234,
      images: [
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=400',
      amenities: [AmenityType.WIFI, AmenityType.AIR_CONDITIONING, AmenityType.TV],
      pricePerNight: 950000,
      currency: 'VND',
      rooms: this.createRooms('HT-007'),
      policies: {
        checkIn: '14:00',
        checkOut: '12:00',
        cancellation: 'Miễn phí hủy trước 24 giờ',
        children: 'Trẻ em tính phí 100%',
        pets: 'Không cho phép',
        smoking: 'Phòng không hút thuốc',
      },
      location: {
        latitude: 10.762,
        longitude: 106.689,
        district: 'Quận 1',
        nearbyAttractions: ['Chợ Bến Thành', 'Phố Tây Bùi Viện'],
      },
    },
    {
      id: 'HT-008',
      name: 'Sapa Mountain Lodge',
      description: 'Lodge ấm cúng giữa núi rừng Tây Bắc với view ruộng bậc thang và sương mù buổi sáng.',
      address: 'Bản Cát Cát, Sapa',
      city: 'Sa Pa',
      country: 'Việt Nam',
      starRating: 4,
      userRating: 8.7,
      reviewCount: 412,
      images: [
        'https://images.unsplash.com/photo-1571401835393-8c5f35328320?w=800',
        'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?w=800',
      ],
      thumbnail: 'https://images.unsplash.com/photo-1571401835393-8c5f35328320?w=400',
      amenities: [AmenityType.WIFI, AmenityType.BREAKFAST, AmenityType.SPA, AmenityType.PARKING],
      pricePerNight: 1800000,
      currency: 'VND',
      rooms: this.createRooms('HT-008'),
      policies: {
        checkIn: '14:00',
        checkOut: '11:00',
        cancellation: 'Miễn phí hủy trước 48 giờ',
        children: 'Trẻ em dưới 6 tuổi miễn phí',
        pets: 'Cho phép thú cư nhỏ',
        smoking: 'Có khu vực riêng',
      },
      location: {
        latitude: 22.336,
        longitude: 103.844,
        district: 'Sa Pa',
        nearbyAttractions: ['Bản Cát Cát', 'Fansipan', 'Núi Hàm Rồng'],
      },
    },
  ];

  // === Helper: tạo phòng mẫu cho mỗi khách sạn ===
  private createRooms(hotelId: string): Room[] {
    return [
      {
        id: `${hotelId}-R1`,
        hotelId,
        name: 'Deluxe Room',
        description: 'Phòng deluxe sang trọng với view thành phố',
        capacity: 2,
        bedType: '1 King Bed',
        size: 32,
        pricePerNight: 1500000,
        available: 5,
        images: ['https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=600'],
        amenities: [AmenityType.WIFI, AmenityType.AIR_CONDITIONING, AmenityType.TV],
        maxAdults: 2,
        maxChildren: 1,
        breakfastIncluded: true,
        freeCancellation: true,
        payLater: true,
      },
      {
        id: `${hotelId}-R2`,
        hotelId,
        name: 'Suite Ocean View',
        description: 'Phòng suite rộng rãi với view biển tuyệt đẹp',
        capacity: 4,
        bedType: '2 Queen Beds',
        size: 55,
        pricePerNight: 2800000,
        available: 3,
        images: ['https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=600'],
        amenities: [AmenityType.WIFI, AmenityType.AIR_CONDITIONING, AmenityType.TV, AmenityType.MINIBAR, AmenityType.SAFE],
        maxAdults: 4,
        maxChildren: 2,
        breakfastIncluded: true,
        freeCancellation: true,
        payLater: false,
      },
      {
        id: `${hotelId}-R3`,
        hotelId,
        name: 'Family Room',
        description: 'Phòng gia đình rộng rãi, phù hợp cho 4-5 người',
        capacity: 5,
        bedType: '1 King + 1 Single',
        size: 45,
        pricePerNight: 3500000,
        available: 2,
        images: ['https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?w=600'],
        amenities: [AmenityType.WIFI, AmenityType.AIR_CONDITIONING, AmenityType.TV, AmenityType.MINIBAR],
        maxAdults: 3,
        maxChildren: 2,
        breakfastIncluded: false,
        freeCancellation: false,
        payLater: true,
      },
    ];
  }

  // ======================================
  // PUBLIC API
  // ======================================

  /**
   * Tìm kiếm khách sạn theo filter
   */
  searchHotels(filter: BookingFilter): Observable<SearchResult> {
    let results = [...this.mockHotels];

    // Filter theo city
    if (filter.city) {
      const cityLower = filter.city.toLowerCase();
      results = results.filter(h => h.city.toLowerCase().includes(cityLower));
    }

    // Filter theo giá
    if (filter.priceMin !== undefined) {
      results = results.filter(h => h.pricePerNight >= filter.priceMin!);
    }
    if (filter.priceMax !== undefined) {
      results = results.filter(h => h.pricePerNight <= filter.priceMax!);
    }

    // Filter theo số sao
    if (filter.starRatings && filter.starRatings.length > 0) {
      results = results.filter(h => filter.starRatings!.includes(h.starRating));
    }

    // Filter theo tiện nghi
    if (filter.amenities && filter.amenities.length > 0) {
      results = results.filter(h =>
        filter.amenities!.every(a => h.amenities.includes(a as AmenityType))
      );
    }

    // Sort
    switch (filter.sortBy) {
      case 'price_low':
        results.sort((a, b) => a.pricePerNight - b.pricePerNight);
        break;
      case 'price_high':
        results.sort((a, b) => b.pricePerNight - a.pricePerNight);
        break;
      case 'rating_high':
        results.sort((a, b) => b.userRating - a.userRating);
        break;
      case 'star_rating':
        results.sort((a, b) => b.starRating - a.starRating);
        break;
      default: // recommended
        results.sort((a, b) => b.userRating - a.userRating);
    }

    // Simulate network delay
    return of({
      hotels: results,
      total: results.length,
      page: 1,
      pageSize: 20,
      filters: filter,
    }).pipe(delay(300));
  }

  /**
   * Lấy chi tiết 1 khách sạn
   */
  getHotelById(id: string): Observable<Hotel | null> {
    const hotel = this.mockHotels.find(h => h.id === id) || null;
    return of(hotel).pipe(delay(200));
  }

  /**
   * Lấy reviews của khách sạn
   */
  getHotelReviews(hotelId: string): Observable<Review[]> {
    const mockReviews: Review[] = [
      {
        id: 'R-001',
        hotelId,
        userId: 'U-101',
        username: 'Nguyễn Văn A',
        rating: 9.5,
        title: 'Tuyệt vời!',
        comment: 'Phòng rộng rãi, view đẹp, nhân viên nhiệt tình. Sẽ quay lại.',
        createdAt: '2026-05-15',
        helpful: 23,
      },
      {
        id: 'R-002',
        hotelId,
        userId: 'U-102',
        username: 'Trần Thị B',
        rating: 8.5,
        title: 'Rất tốt',
        comment: 'Vị trí thuận tiện, gần trung tâm. Bữa sáng ngon.',
        createdAt: '2026-05-20',
        helpful: 15,
      },
      {
        id: 'R-003',
        hotelId,
        userId: 'U-103',
        username: 'Lê Văn C',
        rating: 9.0,
        title: 'Đáng tiền',
        comment: 'Giá hợp lý, phòng sạch đẹp. Sẽ giới thiệu bạn bè.',
        createdAt: '2026-05-25',
        helpful: 8,
      },
    ];
    return of(mockReviews).pipe(delay(200));
  }

  /**
   * Lấy danh sách khách sạn nổi bật
   */
  getFeaturedHotels(): Observable<Hotel[]> {
    const featured = this.mockHotels.filter(h => h.isFeatured);
    return of(featured).pipe(delay(200));
  }

  /**
   * Lấy danh sách điểm đến (unique cities)
   */
  getDestinations(): Observable<{ name: string; image: string; count: number }[]> {
    const cities = [
      { name: 'Nha Trang', image: 'https://images.unsplash.com/photo-1559592413-7c4d2b1c3b73?w=400' },
      { name: 'Đà Nẵng', image: 'https://images.unsplash.com/photo-1573270689103-d7a4e42b7498?w=400' },
      { name: 'Hà Nội', image: 'https://images.unsplash.com/photo-1528127269322-539801943592?w=400' },
      { name: 'TP. Hồ Chí Minh', image: 'https://images.unsplash.com/photo-1583417319070-4a69db38a482?w=400' },
      { name: 'Phú Quốc', image: 'https://images.unsplash.com/photo-1540541338287-417002247dee?w=400' },
      { name: 'Hội An', image: 'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?w=400' },
      { name: 'Sa Pa', image: 'https://images.unsplash.com/photo-1571401835393-8c5f35328320?w=400' },
    ];
    return of(cities.map(c => ({ ...c, count: 0 }))).pipe(delay(100));
  }
}
