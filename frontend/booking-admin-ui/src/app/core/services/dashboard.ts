import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class Dashboard {
  private apiUrl = '/api/admin/dashboard';

  constructor(private http: HttpClient) {}

  // Gọi API lấy stats
  getStats(): Observable<any> {
    return this.http.get<any>(this.apiUrl);
  }
}
