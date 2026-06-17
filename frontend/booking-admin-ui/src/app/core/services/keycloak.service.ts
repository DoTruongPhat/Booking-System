import { Injectable } from '@angular/core';

/**
 * KeycloakService - xử lý PKCE flow
 *
 * LUỒNG:
 *   1. Generate code_verifier (random 32 bytes)
 *   2. SHA256(code_verifier) → code_challenge
 *   3. Redirect Keycloak với code_challenge
 *   4. Keycloak trả code về FE
 *   5. FE gửi code + code_verifier lên BE exchange
 */
@Injectable({ providedIn: 'root' })
export class KeycloakService {

  private readonly kcBaseUrl = 'http://localhost:8180';
  private readonly realm = 'smartbooking';
  private readonly clientId = 'smartbooking-fe';
  // Lấy redirectUri khi cần (tránh SSR error window not defined)
  private get redirectUri(): string {
    if (typeof window === 'undefined') return '';
    return window.location.origin + '/auth/callback';
  }

  // 1. Generate random 32 bytes → base64url (code_verifier)
  private generateCodeVerifier(): string {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return this.base64UrlEncode(array);
  }

  // 2. SHA256(code_verifier) → base64url (code_challenge)
  private async generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return this.base64UrlEncode(new Uint8Array(hash));
  }

  // 3. base64url encode (URL-safe base64)
  private base64UrlEncode(bytes: Uint8Array): string {
    let binary = '';
    for (let i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  // 4. Build URL redirect sang Keycloak
  async getAuthorizationUrl(): Promise<string> {
    const codeVerifier = this.generateCodeVerifier();
    const codeChallenge = await this.generateCodeChallenge(codeVerifier);

    // Lưu code_verifier vào sessionStorage (xóa sau khi dùng)
    sessionStorage.setItem('kc_code_verifier', codeVerifier);

    const params = new URLSearchParams({
      client_id: this.clientId,
      response_type: 'code',
      redirect_uri: this.redirectUri,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
      scope: 'openid profile email',
    });

    return `${this.kcBaseUrl}/realms/${this.realm}/protocol/openid-connect/auth?${params}`;
  }

  // 5. Lấy code_verifier đã lưu (sau khi Keycloak redirect về)
  getCodeVerifier(): string | null {
    return sessionStorage.getItem('kc_code_verifier');
  }

  // 6. Xóa code_verifier (sau khi exchange thành công)
  clearCodeVerifier(): void {
    sessionStorage.removeItem('kc_code_verifier');
  }

  // 7. Lấy redirect_uri (gửi lên BE để verify)
  getRedirectUri(): string {
    if (typeof window === 'undefined') return '';
    return window.location.origin + '/auth/callback';
  }
}
