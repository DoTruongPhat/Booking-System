// ═══════════════════════════════════════════════════════════
// ADMIN TICKETS PAGE (A.3)
// Trang quản lý tất cả tickets cho admin
// - List tất cả tickets (kể cả chưa assign)
// - Filter: status, priority, assignedTo, date range, keyword
// - Assign cho staff
// - Đổi status (OPEN/IN_PROGRESS/RESOLVED/CLOSED)
// - Click row → mở /admin/tickets/:id
// - Dùng TicketService BE thật (đã có sẵn)
// ═══════════════════════════════════════════════════════════

import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzDividerModule } from 'ng-zorro-antd/divider';
import { TicketService } from '../../../core/services/ticket';
import { UserService } from '../../../core/services/user';
import {
  SupportTicket,
  TicketStatus,
  TicketPriority,
  Role,
  User,
} from '../../../core/models/auth.model';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-admin-tickets',
  imports: [
    CommonModule,
    ReactiveFormsModule,
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
    NzPopconfirmModule,
    NzSpaceModule,
    NzTooltipModule,
    NzCardModule,
    NzAvatarModule,
    NzDatePickerModule,
    NzEmptyModule,
    NzAlertModule,
    NzSpinModule,
    NzDividerModule,
  ],
  templateUrl: './tickets.html',
  styleUrl: './tickets.scss',
})
export class AdminTickets implements OnInit {
  private ticketService = inject(TicketService);
  private userService = inject(UserService);
  private message = inject(NzMessageService);
  private router = inject(Router);

  // ── Data ────────────────────────────────────────────────
  tickets: SupportTicket[] = [];
  isLoading = false;

  // Staff list (for assign dropdown)
  staffList: User[] = [];
  isLoadingStaff = false;

  // ── Paging ──────────────────────────────────────────────
  pageIndex = 1;
  pageSize = 10;
  total = 0;

  // ── Filters ─────────────────────────────────────────────
  filterStatus: TicketStatus | 'all' = 'all';
  filterPriority: TicketPriority | 'all' = 'all';
  filterAssignedTo: string | 'all' | 'unassigned' = 'all';
  keyword = '';
  dateRange: [Date, Date] | null = null;

  // ── Modal: assign ticket ────────────────────────────────
  isAssignModalOpen = false;
  selectedTicket: SupportTicket | null = null;
  isSubmitting = false;
  assignForm = inject(FormBuilder).nonNullable.group({
    staffId: ['',],
  });

  // ── STATUS CONFIG ───────────────────────────────────────
  readonly statusConfig: Record<
    TicketStatus,
    { label: string; color: string; icon: string }
  > = {
    OPEN: { label: 'Mở', color: 'orange', icon: 'inbox' },
    IN_PROGRESS: { label: 'Đang xử lý', color: 'green', icon: 'sync' },
    RESOLVED: { label: 'Đã giải quyết', color: 'green', icon: 'check-circle' },
    CLOSED: { label: 'Đã đóng', color: 'default', icon: 'close-circle' },
  };

  readonly priorityConfig: Record<
    TicketPriority,
    { label: string; color: string }
  > = {
    LOW: { label: 'Thấp', color: 'default' },
    MEDIUM: { label: 'Trung bình', color: 'green' },
    HIGH: { label: 'Cao', color: 'orange' },
    URGENT: { label: 'Khẩn cấp', color: 'red' },
  };

  // Tất cả status để filter
  readonly allStatuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  readonly allPriorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

  ngOnInit() {
    this.loadTickets();
    this.loadStaffList();
  }

  // ── LOAD TICKETS ────────────────────────────────────────
  loadTickets() {
    this.isLoading = true;
    const status = this.filterStatus === 'all' ? undefined : this.filterStatus;

    this.ticketService
      .getAllTickets(this.pageIndex - 1, this.pageSize, status)
      .subscribe({
        next: (res) => {
          this.tickets = this.applyClientFilters(res.content);
          this.total = res.totalElements;
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.message.error(extractErrorMessage(err, 'Failed to load tickets'));
        },
      });
  }

  // Filter client-side: priority, assignedTo, keyword, date
  private applyClientFilters(list: SupportTicket[]): SupportTicket[] {
    return list.filter((t) => {
      // Priority
      if (this.filterPriority !== 'all' && t.priority !== this.filterPriority) {
        return false;
      }
      // AssignedTo
      if (this.filterAssignedTo === 'unassigned' && t.assignedTo) return false;
      if (
        this.filterAssignedTo !== 'all' &&
        this.filterAssignedTo !== 'unassigned' &&
        t.assignedTo !== this.filterAssignedTo
      ) {
        return false;
      }
      // Keyword (title + description)
      if (this.keyword.trim()) {
        const kw = this.keyword.toLowerCase();
        if (
          !t.title.toLowerCase().includes(kw) &&
          !t.description.toLowerCase().includes(kw)
        ) {
          return false;
        }
      }
      // Date range
      if (this.dateRange) {
        const [from, to] = this.dateRange;
        const created = new Date(t.createdAt);
        if (created < from || created > to) return false;
      }
      return true;
    });
  }

