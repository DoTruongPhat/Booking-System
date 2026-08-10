import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { WorkflowService, TaskResponse } from '../../../core/services/workflow.service';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-workflow',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    NzTableModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzModalModule,
    NzInputModule,
    NzPopconfirmModule,
    NzEmptyModule,
    NzCardModule,
    NzTooltipModule,
    NzSpinModule,
    NzAlertModule,
  ],
  templateUrl: './workflow.html',
  styleUrl: './workflow.scss',
})
export class WorkflowComponent implements OnInit {
  private workflowService = inject(WorkflowService);
  private auth = inject(Auth);
  private message = inject(NzMessageService);
  private route = inject(ActivatedRoute);

  tasks: TaskResponse[] = [];
  loading = false;
  focusTaskId: string | null = null;
  focusHotelId: string | null = null;

  isReviewModalOpen = false;
  selectedTask: TaskResponse | null = null;
  reviewComment = '';
  isSubmitting = false;

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.focusTaskId = params.get('taskId');
      this.focusHotelId = params.get('hotelId');
    });
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;

    if (this.auth.getPrimaryRole() === 'ADMIN') {
      this.workflowService.getTasks().subscribe({
        next: (tasks) => {
          this.tasks = this.sortWorkflowTasks(tasks);
          this.notifyIfFocusedTaskMissing();
          this.loading = false;
        },
        error: () => {
          this.message.error('Không thể tải danh sách task');
          this.loading = false;
        },
      });
      return;
    }

    this.workflowService.getTasks({ candidateGroup: 'ADMIN' }).subscribe({
      next: (tasks) => {
        const userId = this.auth.getUserId();
        this.workflowService.getTasks({ assignee: userId }).subscribe({
          next: (assignedTasks) => {
            this.tasks = this.sortWorkflowTasks(this.mergeTasks(tasks, assignedTasks));
            this.notifyIfFocusedTaskMissing();
            this.loading = false;
          },
          error: () => {
            this.tasks = this.sortWorkflowTasks(tasks);
            this.notifyIfFocusedTaskMissing();
            this.loading = false;
          },
        });
      },
      error: () => {
        this.message.error('Không thể tải danh sách task');
        this.loading = false;
      },
    });
  }

  claimTask(task: TaskResponse): void {
    this.workflowService.claimTask(task.taskId).subscribe({
      next: () => {
        this.message.success('Đã nhận task');
        this.loadTasks();
      },
      error: (err) => {
        this.message.error(err?.error?.message || 'Không thể nhận task');
      },
    });
  }

  unclaimTask(task: TaskResponse): void {
    this.workflowService.unclaimTask(task.taskId).subscribe({
      next: () => {
        this.message.success('Đã trả task');
        this.loadTasks();
      },
    });
  }

  openReviewModal(task: TaskResponse): void {
    this.selectedTask = task;
    this.reviewComment = '';
    this.isReviewModalOpen = true;
  }

  approveTask(): void {
    if (!this.selectedTask) return;
    this.isSubmitting = true;

    this.workflowService
      .decideHotelApproval(this.selectedTask.taskId, 'APPROVED', this.reviewComment || 'Approved')
      .subscribe({
        next: () => {
          this.message.success('Hotel đã được duyệt!');
          this.isReviewModalOpen = false;
          this.isSubmitting = false;
          this.loadTasks();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi khi duyệt');
          this.isSubmitting = false;
        },
      });
  }

  rejectTask(): void {
    if (!this.selectedTask) return;
    if (!this.reviewComment.trim()) {
      this.message.warning('Vui lòng nhập lý do từ chối');
      return;
    }
    this.isSubmitting = true;

    this.workflowService
      .decideHotelApproval(this.selectedTask.taskId, 'REJECTED', this.reviewComment)
      .subscribe({
        next: () => {
          this.message.success('Hotel đã bị từ chối');
          this.isReviewModalOpen = false;
          this.isSubmitting = false;
          this.loadTasks();
        },
        error: (err) => {
          this.message.error(err?.error?.message || 'Lỗi khi từ chối');
          this.isSubmitting = false;
        },
      });
  }

  getHotelName(task: TaskResponse | null | undefined): string {
    return task?.variables?.['hotelName'] || 'N/A';
  }

  getCity(task: TaskResponse | null | undefined): string {
    return task?.variables?.['city'] || 'N/A';
  }

  getHostId(task: TaskResponse | null | undefined): string {
    return task?.variables?.['hostId'] || 'N/A';
  }

  getHotelId(task: TaskResponse | null | undefined): string {
    return task?.variables?.['hotelId'] || task?.businessKey || '';
  }

  getWorkflowType(task: TaskResponse | null | undefined): string {
    return task?.variables?.['workflowType'] || 'CREATE_HOTEL';
  }

  getWorkflowTypeLabel(task: TaskResponse | null | undefined): string {
    return this.getWorkflowType(task) === 'UPDATE_HOTEL' ? 'Cập nhật khách sạn' : 'Tạo khách sạn';
  }

  getProposedChanges(task: TaskResponse | null | undefined): { key: string; value: any }[] {
    const changes = task?.variables?.['proposedChanges'];
    if (!changes || typeof changes !== 'object') return [];
    return Object.entries(changes).map(([key, value]) => ({ key, value }));
  }

  formatChangeValue(value: any): string {
    if (Array.isArray(value)) return value.join(', ');
    if (value === null || value === undefined || value === '') return '—';
    return String(value);
  }

  getReviewStatus(task: TaskResponse | null | undefined): string {
    return task?.variables?.['reviewStatus'] || 'WAITING_ADMIN_REVIEW';
  }

  getReminderCount(task: TaskResponse | null | undefined): number {
    return Number(task?.variables?.['reviewReminderCount'] || 0);
  }

  hasReminder(task: TaskResponse | null | undefined): boolean {
    return this.getReminderCount(task) > 0 || Boolean(task?.variables?.['lastReviewReminderAt']);
  }

  getReminderLevel(task: TaskResponse | null | undefined): string {
    return String(task?.variables?.['reminderLevel'] || '').toUpperCase();
  }

  getReminderLabel(task: TaskResponse | null | undefined): string {
    const count = this.getReminderCount(task);
    if (!count) return 'No reminder';
    return `${this.getReminderLevel(task) || 'LOW'} / ${count} reminder`;
  }

  formatReminderTime(task: TaskResponse | null | undefined): string {
    const value = task?.variables?.['lastReviewReminderAt'];
    return typeof value === 'string' ? this.formatDateTime(value) : '';
  }

  formatDateTime(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleString('vi-VN');
  }

  isMyTask(task: TaskResponse): boolean {
    return task.assignee === this.auth.getUserId();
  }

  canReview(task: TaskResponse): boolean {
    return !task.assignee || this.isMyTask(task);
  }

  isFocusedTask(task: TaskResponse): boolean {
    return task.taskId === this.focusTaskId || task.businessKey === this.focusHotelId;
  }

  private mergeTasks(candidateTasks: TaskResponse[], assignedTasks: TaskResponse[]): TaskResponse[] {
    const seen = new Set<string>();
    return [...candidateTasks, ...assignedTasks].filter((task) => {
      if (seen.has(task.taskId)) return false;
      seen.add(task.taskId);
      return true;
    });
  }

  private sortWorkflowTasks(tasks: TaskResponse[]): TaskResponse[] {
    return [...tasks].sort((left, right) => {
      const leftReminder = this.hasReminder(left) ? 1 : 0;
      const rightReminder = this.hasReminder(right) ? 1 : 0;
      if (leftReminder !== rightReminder) return rightReminder - leftReminder;

      return new Date(right.created).getTime() - new Date(left.created).getTime();
    });
  }

  private notifyIfFocusedTaskMissing(): void {
    if (!this.focusTaskId && !this.focusHotelId) return;
    const found = this.tasks.some((task) => this.isFocusedTask(task));
    if (!found) {
      this.message.info('Task này có thể đã được xử lý hoặc không còn active');
    }
  }
}
