import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-my-tickets',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    NzIconModule,
    NzButtonModule,
    NzTagModule,
    NzFormModule,
    NzInputModule,
    NavbarComponent,
  ],
  templateUrl: './tickets.component.html',
  styleUrl: './tickets.component.scss',
})
export class MyTicketsComponent {
  private fb = inject(FormBuilder);

  showForm = false;
  isSubmitting = false;

  ticketForm = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    priority: ['MEDIUM'],
  });

  tickets = [
    {
      id: 'TK001',
      title: 'Không thể check-in online',
      description: 'Tôi đã thanh toán nhưng hệ thống không cho phép check-in online trước 24h.',
      status: 'OPEN' as const,
      priority: 'HIGH' as const,
      createdAt: '2026-06-10',
      updatedAt: '2026-06-11',
    },
    {
      id: 'TK002',
      title: 'Yêu cầu hủy phòng',
      description: 'Tôi cần hủy đặt phòng BK003 do thay đổi lịch trình công tác.',
      status: 'IN_PROGRESS' as const,
      priority: 'MEDIUM' as const,
      createdAt: '2026-06-08',
      updatedAt: '2026-06-12',
    },
    {
      id: 'TK003',
      title: 'Cảm ơn SmartBooking',
      description: 'Dịch vụ rất tốt, nhân viên hỗ trợ nhiệt tình!',
      status: 'RESOLVED' as const,
      priority: 'LOW' as const,
      createdAt: '2026-05-20',
      updatedAt: '2026-05-25',
    },
  ];

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.ticketForm.reset({ priority: 'MEDIUM' });
    }
  }

  submitTicket(): void {
    if (this.ticketForm.invalid) return;
    this.isSubmitting = true;

    setTimeout(() => {
      const formValue = this.ticketForm.value;
      const newTicket = {
        id: 'TK' + String(this.tickets.length + 1).padStart(3, '0'),
        title: formValue.title!,
        description: formValue.description!,
        status: 'OPEN' as const,
        priority: formValue.priority as any,
        createdAt: new Date().toISOString().split('T')[0],
        updatedAt: new Date().toISOString().split('T')[0],
      };
      this.tickets.unshift(newTicket);
      this.isSubmitting = false;
      this.showForm = false;
      this.ticketForm.reset({ priority: 'MEDIUM' });
    }, 1000);
  }

  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'orange',
      IN_PROGRESS: 'blue',
      RESOLVED: 'green',
      CLOSED: 'default',
    };
    return map[status] || 'default';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      OPEN: 'Đang mở',
      IN_PROGRESS: 'Đang xử lý',
      RESOLVED: 'Đã giải quyết',
      CLOSED: 'Đã đóng',
    };
    return map[status] || status;
  }

  getPriorityColor(priority: string): string {
    const map: Record<string, string> = {
      LOW: 'default',
      MEDIUM: 'blue',
      HIGH: 'orange',
      URGENT: 'red',
    };
    return map[priority] || 'default';
  }

  getPriorityLabel(priority: string): string {
    const map: Record<string, string> = {
      LOW: 'Thấp',
      MEDIUM: 'Trung bình',
      HIGH: 'Cao',
      URGENT: 'Khẩn cấp',
    };
    return map[priority] || priority;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }
}