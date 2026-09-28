import { Component, inject, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthStore } from '../../services/auth.store';
import { SseService } from '../../services/sse.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="nav-header">
      <div style="max-width: 1320px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between; padding: 0 4px;">

        <!-- Logo & Brand -->
        <div style="display: flex; align-items: center; gap: 14px; cursor: pointer; user-select: none; flex-shrink: 0;" routerLink="/">
          <div class="logo-box">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 2v20M2 12h20M4.93 4.93l14.14 14.14M4.93 19.07l14.14-14.14"/>
            </svg>
          </div>
          <div>
            <div style="display: flex; align-items: center; gap: 8px;">
              <h1 style="font-size: 1.35rem; font-weight: 800; letter-spacing: -0.03em; color: var(--text-main); line-height: 1;">DropWatch</h1>
              <span class="pro-tag">PRO</span>
            </div>
            <span style="font-size: 0.68rem; color: var(--text-dim); letter-spacing: 0.8px; text-transform: uppercase; font-weight: 600;">Price Intelligence</span>
          </div>
        </div>

        <!-- Center Nav Tabs -->
        <div class="nav-tabs-wrapper">
          <a routerLink="/" routerLinkActive="active-nav" [routerLinkActiveOptions]="{exact: true}" class="nav-btn">Dashboard</a>
          <a routerLink="/flags" routerLinkActive="active-nav" class="nav-btn">Feature Flags</a>
          <a routerLink="/settings" routerLinkActive="active-nav" class="nav-btn">Settings</a>
        </div>

        <!-- Right Tools -->
        <div style="display: flex; align-items: center; gap: 10px; flex-shrink: 0;">

          <!-- Command Palette Trigger -->
          <button (click)="openPalette.emit()" class="btn-secondary" style="padding: 7px 13px; font-size: 0.82rem;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
            <span>Search</span>
            <span style="font-size: 0.68rem; background: var(--btn-secondary-bg); color: var(--text-muted); padding: 2px 6px; border-radius: 4px; font-family: var(--font-mono);">⌘K</span>
          </button>

          <!-- SSE Stream Indicator -->
          <div
            [title]="sseService.isConnected() ? 'Live price stream connected' : 'Reconnecting... attempt #' + sseService.reconnectAttempts()"
            [class]="sseService.isConnected() ? 'sse-badge sse-badge--live' : 'sse-badge sse-badge--reconnecting'"
          >
            @if (sseService.isConnected()) {
              <div class="pulse-dot"></div>
              <span>LIVE</span>
            } @else {
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="animation: spin 1.2s linear infinite;">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
              <span>{{ sseService.reconnectAttempts() > 0 ? 'RETRY #' + sseService.reconnectAttempts() : 'CONNECTING' }}</span>
            }
          </div>

          <!-- Theme Toggle -->
          <button
            (click)="themeService.toggle()"
            class="btn-ghost theme-toggle-btn"
            [title]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'"
            aria-label="Toggle theme"
          >
            @if (themeService.isDark()) {
              <!-- Sun icon for "switch to light" -->
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <circle cx="12" cy="12" r="4"/>
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/>
              </svg>
            } @else {
              <!-- Moon icon for "switch to dark" -->
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
              </svg>
            }
          </button>

          <!-- Add Tracker Button -->
          <button (click)="openAddModal.emit()" class="btn-primary" style="font-size: 0.85rem; padding: 8px 18px;">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            <span>Add Tracker</span>
          </button>

          <!-- User Profile Avatar -->
          @if (authStore.user()) {
            <div style="display: flex; align-items: center; padding-left: 8px; border-left: 1px solid var(--card-border);">
              <div class="avatar-circle" [title]="authStore.user()?.email || ''">
                {{ authStore.user()?.fullName?.charAt(0)?.toUpperCase() || 'U' }}
              </div>
            </div>
          }
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .logo-box {
      width: 40px;
      height: 40px;
      border-radius: 12px;
      background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
      flex-shrink: 0;
    }

    .pro-tag {
      font-size: 0.62rem;
      font-weight: 800;
      background: rgba(99, 102, 241, 0.15);
      color: #6366F1;
      border: 1px solid rgba(99, 102, 241, 0.3);
      padding: 1px 5px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }

    .nav-tabs-wrapper {
      display: flex;
      align-items: center;
      gap: 4px;
      background: var(--filter-bar-bg);
      padding: 4px;
      border-radius: 12px;
      border: 1px solid var(--filter-bar-border);
      transition: background-color 0.3s ease;
    }

    .nav-btn {
      color: var(--text-muted);
      text-decoration: none;
      padding: 6px 14px;
      border-radius: 8px;
      font-size: 0.84rem;
      font-weight: 600;
      font-family: var(--font-body);
      transition: color 0.15s ease, background-color 0.15s ease;
    }

    .nav-btn:hover {
      color: var(--text-main);
      background: var(--btn-secondary-bg);
    }

    .active-nav {
      color: var(--text-main) !important;
      background: rgba(99, 102, 241, 0.22) !important;
      border: 1px solid rgba(99, 102, 241, 0.35);
    }

    .sse-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 5px 11px;
      border-radius: 20px;
      font-size: 0.7rem;
      font-weight: 700;
      letter-spacing: 0.5px;
      transition: all 0.3s ease;
    }

    .sse-badge--live {
      background: rgba(16, 185, 129, 0.08);
      border: 1px solid rgba(16, 185, 129, 0.22);
      color: #10B981;
    }

    .sse-badge--reconnecting {
      background: rgba(245, 158, 11, 0.08);
      border: 1px solid rgba(245, 158, 11, 0.22);
      color: #F59E0B;
    }

    .theme-toggle-btn {
      width: 36px;
      height: 36px;
      padding: 0;
      border-radius: 10px;
      color: var(--text-muted);
      border: 1px solid var(--card-border);
      transition: all 0.2s ease;
    }

    .theme-toggle-btn:hover {
      color: var(--accent-primary);
      border-color: rgba(99, 102, 241, 0.4);
      background: rgba(99, 102, 241, 0.08);
      transform: rotate(15deg);
    }

    .avatar-circle {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: linear-gradient(135deg, #6366F1, #4F46E5);
      border: 2px solid rgba(99, 102, 241, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: #FFF;
      font-size: 0.85rem;
      cursor: pointer;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class NavbarComponent {
  readonly authStore = inject(AuthStore);
  readonly sseService = inject(SseService);
  readonly themeService = inject(ThemeService);

  @Output() openAddModal = new EventEmitter<void>();
  @Output() openPalette = new EventEmitter<void>();
}
