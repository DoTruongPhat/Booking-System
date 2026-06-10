// ═══════════════════════════════════════════════════════════
// TICKET SERVICE
// Gọi các endpoint /api/tickets và /api/admin/tickets
// ═══════════════════════════════════════════════════════════

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupportTicket, SpringPage, TicketStatus } from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class TicketService {
  private apiUrl = '/api/tickets';
  private adminUrl = '/api/admin/tickets';

  constructor(private http: HttpClient) {}

  // ── USER ENDPOINTS ─────────────────────────────────────

  // User tạo ticket mới
  createTicket(body: { title: string; description: string; priority?: string }): Observable<SupportTicket> {
    return this.http.post<SupportTicket>(this.apiUrl, body);
  }

  // User xem tickets của mình
  getMyTickets(page: number = 0, size: number = 10, status?: TicketStatus): Observable<SpringPage<SupportTicket>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (status) params = params.set('status', status);
    return this.http.get<SpringPage<SupportTicket>>(this.apiUrl, { params });
  }

  // User xem chi tiết 1 ticket
  getTicketById(id: string): Observable<SupportTicket> {
    return this.http.get<SupportTicket>(`${this.apiUrl}/${id}`);
  }

  // ── STAFF ENDPOINTS (tickets được assign cho mình) ──────

  // Staff xem tickets được assign (filter status optional)
  getAssignedTickets(
    page: number = 0,
    size: number = 10,
    status?: TicketStatus,
  ): Observable<SpringPage<SupportTicket>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (status) params = params.set('status', status);
    return this.http.get<SpringPage<SupportTicket>>(
      `${this.apiUrl}/assigned`,
      { params }
    );
  }

  // Staff xem chi tiết 1 ticket được assign (có security check ở BE)
  getAssignedTicketById(id: string): Observable<SupportTicket> {
    return this.http.get<SupportTicket>(`${this.apiUrl}/assigned/${id}`);
  }

  // ── ADMIN ENDPOINTS ─────────────────────────────────────

  // Admin list tất cả tickets
  getAllTickets(
    page: number = 0,
    size: number = 10,
    status?: TicketStatus,
  ): Observable<SpringPage<SupportTicket>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (status) params = params.set('status', status);
    return this.http.get<SpringPage<SupportTicket>>(this.adminUrl, { params });
  }

  // Admin assign ticket cho staff
  assignTicket(ticketId: string, staffId: string): Observable<SupportTicket> {
    return this.http.put<SupportTicket>(
      `${this.adminUrl}/${ticketId}/assign`,
      null,
      { params: new HttpParams().set('staffId', staffId) }
    );
  }

  // Admin hoặc Staff update status ticket
  updateTicketStatus(ticketId: string, status: TicketStatus): Observable<SupportTicket> {
    return this.http.put<SupportTicket>(
      `${this.adminUrl}/${ticketId}/status`,
      null,
      { params: new HttpParams().set('status', status) }
    );
  }
}
