import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-hero-card',
  standalone: true,
  imports: [CommonModule, NzIconModule],
  template: `
    <div class="hero-card" [class]="'hero-card--' + variant">
      <div *ngIf="icon" class="hero-icon">
        <span [nzType]="icon" nz-icon nzTheme="outline"></span>
      </div>
      <div class="hero-content">
        <h2 *ngIf="title">{{ title }}</h2>
        <p *ngIf="subtitle">{{ subtitle }}</p>
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .hero-card {
      border-radius: 16px;
      padding: 24px;
      color: white;
      display: flex;
      gap: 20px;
      align-items: center;
    }

    .hero-card--primary { background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%); }
    .hero-card--success { background: linear-gradient(135deg, #10B981 0%, #34D399 100%); }
    .hero-card--warning { background: linear-gradient(135deg, #F59E0B 0%, #FBBF24 100%); }
    .hero-card--danger  { background: linear-gradient(135deg, #EF4444 0%, #F87171 100%); }
    .hero-card--info    { background: linear-gradient(135deg, #3B82F6 0%, #60A5FA 100%); }

    .hero-icon {
      font-size: 32px;
      width: 56px;
      height: 56px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .hero-content {
      flex: 1;
      h2 {
        color: white;
        font-size: 22px;
        font-weight: 700;
        margin: 0 0 4px 0;
      }
      p {
        opacity: 0.9;
        margin: 0;
        font-size: 14px;
      }
    }
  `]
})
export class HeroCardComponent {
  @Input() icon = '';
  @Input() title = '';
  @Input() subtitle = '';
  @Input() variant: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'primary';
}
