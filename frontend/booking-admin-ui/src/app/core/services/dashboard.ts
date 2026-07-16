import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';

export interface DashboardStats {
  totalUsers: number;
  totalHotels?: number;
  activeHotels?: number;
  pendingHotels?: number;
  totalBookings: number;
  totalRooms: number;
  totalRoomTypes?: number;
  availableRoomTypes?: number;
  activeBookings?: number;
  pendingBookings?: number;
  upcomingCheckIns?: number;
  totalRevenue: number;
  message?: string;
  status?: string;
}

@Injectable({
  providedIn: 'root',
})
export class Dashboard {
  private adminApiUrl = '/api/admin/dashboard';
  private hostApiUrl = '/api/host/dashboard';

  constructor(private http: HttpClient) {}

  getAdminStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(this.adminApiUrl, { withCredentials: true });
  }

  getHostStats(): Observable<DashboardStats> {
    return this.http
      .get<ApiResponse<DashboardStats>>(this.hostApiUrl, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  getStats(role: 'ADMIN' | 'HOST' | 'USER' = 'ADMIN'): Observable<DashboardStats> {
    return role === 'HOST' ? this.getHostStats() : this.getAdminStats();
  }
}
