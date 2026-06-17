import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule, NzIconModule],
  template: `
    <div class="kpi-card" [class]="'kpi-card--' + variant">
      <div class="kpi-icon">
        <span [nzType]="icon" nz-icon nzTheme="outline"></span>
      </div>
      <div class="kpi-content">
        <div class="kpi-label">{{ label }}</div>
        <div class="kpi-value">{{ value }}</div>
        <div *ngIf="change" class="kpi-change" [class.up]="changeDirection === 'up'" [class.down]="changeDirection === 'down'">
          <span nz-icon [nzType]="changeDirection === 'up' ? 'caret-up' : 'caret-down'"></span>
          {{ change }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kpi-card {
      background: white;
      border-radius: 16px;
      padding: 20px;
      border: 1px solid #F3F4F6;
      display: flex;
      align-items: center;
      gap: 16px;
      transition: all 0.25s ease;
    }

    .kpi-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;
    }

    .kpi-card--primary .kpi-icon { background: #EEF2FF; color: #6366F1; }
    .kpi-card--success .kpi-icon { background: #D1FAE5; color: #10B981; }
    .kpi-card--warning .kpi-icon { background: #FEF3C7; color: #F59E0B; }
    .kpi-card--danger  .kpi-icon { background: #FEE2E2; color: #EF4444; }
    .kpi-card--info    .kpi-icon { background: #DBEAFE; color: #3B82F6; }

    .kpi-content { flex: 1; min-width: 0; }

    .kpi-label {
      font-size: 13px;
      color: #6B7280;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .kpi-value {
      font-size: 24px;
      font-weight: 700;
      color: #1F2937;
      line-height: 1.2;
    }

    .kpi-change {
      font-size: 12px;
      margin-top: 4px;
      display: flex;
      align-items: center;
      gap: 2px;
      &.up { color: #10B981; }
      &.down { color: #EF4444; }
    }
  `]
})
export class KpiCardComponent {
  @Input() label = '';
  @Input() value: string | number = '';
  @Input() icon = 'star';
  @Input() variant: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'primary';
  @Input() change = '';
  @Input() changeDirection: 'up' | 'down' = 'up';
}
