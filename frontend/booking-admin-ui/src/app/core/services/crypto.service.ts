// ═══════════════════════════════════════════════════════════
// CRYPTO SERVICE
// Encrypt password bằng JWE compact (RFC 7516):
//   header.encryptedKey.iv.ciphertext.authTag
// Dùng Web Crypto API thuần của browser, không thêm dependency.
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface PublicKeyInfo {
  algorithm: 'RSA-OAEP-256' | string;
  encryption: 'A256GCM' | string;
  publicKey: string; // PEM string
}

@Injectable({
  providedIn: 'root',
})
export class CryptoService {
  private apiUrl = '/api/auth';
  // TODO prod: cache public key theo TTL (vd 5 phút) + dùng key ổn định

  constructor(private http: HttpClient) {}

  /**
   * Encrypt password → JWE compact string.
   * Flow:
   *   1. Generate ngẫu nhiên CEK (32 bytes) + IV (12 bytes)
   *   2. Encrypt password bằng AES-256-GCM với CEK + IV
   *   3. Encrypt CEK bằng RSA public key (RSA-OAEP-256)
   *   4. Build JWE compact: header.encryptedKey.iv.ciphertext.tag
   */
  encryptPassword(plainPassword: string): Observable<string> {
    return this.getOrLoadPublicKey().pipe(
      switchMap((cryptoKey) =>
        from(this.buildJwe(plainPassword, cryptoKey))
      )
    );
  }

  // ── Private helpers ───────────────────────────────────────

  private getOrLoadPublicKey(): Observable<CryptoKey> {
    return this.http
      .get<PublicKeyInfo>(`${this.apiUrl}/public-key`)
      .pipe(
        switchMap((info) => from(this.importPublicKey(info.publicKey))),
        catchError((err) => {
          console.error('[JWE] Failed to load public key', err);
          throw err;
        })
      );
  }

  private async importPublicKey(pem: string): Promise<CryptoKey> {
    const pemBody = pem
      .replace('-----BEGIN PUBLIC KEY-----', '')
      .replace('-----END PUBLIC KEY-----', '')
      .replace(/\s/g, '');
    const binaryDer = this.base64ToArrayBuffer(pemBody);

    return await window.crypto.subtle.importKey(
      'spki',
      binaryDer,
      {
        name: 'RSA-OAEP',
        hash: 'SHA-256',
      },
      false,
      ['encrypt']
    );
  }

  private async buildJwe(
    plainPassword: string,
    publicKey: CryptoKey
  ): Promise<string> {
    // 1. Header (UTF-8 → base64url)
    const header = {
      alg: 'RSA-OAEP-256',
      enc: 'A256GCM',
      typ: 'JWE',
    };
    const headerB64 = this.stringToBase64Url(
      JSON.stringify(header));

    // 2. Generate CEK (32 bytes) + IV (12 bytes)
    const cek = window.crypto.getRandomValues(new Uint8Array(32));
    const iv = window.crypto.getRandomValues(new Uint8Array(12));

    // 3. AES-256-GCM encrypt password
    const data = new TextEncoder().encode(plainPassword);
    const aesKey = await window.crypto.subtle.importKey(
      'raw',
      cek,
      { name: 'AES-GCM' },
      false,
      ['encrypt']
    );
    const cipherBuffer = await window.crypto.subtle.encrypt(
      { name: 'AES-GCM', iv, tagLength: 128 },
      aesKey,
      data
    );
    // Tách ciphertext + authTag (16 bytes cuối)
    const cipherBytes = new Uint8Array(cipherBuffer);
    const tagLength = 16;
    const ciphertext = cipherBytes.slice(0, cipherBytes.length - tagLength);
    const authTag = cipherBytes.slice(cipherBytes.length - tagLength);

    // 4. Encrypt CEK bằng RSA-OAEP-256
    const encryptedCek = new Uint8Array(
      await window.crypto.subtle.encrypt(
        { name: 'RSA-OAEP' },
        publicKey,
        cek
      )
    );

    // 5. Build JWE compact
    return [
      headerB64,
      this.bytesToBase64Url(encryptedCek),
      this.bytesToBase64Url(iv),
      this.bytesToBase64Url(ciphertext),
      this.bytesToBase64Url(authTag),
    ].join('.');
  }

  // ── Encoding helpers (URL-safe base64) ────────────────────

  private stringToBase64Url(s: string): string {
    const bytes = new TextEncoder().encode(s);
    return this.bytesToBase64Url(bytes);
  }

  private bytesToBase64Url(bytes: Uint8Array): string {
    let binary = '';
    const CHUNK = 0x8000;
    for (let i = 0; i < bytes.length; i += CHUNK) {
      const slice = bytes.subarray(i, i + CHUNK);
      binary += String.fromCharCode.apply(
          null, Array.from(slice) as number[]);
    }
    return btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
  }
}
