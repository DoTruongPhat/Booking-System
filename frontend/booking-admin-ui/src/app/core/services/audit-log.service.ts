import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditLogFilter, AuditLogPage } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/admin/audit-logs';

  getAuditLogs(filter: AuditLogFilter = {}): Observable<AuditLogPage> {
    let params = new HttpParams()
      .set('page', String(filter.page ?? 0))
      .set('size', String(filter.size ?? 20));

    Object.entries(filter).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '' && key !== 'page' && key !== 'size') {
        params = params.set(key, String(value));
      }
    });

    return this.http.get<AuditLogPage>(this.baseUrl, { params, withCredentials: true });
  }
}
