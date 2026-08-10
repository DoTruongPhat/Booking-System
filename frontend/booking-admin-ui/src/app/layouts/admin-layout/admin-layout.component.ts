import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { Auth } from '../../core/services/auth';
import { TaskResponse, WorkflowService } from '../../core/services/workflow.service';

@Component({
  selector: 'app-admin-layout',
  imports: [
    CommonModule,
    RouterModule,
    NzIconModule,
    NzLayoutModule,
    NzMenuModule,
    NzTooltipModule,
    NzDropDownModule,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  isCollapsed = false;
  user: any;
  workflowTasks: TaskResponse[] = [];
  loadingWorkflowTasks = false;
  private workflowRefreshTimer: ReturnType<typeof setInterval> | null = null;

  constructor(
    private auth: Auth,
    private router: Router,
    private workflowService: WorkflowService,
  ) {
    this.user = this.auth.getUser();
  }

  ngOnInit(): void {
    if (this.canUseWorkflowNotifications()) {
      this.loadWorkflowNotifications();
      this.workflowRefreshTimer = setInterval(() => this.loadWorkflowNotifications(false), 60000);
    }
  }

  ngOnDestroy(): void {
    if (this.workflowRefreshTimer) {
      clearInterval(this.workflowRefreshTimer);
    }
  }

  toggleSidebar(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  get userInitial(): string {
    return this.user?.username?.charAt(0).toUpperCase() || 'A';
  }

  get roleLabel(): string {
    const roles = this.user?.roles || [];
    if (roles.includes('ADMIN_ALL') || roles.includes('ADMIN')) return 'Quản trị viên';
    if (roles.includes('HOST')) return 'Chủ khách sạn';
    return 'Người dùng';
  }

  logout(): void {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.clearAll();
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.auth.clearAll();
        this.router.navigate(['/auth/login']);
      },
    });
  }

  hasAnyRole(roles: string[]): boolean {
    const userRoles = this.user?.roles || [];
    return roles.some((r) => userRoles.includes(r));
  }

  get workflowNotificationCount(): number {
    return this.workflowTasks.length;
  }

  get workflowReminderCount(): number {
    return this.workflowTasks.filter((task) => this.hasWorkflowReminder(task)).length;
  }

  loadWorkflowNotifications(showLoading = true): void {
    if (!this.canUseWorkflowNotifications()) return;

    this.loadingWorkflowTasks = showLoading;
    const currentUserId = this.auth.getUserId();

    this.workflowService.getTasks({ candidateGroup: 'ADMIN' }).subscribe({
      next: (candidateTasks) => {
        this.workflowService.getTasks({ assignee: currentUserId }).subscribe({
          next: (assignedTasks) => {
            this.workflowTasks = this.sortWorkflowTasks(
              this.mergeTasks(candidateTasks, assignedTasks),
            ).slice(0, 8);
            this.loadingWorkflowTasks = false;
          },
          error: () => {
            this.workflowTasks = this.sortWorkflowTasks(candidateTasks).slice(0, 8);
            this.loadingWorkflowTasks = false;
          },
        });
      },
      error: () => {
        this.workflowTasks = [];
        this.loadingWorkflowTasks = false;
      },
    });
  }

  openWorkflowTask(task?: TaskResponse): void {
    this.router.navigate(['/admin/workflow'], {
      queryParams: task
        ? {
            taskId: task.taskId,
            hotelId: task.businessKey,
          }
        : undefined,
    });
  }

  getTaskHotelName(task: TaskResponse): string {
    return task.variables?.['hotelName'] || 'Khách sạn chờ duyệt';
  }

  getTaskCity(task: TaskResponse): string {
    return task.variables?.['city'] || 'Chưa có thành phố';
  }

  getReminderCount(task: TaskResponse): number {
    return Number(task.variables?.['reviewReminderCount'] || 0);
  }

  getReminderLevel(task: TaskResponse): string {
    return String(task.variables?.['reminderLevel'] || '').toUpperCase();
  }

  hasWorkflowReminder(task: TaskResponse): boolean {
    return this.getReminderCount(task) > 0 || Boolean(task.variables?.['lastReviewReminderAt']);
  }

  getTaskStateLabel(task: TaskResponse): string {
    if (this.hasWorkflowReminder(task)) {
      const count = this.getReminderCount(task);
      return count > 1 ? `Đã nhắc ${count} lần` : 'Cần xử lý';
    }
    return task.assignee ? 'Đã nhận' : 'Chưa nhận';
  }

  formatReminderTime(task: TaskResponse): string {
    const value = task.variables?.['lastReviewReminderAt'];
    return typeof value === 'string' ? this.formatWorkflowTaskTime(value) : '';
  }

  formatWorkflowTaskTime(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private sortWorkflowTasks(tasks: TaskResponse[]): TaskResponse[] {
    return [...tasks].sort((left, right) => {
      const leftReminder = this.hasWorkflowReminder(left) ? 1 : 0;
      const rightReminder = this.hasWorkflowReminder(right) ? 1 : 0;
      if (leftReminder !== rightReminder) return rightReminder - leftReminder;

      return new Date(right.created).getTime() - new Date(left.created).getTime();
    });
  }

  private canUseWorkflowNotifications(): boolean {
    return this.hasAnyRole(['ADMIN_ALL', 'ADMIN']);
  }

  private mergeTasks(candidateTasks: TaskResponse[], assignedTasks: TaskResponse[]): TaskResponse[] {
    const seen = new Set<string>();
    return [...candidateTasks, ...assignedTasks].filter((task) => {
      if (seen.has(task.taskId)) return false;
      seen.add(task.taskId);
      return true;
    });
  }
}
