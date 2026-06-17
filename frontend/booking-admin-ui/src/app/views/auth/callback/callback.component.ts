import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { Auth } from '../../../core/services/auth';
import { KeycloakService } from '../../../core/services/keycloak.service';

@Component({
  selector: 'app-callback',
  standalone: true,
  imports: [CommonModule, NzIconModule, NzButtonModule],
  templateUrl: './callback.component.html',
  styleUrls: ['./callback.component.scss']
})
export class CallbackComponent implements OnInit {

  errorMessage = '';
  isProcessing = true;

  // router cần public để dùng trong template
  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private auth: Auth,
    private keycloak: KeycloakService
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    const error = this.route.snapshot.queryParamMap.get('error');

    if (error) {
      this.errorMessage = this.route.snapshot.queryParamMap.get('error_description') || error;
      this.isProcessing = false;
      return;
    }

    if (!code) {
      this.errorMessage = 'Missing authorization code from Keycloak';
      this.isProcessing = false;
      return;
    }

    const codeVerifier = this.keycloak.getCodeVerifier();
    if (!codeVerifier) {
      this.errorMessage = 'Missing code_verifier. Please try again.';
      this.isProcessing = false;
      return;
    }

    const redirectUri = this.keycloak.getRedirectUri();

    this.auth.exchangeCode(code, codeVerifier, redirectUri).subscribe({
      next: (response: any) => {
        if (response.twoFactorRequired) {
          this.keycloak.clearCodeVerifier();
          this.router.navigate(['/auth/verify-2fa'], {
            queryParams: { mfaToken: response.mfaSessionToken }
          });
          return;
        }

        this.auth.saveUser({
          username: response.username,
          email: response.email,
          roles: response.roles
        });

        this.keycloak.clearCodeVerifier();
        this.router.navigateByUrl(this.auth.getLandingPath());
      },
      error: (err: any) => {
        console.error('[Callback] Exchange failed:', err);
        this.errorMessage = err?.error?.message || 'Login failed';
        this.isProcessing = false;
        this.keycloak.clearCodeVerifier();
      }
    });
  }
}
