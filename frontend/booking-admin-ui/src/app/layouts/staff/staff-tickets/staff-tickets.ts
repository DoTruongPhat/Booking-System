// ═══════════════════════════════════════════════════════════
// STAFF TICKETS PAGE
// Hiển thị các ticket được assign cho staff đang login.
// Staff có thể update status (IN_PROGRESS → RESOLVED).
// Endpoint BE: GET /api/tickets/assigned
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { TicketService } from '../../../core/services/ticket';
import { SupportTicket, TicketStatus, TicketPriority } from '../../../core/models/auth.model';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-staff-tickets',
  imports: [
    CommonModule,
    FormsModule,
    NzTableModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    NzPaginationModule,
    NzEmptyModule,
    NzCardModule,
    NzAlertModule,
    NzSpinModule,
    NzPopconfirmModule,
    NzDescriptionsModule,
    NzAvatarModule,
    NzSpaceModule,
  ],
  templateUrl: './staff-tickets.html',
  styleUrl: './staff-tickets.scss',
})
export class StaffTicketsComponent implements OnInit {
  private ticketService = inject(TicketService);
  private message = inject(NzMessageService);

  // ── Data state ─────────────────────────────────────────
  tickets: SupportTicket[] = [];
  isLoading = false;

  // ── Paging ─────────────────────────────────────────────
  pageIndex = 1;
  pageSize = 10;
  total = 0;

  // ── Filter ─────────────────────────────────────────────
  statusFilter: TicketStatus | 'ALL' = 'ALL';

  // ── Modal state ────────────────────────────────────────
  isDetailModalOpen = false;
  isStatusModalOpen = false;
  isSubmitting = false;
  selectedTicket: SupportTicket | null = null;
  newStatus: TicketStatus = 'IN_PROGRESS';

  // Status options (cho filter + modal update)
  statusOptions: { value: TicketStatus; label: string; color: string }[] = [
    { value: 'OPEN', label: 'Open', color: 'blue' },
    { value: 'IN_PROGRESS', label: 'In Progress', color: 'orange' },
    { value: 'RESOLVED', label: 'Resolved', color: 'green' },
    { value: 'CLOSED', label: 'Closed', color: 'default' },
  ];

  // Status transitions staff được phép (theo plan: chỉ IN_PROGRESS → RESOLVED)
  // Staff không được CLOSE (chỉ admin/user close)
  allowedTransitions: Record<TicketStatus, TicketStatus[]> = {
    OPEN: ['IN_PROGRESS'],
    IN_PROGRESS: ['RESOLVED'],
    RESOLVED: [],
    CLOSED: [],
  };

  ngOnInit() {
    this.loadTickets();
  }

  // ── LOAD ───────────────────────────────────────────────
  loadTickets() {
    this.isLoading = true;
    const status = this.statusFilter === 'ALL' ? undefined : this.statusFilter;
    this.ticketService.getAssignedTickets(this.pageIndex - 1, this.pageSize, status).subscribe({
      next: (res) => {
        this.tickets = res.content;
        this.total = res.totalElements;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.message.error(extractErrorMessage(err, 'Failed to load tickets'));
      },
    });
  }

  // ── PAGING ─────────────────────────────────────────────
  onPageChange(page: number) {
    this.pageIndex = page;
    this.loadTickets();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadTickets();
  }

  onStatusFilterChange() {
    this.pageIndex = 1;
    this.loadTickets();
  }

  resetFilter() {
    this.statusFilter = 'ALL';
    this.pageIndex = 1;
    this.loadTickets();
  }

  // ── DETAIL MODAL ───────────────────────────────────────
  openDetailModal(ticket: SupportTicket) {
    this.selectedTicket = ticket;
    this.isDetailModalOpen = true;
  }

  // ── STATUS UPDATE MODAL ────────────────────────────────
  openStatusModal(ticket: SupportTicket) {
    this.selectedTicket = ticket;
    const allowed = this.allowedTransitions[ticket.status];
    // Mặc định chọn status đầu tiên được phép
    this.newStatus = allowed.length > 0 ? allowed[0] : ticket.status;
    this.isStatusModalOpen = true;
  }

  submitStatusUpdate() {
    if (!this.selectedTicket) return;
    this.isSubmitting = true;
    this.ticketService.updateTicketStatus(this.selectedTicket.id, this.newStatus).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('Ticket status updated');
        this.isStatusModalOpen = false;
        this.loadTickets();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.message.error(extractErrorMessage(err, 'Failed to update status'));
      },
    });
  }

  // ── HELPERS ────────────────────────────────────────────
  getStatusColor(status: TicketStatus): string {
    const map: Record<TicketStatus, string> = {
      OPEN: 'blue',
      IN_PROGRESS: 'orange',
      RESOLVED: 'green',
      CLOSED: 'default',
    };
    return map[status];
  }

  getPriorityColor(priority: TicketPriority): string {
    const map: Record<TicketPriority, string> = {
      LOW: 'default',
      MEDIUM: 'blue',
      HIGH: 'orange',
      URGENT: 'red',
    };
    return map[priority];
  }

  canUpdateStatus(ticket: SupportTicket): boolean {
    return this.allowedTransitions[ticket.status].length > 0;
  }

  getNextStatusLabel(ticket: SupportTicket): string {
    const allowed = this.allowedTransitions[ticket.status];
    if (allowed.length === 0) return 'No action available';
    return `Mark as ${allowed[0]}`;
  }
}
