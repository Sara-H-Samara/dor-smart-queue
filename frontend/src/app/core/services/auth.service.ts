import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, RegisterRequest, Role, RegisteredUser } from '../models/auth.model';

const SESSION_STORAGE_KEY = 'dor_auth';
const VALID_ROLES: readonly Role[] = ['STUDENT', 'STAFF', 'ADMIN'];

interface Session {
  token: string;
  fullName: string;
  role: Role;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;

  private readonly session = signal<Session | null>(this.loadStoredSession());

  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly fullName = computed(() => this.session()?.fullName ?? '');

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((response) => this.storeAndActivateSession(response)));
  }

  register(request: RegisterRequest): Observable<RegisteredUser> {
    return this.http.post<RegisteredUser>(`${this.apiUrl}/register`, request);
  }

  logout(): void {
    this.removeStoredSession();
    this.session.set(null);
  }

  getToken(): string | null {
    return this.session()?.token ?? null;
  }

  private storeAndActivateSession(response: LoginResponse): void {
    const session: Session = {
      token: response.token,
      fullName: response.fullName,
      role: response.role,
    };
    try {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Storage is blocked: the session still works in memory until reload.
    }
    this.session.set(session);
  }

  /** Reads the saved session; also clears the stored data if it is corrupt. */
  private loadStoredSession(): Session | null {
    let raw: string | null;
    try {
      raw = localStorage.getItem(SESSION_STORAGE_KEY);
    } catch {
      return null;
    }
    if (raw === null) {
      return null;
    }

    try {
      const parsedJson: unknown = JSON.parse(raw);
      if (typeof parsedJson === 'object' && parsedJson !== null) {
        const { token, fullName, role } = parsedJson as Record<string, unknown>;
        if (
          typeof token === 'string' &&
          token.length > 0 &&
          typeof fullName === 'string' &&
          typeof role === 'string' &&
          (VALID_ROLES as readonly string[]).includes(role)
        ) {
          return { token, fullName, role: role as Role };
        }
      }
    } catch {
      // Invalid JSON falls through to clearing the storage.
    }
    this.removeStoredSession();
    return null;
  }

  private removeStoredSession(): void {
    try {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {
      // Ignore: storage is blocked.
    }
  }
}
