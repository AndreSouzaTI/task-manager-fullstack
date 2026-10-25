import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Session } from './models';

const STORAGE_KEY = 'task-manager.session';

interface TokenResponse {
  accessToken: string;
  expiresIn: number;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(this.restore());

  readonly user = computed(() => this.session());
  readonly isAuthenticated = computed(() => this.session() !== null);

  get token(): string | null {
    return this.session()?.accessToken ?? null;
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      tap((response) => {
        const session: Session = {
          accessToken: response.accessToken,
          name: response.name,
          email: response.email,
          expiresAt: Date.now() + response.expiresIn * 1000,
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        this.session.set(session);
      }),
    );
  }

  register(name: string, email: string, password: string): Observable<unknown> {
    return this.http.post('/api/auth/register', { name, email, password });
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigate(['/login']);
  }

  /** Recupera a sessão salva, descartando tokens expirados. */
  private restore(): Session | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      const session = raw ? (JSON.parse(raw) as Session) : null;
      return session && session.expiresAt > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
