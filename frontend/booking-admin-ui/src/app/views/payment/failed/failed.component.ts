import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-payment-failed',
  standalone: true,
  imports: [CommonModule, RouterModule, NzIconModule, NzButtonModule, NavbarComponent],
  templateUrl: './failed.component.html',
  styleUrl: './failed.component.scss',
})
export class PaymentFailedComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  bookingId = '';

  ngOnInit(): void {
    this.bookingId = this.route.snapshot.queryParams['bookingId'] || '';
  }

  retryPayment(): void {
    if (this.bookingId) {
      this.router.navigate(['/booking/checkout', this.bookingId]);
    }
  }
}