  // ── LOAD STAFF LIST (cho dropdown assign) ───────────────
  loadStaffList() {
    this.isLoadingStaff = true;
    this.userService
      .getUsers(0, 100) // lấy 100 user
      .subscribe({
        next: (res) => {
          // Filter: chỉ lấy user có role STAFF hoặc ADMIN
          this.staffList = res.content.filter((u) =>
            u.roles?.some((r) => ['STAFF', 'ADMIN_ALL', 'ADMIN', 'HOST'].includes(r.code)),
          );
          this.isLoadingStaff = false;
        },
        error: () => {
          this.isLoadingStaff = false;
        },
      });
  }

  // ── PAGING ──────────────────────────────────────────────
  onPageChange(page: number) {
    this.pageIndex = page;
    this.loadTickets();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadTickets();
  }

  // ── FILTER HANDLERS ─────────────────────────────────────
  onSearch() {
    this.pageIndex = 1;
    this.loadTickets();
  }

  resetFilter() {
    this.filterStatus = 'all';
    this.filterPriority = 'all';
    this.filterAssignedTo = 'all';
    this.keyword = '';
    this.dateRange = null;
    this.pageIndex = 1;
    this.loadTickets();
  }

  onFilterChange() {
    this.pageIndex = 1;
    this.loadTickets();
  }

  // ── STATUS UPDATE ───────────────────────────────────────
  changeStatus(ticket: SupportTicket, newStatus: TicketStatus) {
    this.ticketService.updateTicketStatus(ticket.id, newStatus).subscribe({
      next: () => {
        this.message.success(
          `Đã đổi trạng thái ticket "${ticket.title}" → ${this.statusConfig[newStatus].label}`,
        );
        this.loadTickets();
      },
      error: (err) => {
        this.message.error(extractErrorMessage(err, 'Update status failed'));
      },
    });
  }

  // ── ASSIGN MODAL ────────────────────────────────────────
  openAssignModal(ticket: SupportTicket) {
    if (!this.canAssign(ticket)) {
      this.message.warning('Ticket da duoc xu ly, khong the assign lai');
      return;
    }

    this.selectedTicket = ticket;
    this.assignForm.reset({
      staffId: ticket.assignedTo ?? '',
    });
    this.isAssignModalOpen = true;
  }

  submitAssign() {
    if (this.assignForm.invalid || !this.selectedTicket) return;
    const staffId = this.assignForm.value.staffId;
    if (!staffId) {
      this.message.warning('Vui lòng chọn staff');
      return;
    }
    this.isSubmitting = true;
    this.ticketService.assignTicket(this.selectedTicket.id, staffId).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('Đã assign ticket cho staff');
        this.isAssignModalOpen = false;
        this.loadTickets();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.message.error(extractErrorMessage(err, 'Assign failed'));
      },
    });
  }

  // ── UNASSIGN ────────────────────────────────────────────
  unassign(ticket: SupportTicket) {
    // Set assignedTo = null qua update status (workaround: chỉ assign mới, ko có unassign riêng)
    this.message.warning('Tính năng unassign: dùng endpoint assign với staffId rỗng');
  }

  // ── NAVIGATE TO DETAIL ──────────────────────────────────
  viewDetail(ticket: SupportTicket) {
    this.router.navigate(['/admin/tickets', ticket.id]);
  }

  // ── HELPERS ─────────────────────────────────────────────
  getStatusColor(status: TicketStatus): string {
    return this.statusConfig[status]?.color ?? 'default';
  }

  getStatusLabel(status: TicketStatus): string {
    return this.statusConfig[status]?.label ?? status;
  }

  getPriorityColor(priority: TicketPriority): string {
    return this.priorityConfig[priority]?.color ?? 'default';
  }

  getPriorityLabel(priority: TicketPriority): string {
    return this.priorityConfig[priority]?.label ?? priority;
  }

  getStaffName(staffId: string | null): string {
    if (!staffId) return '—';
    const s = this.staffList.find((u) => u.id === staffId);
    if (!s) return staffId.substring(0, 8);
    const fullName = `${s.firstName ?? ''} ${s.lastName ?? ''}`.trim();
    return fullName || s.username;
  }

  canAssign(ticket: SupportTicket): boolean {
    return ticket.status !== 'RESOLVED' && ticket.status !== 'CLOSED';
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('vi-VN');
  }

  truncate(text: string, len = 50): string {
    return text.length > len ? text.substring(0, len) + '...' : text;
  }

  // ── STATS HELPERS ───────────────────────────────────────
  countByStatus(status: TicketStatus): number {
    return this.tickets.filter((t) => t.status === status).length;
  }

  /** Click vào stat card để filter nhanh */
  setStatusFilter(status: TicketStatus) {
    this.filterStatus = status;
    this.onFilterChange();
  }
}
