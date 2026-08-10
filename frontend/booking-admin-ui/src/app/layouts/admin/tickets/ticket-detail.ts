// ═══════════════════════════════════════════════════════════
// ADMIN TICKET DETAIL (A.3)
// Trang xem chi tiết 1 ticket + đổi trạng thái + assign lại
// Route: /admin/tickets/:id
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { TicketService } from '../../../core/services/ticket';
import {
  SupportTicket,
  TicketStatus,
  User,
} from '../../../core/models/auth.model';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-admin-ticket-detail',
  imports: [
    CommonModule,
    FormsModule,
    NzCardModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzDescriptionsModule,
    NzDividerModule,
    NzAvatarModule,
    NzSelectModule,
    NzSpinModule,
    NzEmptyModule,
    NzAlertModule,
  ],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.scss',
})
export class AdminTicketDetail implements OnInit {
  private ticketService = inject(TicketService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private message = inject(NzMessageService);

  ticket: SupportTicket | null = null;
  isLoading = false;

  readonly allStatuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly statusConfig: Record<TicketStatus, { label: string; color: string }> = {
    OPEN: { label: 'Mở', color: 'orange' },
    IN_PROGRESS: { label: 'Đang xử lý', color: 'green' },
    RESOLVED: { label: 'Đã giải quyết', color: 'green' },
    CLOSED: { label: 'Đã đóng', color: 'default' },
  };
  readonly priorityConfig: Record<string, { label: string; color: string }> = {
    LOW: { label: 'Thấp', color: 'default' },
    MEDIUM: { label: 'Trung bình', color: 'green' },
    HIGH: { label: 'Cao', color: 'orange' },
    URGENT: { label: 'Khẩn cấp', color: 'red' },
  };

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.loadTicket(id);
  }

  loadTicket(id: string) {
    this.isLoading = true;
    this.ticketService.getTicketById(id).subscribe({
      next: (ticket) => {
        this.ticket = ticket;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.message.error(extractErrorMessage(err, 'Failed to load ticket'));
        this.router.navigate(['/admin/tickets']);
      },
    });
  }

  changeStatus(newStatus: TicketStatus) {
    if (!this.ticket) return;
    this.ticketService.updateTicketStatus(this.ticket.id, newStatus).subscribe({
      next: (updated) => {
        this.ticket = updated;
        this.message.success(`Đã đổi trạng thái → ${this.statusConfig[newStatus].label}`);
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Update failed'));
      },
    });
  }

  formatDate(iso: string | null): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('vi-VN');
  }

  goBack() {
    this.router.navigate(['/admin/tickets']);
  }
}
