import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="max-width: 840px; margin: 34px auto; padding: 0 24px;">

      <div style="margin-bottom: 28px;">
        <h2 style="font-size: 1.6rem; font-weight: 800; color: var(--text-main);">Notification Settings</h2>
        <p style="font-size: 0.88rem; color: var(--text-muted); margin-top: 2px;">Manage Telegram channel configuration & automated alert dispatches</p>
      </div>

      <!-- Telegram Channel Setup Card -->
      <div class="glass-card" style="padding: 28px; margin-bottom: 24px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;">
          <div style="display: flex; align-items: center; gap: 14px;">
            <div style="width: 44px; height: 44px; border-radius: 12px; background: rgba(0, 136, 204, 0.12); color: #0088cc; display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
            </div>
            <div>
              <h3 style="font-size: 1.15rem; font-weight: 700; color: var(--text-main);">Telegram Alerts Channel</h3>
              <p style="font-size: 0.82rem; color: var(--text-muted);">Instant price drop notifications formatted via MarkdownV2</p>
            </div>
          </div>

          @if (telegramChatId.trim()) {
            <span class="badge badge-success" style="display: flex; align-items: center; gap: 6px;">
              <div class="pulse-dot"></div>
              CONNECTED
            </span>
          } @else {
            <span class="badge" style="background: rgba(245, 158, 11, 0.12); color: #D97706; border: 1px solid rgba(245, 158, 11, 0.28);">
              NOT CONFIGURED
            </span>
          }
        </div>

        <!-- Instructions Box -->
        <div class="section-card" style="padding: 18px; margin-bottom: 24px;">
          <h4 style="font-size: 0.82rem; font-weight: 700; margin-bottom: 10px; color: var(--accent-primary); text-transform: uppercase; letter-spacing: 0.5px;">Setup Instructions</h4>
          <ol style="font-size: 0.85rem; color: var(--text-muted); padding-left: 20px; line-height: 1.8;">
            <li>Search Telegram for <a href="https://t.me/dropwatch_here_bot" target="_blank" rel="noopener" style="color: var(--accent-primary); font-weight: 600;">&#64;dropwatch_here_bot</a> or click the button below</li>
            <li>Click <strong>START</strong> to activate the bot</li>
            <li>Send a message to <a href="https://t.me/userinfobot" target="_blank" rel="noopener" style="color: var(--accent-primary); font-weight: 600;">&#64;userinfobot</a> to get your numeric Chat ID</li>
            <li>Enter your Chat ID below and click <strong>Save Settings</strong></li>
          </ol>
          <div style="margin-top: 14px;">
            <a href="https://t.me/dropwatch_here_bot" target="_blank" rel="noopener" class="btn-secondary" style="padding: 7px 14px; font-size: 0.82rem;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0088cc" stroke-width="2"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
              Open Telegram Bot (&#64;dropwatch_here_bot)
            </a>
          </div>
        </div>

        <!-- Input Form Row -->
        <div style="display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
          <input
            type="text"
            [(ngModel)]="telegramChatId"
            placeholder="Enter your Telegram Chat ID (numeric)"
            class="glass-input"
            style="flex: 1; min-width: 240px; font-family: var(--font-mono); font-size: 0.95rem;"
          />

          <button (click)="saveSettings()" [disabled]="loading" class="btn-primary">
            @if (saving) {
              <div class="spinner"></div><span>Saving...</span>
            } @else {
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <span>Save Settings</span>
            }
          </button>

          <button (click)="sendTestAlert()" [disabled]="loading || !telegramChatId.trim()" class="btn-secondary">
            @if (testing) {
              <div class="spinner"></div><span>Testing...</span>
            } @else {
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
              <span>Send Test Alert</span>
            }
          </button>
        </div>

      </div>

    </div>
  `
})
export class SettingsComponent implements OnInit {
  private readonly apiService = inject(ApiService);
  private readonly toast = inject(ToastService);

  telegramChatId = '';  // No hardcoded default — loaded from API
  loading = false;
  saving = false;
  testing = false;

  ngOnInit(): void {
    this.fetchConfig();
  }

  fetchConfig(): void {
    this.apiService.getTelegramConfig().subscribe({
      next: (res) => {
        if (res?.chatId) {
          this.telegramChatId = res.chatId;
        }
      },
      error: () => { /* silently fail — user can still enter manually */ }
    });
  }

  saveSettings(): void {
    const chatId = this.telegramChatId.trim();
    if (!chatId) {
      this.toast.error('Invalid Chat ID', 'Please enter a valid numeric Telegram Chat ID');
      return;
    }

    // Basic validation: Telegram Chat IDs are numeric (positive or negative)
    if (!/^-?\d+$/.test(chatId)) {
      this.toast.error('Invalid Format', 'Telegram Chat ID must be a numeric value (e.g. 123456789)');
      return;
    }

    this.saving = true;
    this.loading = true;

    this.apiService.saveTelegramConfig(chatId).subscribe({
      next: (res) => {
        this.saving = false;
        this.loading = false;
        this.toast.success('Telegram Saved!', res.message || 'Telegram Chat ID saved successfully');
      },
      error: (err: { error?: { message?: string } }) => {
        this.saving = false;
        this.loading = false;
        this.toast.error('Save Failed', err?.error?.message || 'Failed to save Telegram Chat ID');
      }
    });
  }

  sendTestAlert(): void {
    const chatId = this.telegramChatId.trim();
    if (!chatId) {
      this.toast.error('Missing Chat ID', 'Please enter a valid Chat ID before sending a test alert');
      return;
    }

    this.testing = true;
    this.loading = true;

    this.apiService.sendTestTelegramAlert(chatId).subscribe({
      next: (res) => {
        this.testing = false;
        this.loading = false;
        this.toast.success('Test Alert Dispatched!', res.message || 'Check your Telegram app for the notification.');
      },
      error: () => {
        this.testing = false;
        this.loading = false;
        this.toast.error('Test Alert Failed', 'Ensure you have opened @dropwatch_here_bot on Telegram and clicked START.');
      }
    });
  }
}
