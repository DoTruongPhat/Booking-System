import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { BookingService } from '../../../core/services/booking.service';
import { Booking } from '../../../core/models/booking.model';

@Component({
  selector: 'app-booking-success',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzButtonModule,
    NzSpinModule,
    NavbarComponent,
  ],
  templateUrl: './success.component.html',
  styleUrl: './success.component.scss',
})
export class BookingSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private bookingService = inject(BookingService);
  private message = inject(NzMessageService);

  bookingId = '';
  checkIn = '';
  checkOut = '';
  guests = { adults: 2, children: 0 };
  roomName = '';
  hotelName = '';
  totalPrice = 0;
  loading = true;

  ngOnInit(): void {
    this.bookingId = this.route.snapshot.paramMap.get('id') || '';

    if (this.bookingId) {
      this.loadBooking();
    } else {
      this.loading = false;
    }
  }

  private loadBooking(): void {
    this.loading = true;
    this.bookingService.getBookingById(this.bookingId).subscribe({
      next: (booking: Booking) => {
        this.hotelName = booking.hotelName;
        this.roomName = booking.roomName;
        this.checkIn = booking.checkIn;
        this.checkOut = booking.checkOut;
        this.guests = booking.guests || { adults: 2, children: 0 };
        this.totalPrice = booking.finalPrice;
        this.loading = false;
      },
      error: () => {
        this.message.error('Không thể tải thông tin đặt phòng.');
        this.loading = false;
      },
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN').format(price) + 'đ';
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('vi-VN', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    });
  }

  copyBookingId(): void {
    navigator.clipboard.writeText(this.bookingId);
    this.message.success('Đã sao chép mã đặt phòng!');
  }
}
