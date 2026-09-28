import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FlagStore, type FeatureFlag } from '../../services/flag.store';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-flags-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="max-width: 1040px; margin: 34px auto; padding: 0 24px;">

      <!-- Header -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
        <div>
          <h2 style="font-size: 1.6rem; font-weight: 800; color: var(--text-main);">Feature Flags & Rollouts</h2>
          <p style="font-size: 0.88rem; color: var(--text-muted); margin-top: 2px;">First-party flag engine backed by MongoDB & RabbitMQ fanout broadcast</p>
        </div>
        <button (click)="showCreateForm.set(!showCreateForm())" class="btn-primary" style="font-size: 0.85rem; padding: 8px 18px;">
          @if (showCreateForm()) {
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            Cancel
          } @else {
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            New Flag
          }
        </button>
      </div>

      <!-- Create Flag Inline Form -->
      @if (showCreateForm()) {
        <div class="glass-card" style="padding: 22px; margin-bottom: 24px; animation: slideUp 0.2s ease; border-color: rgba(99,102,241,0.3);">
          <h3 style="font-size: 1rem; font-weight: 700; color: var(--text-main); margin-bottom: 16px;">Create New Feature Flag</h3>
          <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 12px; margin-bottom: 14px;">
            <div>
              <label style="font-size: 0.78rem; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.4px;">Flag Key</label>
              <input
                type="text"
                [(ngModel)]="newFlagKey"
                placeholder="e.g. ui.dark-sidebar"
                class="glass-input"
                style="width: 100%; font-family: var(--font-mono); font-size: 0.9rem;"
              />
            </div>
            <div>
              <label style="font-size: 0.78rem; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.4px;">Description</label>
              <input
                type="text"
                [(ngModel)]="newFlagDescription"
                placeholder="What does this flag control?"
                class="glass-input"
                style="width: 100%;"
              />
            </div>
          </div>
          <div style="display: flex; justify-content: flex-end; gap: 10px;">
            <button (click)="showCreateForm.set(false)" class="btn-secondary" style="font-size: 0.84rem;">Cancel</button>
            <button (click)="createFlag()" [disabled]="!newFlagKey.trim()" class="btn-primary" style="font-size: 0.84rem; padding: 9px 20px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              Create Flag
            </button>
          </div>
        </div>
      }

      <!-- Flags Table -->
      <div class="glass-card" style="padding: 0; overflow: hidden;">
        <table style="width: 100%; border-collapse: collapse; text-align: left;">
          <thead>
            <tr style="border-bottom: 1px solid var(--card-border); background: var(--section-card-bg); font-size: 0.77rem; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-muted);">
              <th style="padding: 14px 20px;">Flag Key</th>
              <th style="padding: 14px 20px;">Status</th>
              <th style="padding: 14px 20px;">Rollout</th>
              <th style="padding: 14px 20px;">Target Users</th>
              <th style="padding: 14px 20px; text-align: right;">Toggle</th>
            </tr>
          </thead>
          <tbody>
            @for (flag of flagStore.flags(); track flag.key) {
              <tr style="border-bottom: 1px solid var(--section-card-border); transition: background-color 0.15s ease;">
                <td style="padding: 15px 20px;">
                  <div style="font-weight: 700; font-family: var(--font-mono); color: var(--accent-primary); font-size: 0.88rem;">{{ flag.key }}</div>
                  <div style="font-size: 0.78rem; color: var(--text-muted); margin-top: 2px;">{{ flag.description }}</div>
                </td>
                <td style="padding: 15px 20px;">
                  <span [class]="flag.enabled ? 'badge badge-success' : 'badge'">
                    {{ flag.enabled ? 'ENABLED' : 'DISABLED' }}
                  </span>
                </td>
                <td style="padding: 15px 20px; font-weight: 600; font-family: var(--font-mono); color: var(--text-main); font-size: 0.9rem;">
                  {{ flag.rolloutPercentage }}%
                </td>
                <td style="padding: 15px 20px; color: var(--text-muted); font-size: 0.84rem;">
                  {{ flag.targetUsers?.length ? flag.targetUsers.join(', ') : 'All users' }}
                </td>
                <td style="padding: 15px 20px; text-align: right;">
                  <!-- Toggle Switch -->
                  <button
                    (click)="toggleFlag(flag)"
                    class="toggle-switch"
                    [class.toggle-switch--on]="flag.enabled"
                    [title]="flag.enabled ? 'Disable ' + flag.key : 'Enable ' + flag.key"
                    [attr.aria-label]="(flag.enabled ? 'Disable' : 'Enable') + ' ' + flag.key"
                  >
                    <span class="toggle-knob"></span>
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

    </div>
  `,
  styles: [`
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(-8px); }
      to { opacity: 1; transform: translateY(0); }
    }

    tr:hover { background: var(--section-card-bg) !important; }

    .toggle-switch {
      position: relative;
      width: 44px;
      height: 24px;
      border-radius: 12px;
      border: none;
      background: var(--btn-secondary-bg);
      border: 1px solid var(--btn-secondary-border);
      cursor: pointer;
      transition: background-color 0.25s ease, border-color 0.25s ease;
      padding: 0;
    }

    .toggle-switch--on {
      background: rgba(16, 185, 129, 0.2);
      border-color: rgba(16, 185, 129, 0.5);
    }

    .toggle-knob {
      position: absolute;
      top: 3px;
      left: 3px;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: var(--text-dim);
      transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1), background-color 0.25s ease;
    }

    .toggle-switch--on .toggle-knob {
      transform: translateX(20px);
      background: #10B981;
      box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
    }
  `]
})
export class FlagsAdminComponent implements OnInit {
  readonly flagStore = inject(FlagStore);
  private readonly toast = inject(ToastService);

  readonly showCreateForm = signal(false);
  newFlagKey = '';
  newFlagDescription = '';

  ngOnInit(): void {
    this.flagStore.loadFlags();
  }

  toggleFlag(flag: FeatureFlag): void {
    this.flagStore.toggleFlag(flag, () => {
      this.toast.success(
        flag.enabled ? 'Flag Disabled' : 'Flag Enabled',
        `${flag.key} is now ${!flag.enabled ? 'enabled' : 'disabled'}`
      );
    });
  }

  createFlag(): void {
    const key = this.newFlagKey.trim();
    if (!key) return;

    this.flagStore.createFlag(key, this.newFlagDescription || 'Custom feature flag');
    this.toast.success('Flag Created', `${key} flag has been created`);
    this.newFlagKey = '';
    this.newFlagDescription = '';
    this.showCreateForm.set(false);
  }
}
