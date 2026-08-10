import { HttpHeaders } from '@angular/common/http';

export const IDEMPOTENCY_HEADER = 'Idempotency-Key';

export function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = (Math.random() * 16) | 0;
    const value = char === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

export function withIdempotencyHeader<T extends object>(
  options: T,
  key?: string,
): T & { headers?: HttpHeaders } {
  if (!key) {
    return options;
  }

  const currentHeaders = (options as { headers?: HttpHeaders }).headers;
  const headers = (currentHeaders ?? new HttpHeaders()).set(IDEMPOTENCY_HEADER, key);
  return { ...options, headers };
}
