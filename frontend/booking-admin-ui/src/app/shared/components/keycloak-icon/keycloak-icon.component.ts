import { Component, input } from '@angular/core';

/**
 * Keycloak brand icon (SVG inline).
 * Màu mặc định #4D4D4D (Keycloak brand color).
 * Dùng cho nút SSO login.
 */
@Component({
  selector: 'app-keycloak-icon',
  standalone: true,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
      [style.color]="color()"
      aria-hidden="true"
    >
      <!-- Keycloak logo: shield + keyhole -->
      <path
        [attr.fill]="color()"
        d="M12 1.5L3 5v6.5c0 5.5 3.8 10.7 9 11.5 5.2-.8 9-6 9-11.5V5l-9-3.5zm0 2.2l7 2.7v5.1c0 4.5-3.1 8.7-7 9.4-3.9-.7-7-4.9-7-9.4V6.4l7-2.7z"
      />
      <!-- Keyhole -->
      <circle [attr.fill]="color()" cx="12" cy="10.5" r="2.2" />
      <rect [attr.fill]="color()" x="11" y="10.5" width="2" height="6" rx="1" />
    </svg>
  `,
})
export class KeycloakIconComponent {
  size = input<number>(20);
  color = input<string>('#4D4D4D');
}