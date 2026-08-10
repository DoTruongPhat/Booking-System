import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { SupportTicket, TicketPriority, TicketStatus } from '../../../core/models/auth.model';
import { TicketService } from '../../../core/services/ticket';
import { extractErrorMessage } from '../../../core/utils/error.util';

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
export class MyTicketsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private ticketService = inject(TicketService);
  private message = inject(NzMessageService);

  showForm = false;
  isLoading = false;
  isSubmitting = false;
  tickets: SupportTicket[] = [];

  ticketForm = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    priority: ['MEDIUM'],
  });

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.isLoading = true;
    this.ticketService.getMyTickets(0, 50).subscribe({
      next: (res) => {
        this.tickets = res.content;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.message.error(extractErrorMessage(err, 'Không thể tải phiếu hỗ trợ'));
      },
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.ticketForm.reset({ priority: 'MEDIUM' });
    }
  }

  submitTicket(): void {
    if (this.ticketForm.invalid) return;
    this.isSubmitting = true;

    const formValue = this.ticketForm.getRawValue();
    this.ticketService
      .createTicket({
        title: formValue.title!,
        description: formValue.description!,
        priority: formValue.priority ?? 'MEDIUM',
      })
      .subscribe({
        next: (ticket) => {
          this.tickets = [ticket, ...this.tickets];
          this.isSubmitting = false;
          this.showForm = false;
          this.ticketForm.reset({ priority: 'MEDIUM' });
          this.message.success('Đã gửi phiếu hỗ trợ');
        },
        error: (err) => {
          this.isSubmitting = false;
          this.message.error(extractErrorMessage(err, 'Không thể gửi phiếu hỗ trợ'));
        },
      });
  }

  getStatusColor(status: TicketStatus): string {
    const map: Record<TicketStatus, string> = {
      OPEN: 'orange',
      IN_PROGRESS: 'green',
      RESOLVED: 'green',
      CLOSED: 'default',
    };
    return map[status] || 'default';
  }

  getStatusLabel(status: TicketStatus): string {
    const map: Record<TicketStatus, string> = {
      OPEN: 'Đang mở',
      IN_PROGRESS: 'Đang xử lý',
      RESOLVED: 'Đã giải quyết',
      CLOSED: 'Đã đóng',
    };
    return map[status] || status;
  }

  getPriorityColor(priority: TicketPriority): string {
    const map: Record<TicketPriority, string> = {
      LOW: 'default',
      MEDIUM: 'green',
      HIGH: 'orange',
      URGENT: 'red',
    };
    return map[priority] || 'default';
  }

  getPriorityLabel(priority: TicketPriority): string {
    const map: Record<TicketPriority, string> = {
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
