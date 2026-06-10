import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { Auth } from '../../../core/services/auth';
import { Dashboard as DashboardService } from '../../../core/services/dashboard';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, NzCardModule, NzIconModule, NzGridModule, NzStatisticModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  currentUser: any;

  // Stats - sẽ gọi API lấy sau
  stats = {
    totalUsers: 150,
    totalBookings: 89,
    totalRooms: 32,
    totalRevenue: 2500000,
  };

  constructor(
    private auth: Auth,
    private dashboardService: DashboardService,
  ) {}

  ngOnInit() {
    this.currentUser = this.auth.getUser();

    // Gọi API lấy stats
    this.dashboardService.getStats().subscribe({
      next: (response) => {
        console.log('API response:', response);
        this.stats = response; // Gán dữ liệu từ API vào stats
      },
      error: (err) => {
        console.error('API failed:', err);
      },
    });
  }
}
