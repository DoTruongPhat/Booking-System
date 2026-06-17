import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { Auth } from '../../../../core/services/auth';

interface Booking {
  id: string;
  hotelName: string;
  location: string;
  image: string;
  checkIn: string;
  checkOut: string;
  guests: number;
  rooms: number;
  totalPrice: number;
  status: 'CONFIRMED' | 'PENDING' | 'CANCELLED' | 'COMPLETED';
  bookedAt: string;
}

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzCardModule,
    NzIconModule,
    NzButtonModule,
    NzTagModule,
    NzEmptyModule,
  ],
  templateUrl: './my-bookings.component.html',
  styleUrl: './my-bookings.component.scss',
})
export class MyBookingsComponent implements OnInit {
  user: any;

  // Mock data - sẽ gọi API sau
  bookings: Booking[] = [
    {
      id: 'BK-2025-001',
      hotelName: 'Vinpearl Resort Nha Trang',
      location: 'Nha Trang, Khánh Hòa',
      image: 'https://via.placeholder.com/200x140/0064D2/fff?text=Hotel',
      checkIn: '2026-07-15',
      checkOut: '2026-07-18',
      guests: 2,
      rooms: 1,
      totalPrice: 4500000,
      status: 'CONFIRMED',
      bookedAt: '2026-06-10',
    },
    {
      id: 'BK-2025-002',
      hotelName: 'InterContinental Đà Nẵng',
      location: 'Đà Nẵng',
      image: 'https://via.placeholder.com/200x140/FF6B00/fff?text=Hotel',
      checkIn: '2026-08-05',
      checkOut: '2026-08-08',
      guests: 4,
      rooms: 2,
      totalPrice: 12500000,
      status: 'PENDING',
      bookedAt: '2026-06-15',
    },
  ];

  constructor(private auth: Auth) {
    this.user = this.auth.getUser();
  }

  ngOnInit(): void {
    // TODO: Gọi API lấy bookings của user
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'success';
      case 'PENDING': return 'processing';
      case 'CANCELLED': return 'error';
      case 'COMPLETED': return 'default';
      default: return 'default';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'Đã xác nhận';
      case 'PENDING': return 'Đang xử lý';
      case 'CANCELLED': return 'Đã hủy';
      case 'COMPLETED': return 'Hoàn thành';
      default: return status;
    }
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price) + ' đ';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }
}
