import { LogoComponent } from './../../shared/components/logo/logo.component';
import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzIconModule } from 'ng-zorro-antd/icon';

@Component({
  selector: 'app-auth-layout',
  imports: [CommonModule, RouterModule, NzLayoutModule, NzMenuModule, NzIconModule, LogoComponent],
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.scss',
})
export class AuthLayoutComponent {
  openDropdown = signal<string | null>(null);
  closeDropdown(): void {
    this.openDropdown.set(null);
  }
}
