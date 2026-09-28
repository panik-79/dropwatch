import { Injectable, signal, computed, inject } from '@angular/core';
import { ApiService } from './api.service';
import { type UserProfile } from '../models/tracker.models';
import { JWT_STORAGE_KEY } from '../constants';

export type { UserProfile };

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(ApiService);

  readonly user = signal<UserProfile | null>(null);
  readonly token = signal<string | null>(
    typeof localStorage !== 'undefined' ? localStorage.getItem(JWT_STORAGE_KEY) : null
  );
  readonly isAuthenticated = computed(() => !!this.token());
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  constructor() {
    if (this.token()) {
      this.fetchProfile();
    }
  }

  fetchProfile(): void {
    this.loading.set(true);
    this.api.getProfile().subscribe({
      next: (profile) => {
        this.user.set(profile as UserProfile);
        this.loading.set(false);
      },
      error: () => {
        // Backend might be offline; clear loading but keep any token
        this.loading.set(false);
      }
    });
  }

  login(credentials: { email: string; password: string }, onSuccess?: () => void, onError?: (msg: string) => void): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.login(credentials).subscribe({
      next: (res) => {
        const { token, user } = res as { token: string; user: UserProfile };
        localStorage.setItem(JWT_STORAGE_KEY, token);
        this.token.set(token);
        this.user.set(user);
        this.loading.set(false);
        onSuccess?.();
      },
      error: (err: { error?: { message?: string } }) => {
        const msg = err?.error?.message || 'Login failed';
        this.error.set(msg);
        this.loading.set(false);
        onError?.(msg);
      }
    });
  }

  register(data: { email: string; password: string; fullName: string }, onSuccess?: () => void, onError?: (msg: string) => void): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.register(data).subscribe({
      next: (res) => {
        const { token, user } = res as { token: string; user: UserProfile };
        localStorage.setItem(JWT_STORAGE_KEY, token);
        this.token.set(token);
        this.user.set(user);
        this.loading.set(false);
        onSuccess?.();
      },
      error: (err: { error?: { message?: string } }) => {
        const msg = err?.error?.message || 'Registration failed';
        this.error.set(msg);
        this.loading.set(false);
        onError?.(msg);
      }
    });
  }

  logout(): void {
    localStorage.removeItem(JWT_STORAGE_KEY);
    this.token.set(null);
    this.user.set(null);
  }
}
