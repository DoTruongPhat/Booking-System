import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzIconModule } from 'ng-zorro-antd/icon';

/**
 * EmptyStateComponent - Hiển thị khi không có data
 *
 * Sử dụng:
 *   <app-empty-state
 *     icon="inbox"
 *     title="No bookings yet"
 *     message="You haven't made any bookings">
 *   </app-empty-state>
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, NzIconModule],
  template: `
    <div class="empty-state">
      <div class="empty-icon">
        <span [nzType]="icon" nz-icon nzTheme="outline"></span>
      </div>
      <h3 *ngIf="title">{{ title }}</h3>
      <p *ngIf="message">{{ message }}</p>
      <ng-content></ng-content>
    </div>
  `,
  styles: [`
    .empty-state {
      text-align: center;
      padding: 64px 24px;
      color: #6B7280;
    }

    .empty-icon {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: linear-gradient(135deg, #6366F1, #A855F7);
      color: white;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;
      font-size: 40px;
    }

    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #1F2937;
      margin: 0 0 8px 0;
    }

    p {
      margin: 0 0 16px 0;
      max-width: 400px;
      margin-left: auto;
      margin-right: auto;
    }
  `]
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() title = 'No data';
  @Input() message = '';
}
