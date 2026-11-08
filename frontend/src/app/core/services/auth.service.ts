import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { LoginRequest, LoginResponse, RegisterRequest, Role, User } from '../models/auth.model';

const TOKEN_KEY = 'dor_token';
const SESSION_KEY = 'dor_session';

interface Session {
  fullName: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/auth';

  private readonly session = signal<Session | null>(this.readSession());

  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly fullName = computed(() => this.session()?.fullName ?? '');

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.saveSession(response)));
  }

  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/register`, request);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(SESSION_KEY);
    this.session.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private saveSession(response: LoginResponse): void {
    const session: Session = { fullName: response.fullName, role: response.role };
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private readSession(): Session | null {
    if (!localStorage.getItem(TOKEN_KEY)) {
      return null;
    }
    try {
      return JSON.parse(localStorage.getItem(SESSION_KEY) ?? 'null') as Session | null;
    } catch {
      return null;
    }
  }
}
