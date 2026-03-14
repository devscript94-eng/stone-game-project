import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api/api.config';
import { AuthResponse } from '../../shared/models/auth-response';
import { UserProfile } from '../../shared/models/user-profile';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenKey = 'stonegame_token';

  register(payload: { username: string; email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/register`, payload).pipe(
      tap(response => this.storeToken(response.token))
    );
  }

  login(payload: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, payload).pipe(
      tap(response => this.storeToken(response.token))
    );
  }

  me(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API_BASE_URL}/users/me`);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }
}