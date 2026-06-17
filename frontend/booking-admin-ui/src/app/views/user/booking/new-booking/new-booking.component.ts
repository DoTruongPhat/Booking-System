import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';

@Component({
  selector: 'app-new-booking',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NzCardModule,
    NzIconModule,
    NzButtonModule,
    NzFormModule,
    NzInputModule,
    NzDatePickerModule,
    NzInputNumberModule,
  ],
  templateUrl: './new-booking.component.html',
  styleUrl: './new-booking.component.scss',
})
export class NewBookingComponent {
  // Search params
  destination = '';
  checkIn: Date | null = null;
  checkOut: Date | null = null;
  guests = 1;
  rooms = 1;

  // Mock data
  results = [
    {
      id: 1,
      name: 'Vinpearl Resort Nha Trang',
      location: 'Nha Trang, Khánh Hòa',
      rating: 4.8,
      reviews: 1234,
      price: 1500000,
      image: 'https://via.placeholder.com/300x200/0064D2/fff?text=Hotel+1',
    },
    {
      id: 2,
      name: 'InterContinental Đà Nẵng',
      location: 'Đà Nẵng',
      rating: 4.7,
      reviews: 892,
      price: 2500000,
      image: 'https://via.placeholder.com/300x200/FF6B00/fff?text=Hotel+2',
    },
  ];

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price) + ' đ/đêm';
  }
}
