import { Injectable } from '@angular/core';
import { Observable, of, delay } from 'rxjs';
import { Booking, BookingStatus, GuestInfo, PaymentMethod } from '../models/booking.model';
import { Auth } from './auth';

/**
 * BookingService - Mock service quản lý bookings
 * Lưu trong localStorage để giữ data khi refresh
 */
@Injectable({ providedIn: 'root' })
export class BookingService {

  private readonly STORAGE_KEY = 'smartbooking_bookings';

  constructor(private auth: Auth) {}

  // ======================================
  // CRUD BOOKINGS
  // ======================================

  /**
   * Tạo booking mới
   */
  createBooking(booking: Omit<Booking, 'id' | 'userId' | 'createdAt' | 'updatedAt' | 'status' | 'paymentStatus'>): Observable<Booking> {
    const user = this.auth.getUser();
    if (!user) {
      throw new Error('User not logged in');
    }

    const nights = this.calculateNights(booking.checkIn, booking.checkOut);
    const totalPrice = booking.pricePerNight * booking.rooms * nights;
    const taxAmount = Math.round(totalPrice * 0.1);
    const finalPrice = totalPrice + taxAmount;

    const newBooking: Booking = {
      ...booking,
      id: this.generateId(),
      userId: user.id || user.username,
      status: 'PENDING',
      paymentStatus: 'UNPAID',
      nights,
      totalPrice,
      taxAmount,
      finalPrice,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const bookings = this.getAllBookings();
    bookings.push(newBooking);
    this.saveAllBookings(bookings);

    return of(newBooking).pipe(delay(500));
  }

  /**
   * Lấy danh sách bookings của user hiện tại
   */
  getMyBookings(): Observable<Booking[]> {
    const user = this.auth.getUser();
    if (!user) {
      return of([]);
    }

    const userId = user.id || user.username;
    const all = this.getAllBookings();
    const myBookings = all
      .filter(b => b.userId === userId)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    return of(myBookings).pipe(delay(300));
  }

  /**
   * Lấy chi tiết 1 booking
   */
  getBookingById(id: string): Observable<Booking | null> {
    const all = this.getAllBookings();
    const booking = all.find(b => b.id === id) || null;
    return of(booking).pipe(delay(200));
  }

  /**
   * Cập nhật trạng thái booking (cancel, pay, etc)
   */
  updateBookingStatus(id: string, status: BookingStatus, paymentStatus?: string): Observable<Booking | null> {
    const all = this.getAllBookings();
    const idx = all.findIndex(b => b.id === id);
    if (idx === -1) {
      return of(null);
    }

    all[idx] = {
      ...all[idx],
      status,
      paymentStatus: (paymentStatus as any) || all[idx].paymentStatus,
      updatedAt: new Date().toISOString(),
    };

    if (status === 'CANCELLED') {
      all[idx].cancelledAt = new Date().toISOString();
    } else if (status === 'CONFIRMED' && !all[idx].confirmedAt) {
      all[idx].confirmedAt = new Date().toISOString();
    }

    this.saveAllBookings(all);
    return of(all[idx]).pipe(delay(300));
  }

  /**
   * Thanh toán booking
   */
  payBooking(id: string, method: PaymentMethod): Observable<Booking | null> {
    const all = this.getAllBookings();
    const idx = all.findIndex(b => b.id === id);
    if (idx === -1) return of(null);

    all[idx] = {
      ...all[idx],
      status: 'CONFIRMED',
      paymentStatus: 'PAID',
      paymentMethod: method,
      confirmedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    this.saveAllBookings(all);
    return of(all[idx]).pipe(delay(500));
  }

  // ======================================
  // HELPERS
  // ======================================

  private getAllBookings(): Booking[] {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    if (!raw) return [];
    try {
      return JSON.parse(raw);
    } catch {
      return [];
    }
  }

  private saveAllBookings(bookings: Booking[]): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(bookings));
  }

  private generateId(): string {
    return 'BK-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6).toUpperCase();
  }

  private calculateNights(checkIn: string, checkOut: string): number {
    const date1 = new Date(checkIn);
    const date2 = new Date(checkOut);
    const diff = date2.getTime() - date1.getTime();
    return Math.max(1, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  }
}
