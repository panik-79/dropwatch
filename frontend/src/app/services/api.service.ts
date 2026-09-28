import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JWT_STORAGE_KEY } from '../constants';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  private getHeaders(): HttpHeaders {
    const token = (typeof window !== 'undefined' && localStorage.getItem(JWT_STORAGE_KEY)) || '';
    let headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  // ─── Auth ────────────────────────────────────────────────────────────────
  register(data: { email: string; password: string; fullName: string }): Observable<{ token: string; user: unknown }> {
    return this.http.post<{ token: string; user: unknown }>(`${this.baseUrl}/auth/register`, data);
  }

  login(data: { email: string; password: string }): Observable<{ token: string; user: unknown }> {
    return this.http.post<{ token: string; user: unknown }>(`${this.baseUrl}/auth/login`, data);
  }

  getProfile(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/auth/me`, { headers: this.getHeaders() });
  }

  // ─── Products ─────────────────────────────────────────────────────────────
  parseUrl(url: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/products/parse-url`, { url }, { headers: this.getHeaders() });
  }

  getProduct(id: string): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/products/${id}`, { headers: this.getHeaders() });
  }

  searchProducts(query: string): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/products/search?query=${encodeURIComponent(query)}`, { headers: this.getHeaders() });
  }

  getPriceHistory(id: string, sku = 'STANDARD'): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/products/${id}/history?variantSku=${sku}`, { headers: this.getHeaders() });
  }

  // ─── Trackers ─────────────────────────────────────────────────────────────
  getTrackers(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/trackers`, { headers: this.getHeaders() });
  }

  createTracker(data: unknown): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/trackers`, data, { headers: this.getHeaders() });
  }

  updateTracker(id: string, data: unknown): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/trackers/${id}`, data, { headers: this.getHeaders() });
  }

  deleteTracker(id: string): Observable<unknown> {
    return this.http.delete(`${this.baseUrl}/trackers/${id}`, { headers: this.getHeaders() });
  }

  pauseTracker(id: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/trackers/${id}/pause`, {}, { headers: this.getHeaders() });
  }

  resumeTracker(id: string): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/trackers/${id}/resume`, {}, { headers: this.getHeaders() });
  }

  // ─── Alerts ───────────────────────────────────────────────────────────────
  getAlerts(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/alerts`, { headers: this.getHeaders() });
  }

  // ─── Feature Flags ────────────────────────────────────────────────────────
  getFlags(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/flags`, { headers: this.getHeaders() });
  }

  saveFlag(data: unknown): Observable<unknown> {
    return this.http.post(`${this.baseUrl}/flags`, data, { headers: this.getHeaders() });
  }

  // ─── Channels & Notifications ─────────────────────────────────────────────
  getTelegramConfig(): Observable<{ chatId: string }> {
    return this.http.get<{ chatId: string }>(`${this.baseUrl}/channels/telegram/config`, { headers: this.getHeaders() });
  }

  saveTelegramConfig(chatId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/channels/telegram/config`, { chatId }, { headers: this.getHeaders() });
  }

  sendTestTelegramAlert(chatId: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/channels/telegram/test-alert`, { chatId }, { headers: this.getHeaders() });
  }
}
