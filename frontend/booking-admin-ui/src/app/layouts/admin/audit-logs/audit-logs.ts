import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpaceModule } from 'ng-zorro-antd/space';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { AuditLog } from '../../../core/models/audit-log.model';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-audit-logs',
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzDatePickerModule,
    NzEmptyModule,
    NzIconModule,
    NzInputModule,
    NzSelectModule,
    NzSpaceModule,
    NzSpinModule,
    NzTableModule,
    NzTagModule,
    NzTooltipModule,
  ],
  templateUrl: './audit-logs.html',
  styleUrl: './audit-logs.scss',
})
export class AuditLogs implements OnInit {
  private readonly auditLogService = inject(AuditLogService);
  private readonly message = inject(NzMessageService);

  logs: AuditLog[] = [];
  isLoading = false;
  pageIndex = 1;
  pageSize = 20;
  total = 0;

  eventType = '';
  source = '';
  actorName = '';
  entityType = '';
  entityId = '';
  dateRange: Date[] = [];

  readonly sourceOptions = ['ADMIN', 'KAFKA', 'AUTH', 'SYSTEM'];
  readonly entityOptions = ['USER', 'SESSION', 'TICKET', 'HOTEL', 'BOOKING', 'ROOM', 'VOUCHER', 'PAYMENT'];

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.isLoading = true;
    const [from, to] = this.dateRange || [];

    this.auditLogService
      .getAuditLogs({
        eventType: this.eventType,
        source: this.source,
        actorName: this.actorName,
        entityType: this.entityType,
        entityId: this.entityId,
        from: from ? from.toISOString() : undefined,
        to: to ? to.toISOString() : undefined,
        page: this.pageIndex - 1,
        size: this.pageSize,
      })
      .subscribe({
        next: (res) => {
          this.logs = res.content || [];
          this.total = res.totalElements || 0;
          this.isLoading = false;
        },
        error: (err) => {
          this.isLoading = false;
          this.message.error(extractErrorMessage(err, 'Cannot load audit logs'));
        },
      });
  }

  resetFilters(): void {
    this.eventType = '';
    this.source = '';
    this.actorName = '';
    this.entityType = '';
    this.entityId = '';
    this.dateRange = [];
    this.pageIndex = 1;
    this.loadLogs();
  }

  onPageIndexChange(page: number): void {
    this.pageIndex = page;
    this.loadLogs();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.pageIndex = 1;
    this.loadLogs();
  }

  sourceColor(source?: string | null): string {
    const colors: Record<string, string> = {
      ADMIN: 'success',
      KAFKA: 'processing',
      AUTH: 'warning',
      SYSTEM: 'default',
    };
    return source ? colors[source] || 'default' : 'default';
  }

  compactMetadata(log: AuditLog): string {
    if (!log.metadata || !Object.keys(log.metadata).length) {
      return '-';
    }
    return JSON.stringify(log.metadata);
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleString();
  }
}
