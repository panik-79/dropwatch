import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TrackerStore } from '../../services/tracker.store';
import { ToastService } from '../../services/toast.service';
import { type RuleType, type TrackerRule } from '../../models/tracker.models';
import {
  DEFAULT_TARGET_PRICE,
  DEFAULT_POLL_INTERVAL_SECONDS,
  DEFAULT_COOLDOWN_SECONDS,
  PRICE_PRESETS,
  POLL_INTERVAL_OPTIONS,
  COOLDOWN_OPTIONS,
} from '../../constants';

@Component({
  selector: 'app-add-tracker-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="glass-card modal-content" (click)="$event.stopPropagation()" style="max-width: 560px; max-height: 88vh; display: flex; flex-direction: column; padding: 22px; overflow: hidden;">

        <!-- Fixed Header -->
        <div style="flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--card-border); padding-bottom: 14px;">
          <div>
            <h2 style="font-size: 1.2rem; font-weight: 700; color: var(--text-main);">Track New Product</h2>
            <p style="font-size: 0.78rem; color: var(--text-muted); margin-top: 2px;">Paste a link from Myntra or Flipkart to monitor price drops</p>
          </div>
          <button (click)="close.emit()" class="close-btn">&times;</button>
        </div>

        <!-- Scrollable Modal Body -->
        <div class="modal-body">

          <!-- URL Input -->
          <div>
            <label class="field-label">Product Merchant URL</label>
            <input
              type="text"
              [(ngModel)]="targetUrl"
              placeholder="e.g. https://www.flipkart.com/... or https://www.myntra.com/..."
              class="glass-input"
              style="width: 100%; margin-top: 6px;"
              (keyup.enter)="submitTracker()"
              autofocus
            />
          </div>

          <!-- Alert Rule Type Tabs -->
          <div>
            <label class="field-label" style="display: block; margin-bottom: 6px;">Alert Trigger Type</label>
            <div class="filter-bar" style="width: 100%; display: grid; grid-template-columns: 1fr 1fr 1fr;">
              <button
                type="button"
                (click)="selectedRuleType = 'TARGET_PRICE'"
                [class.active-filter]="selectedRuleType === 'TARGET_PRICE'"
                class="filter-pill"
                style="justify-content: center;"
              >
                🎯 Target Price
              </button>
              <button
                type="button"
                (click)="selectedRuleType = 'PERCENT_DROP'"
                [class.active-filter]="selectedRuleType === 'PERCENT_DROP'"
                class="filter-pill"
                style="justify-content: center;"
              >
                📉 % Drop
              </button>
              <button
                type="button"
                (click)="selectedRuleType = 'BACK_IN_STOCK'"
                [class.active-filter]="selectedRuleType === 'BACK_IN_STOCK'"
                class="filter-pill"
                style="justify-content: center;"
              >
                📦 Back in Stock
              </button>
            </div>
          </div>

          <!-- Target Price Config -->
          @if (selectedRuleType === 'TARGET_PRICE') {
            <div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                <label class="field-label">Target Price Limit (₹)</label>
                <span style="font-size: 0.72rem; color: var(--accent-primary);">Alert fires when price ≤ target</span>
              </div>
              <input
                type="number"
                [(ngModel)]="targetPrice"
                placeholder="e.g. 4999"
                class="glass-input"
                min="1"
                style="width: 100%; font-family: var(--font-mono); font-size: 1.05rem; font-weight: 700; padding: 8px 14px;"
              />
              <div style="display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap;">
                @for (preset of pricePresets; track preset) {
                  <button type="button" (click)="targetPrice = preset" class="btn-secondary" style="padding: 3px 8px; font-size: 0.72rem;">
                    ₹{{ preset.toLocaleString('en-IN') }}
                  </button>
                }
              </div>
            </div>
          }

          <!-- Percent Drop Config -->
          @if (selectedRuleType === 'PERCENT_DROP') {
            <div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                <label class="field-label">Price Drop Percentage Threshold (%)</label>
                <span style="font-size: 0.72rem; color: #10B981;">Alert fires when price drops ≥ threshold</span>
              </div>
              <input
                type="number"
                [(ngModel)]="percentDrop"
                placeholder="e.g. 15"
                class="glass-input"
                min="1"
                max="99"
                style="width: 100%; font-family: var(--font-mono); font-size: 1.05rem; font-weight: 700; padding: 8px 14px;"
              />
              <div style="display: flex; gap: 6px; margin-top: 8px; flex-wrap: wrap;">
                @for (pct of [10, 15, 20, 25, 30, 40, 50]; track pct) {
                  <button type="button" (click)="percentDrop = pct" class="btn-secondary" style="padding: 3px 8px; font-size: 0.72rem;">
                    {{ pct }}% Drop
                  </button>
                }
              </div>
            </div>
          }

          <!-- Back in Stock Info -->
          @if (selectedRuleType === 'BACK_IN_STOCK') {
            <div style="background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.25); padding: 12px; border-radius: 10px;">
              <div style="font-size: 0.82rem; font-weight: 700; color: #10B981; margin-bottom: 2px;">📦 Back In Stock Monitoring Active</div>
              <div style="font-size: 0.74rem; color: var(--text-muted);">You will receive an instant alert as soon as this item becomes available for order again.</div>
            </div>
          }

          <!-- Advanced Settings (expandable) -->
          <div>
            <button (click)="showAdvanced = !showAdvanced" class="btn-ghost" style="font-size: 0.78rem; width: 100%; justify-content: space-between; padding: 4px 8px;">
              <span>Advanced Settings</span>
              <svg [style.transform]="showAdvanced ? 'rotate(180deg)' : ''" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" style="transition: transform 0.2s ease;"><polyline points="6 9 12 15 18 9"/></svg>
            </button>

            @if (showAdvanced) {
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 10px; animation: slideUp 0.2s ease;">
                <div>
                  <label class="field-label" style="margin-bottom: 4px; display: block;">Check Frequency</label>
                  <select [(ngModel)]="pollIntervalSeconds" class="glass-input" style="width: 100%; font-size: 0.82rem; padding: 6px 10px;">
                    @for (opt of pollIntervalOptions; track opt.value) {
                      <option [value]="opt.value">{{ opt.label }}</option>
                    }
                  </select>
                </div>
                <div>
                  <label class="field-label" style="margin-bottom: 4px; display: block;">Alert Cooldown</label>
                  <select [(ngModel)]="cooldownSeconds" class="glass-input" style="width: 100%; font-size: 0.82rem; padding: 6px 10px;">
                    @for (opt of cooldownOptions; track opt.value) {
                      <option [value]="opt.value">{{ opt.label }}</option>
                    }
                  </select>
                </div>
              </div>
            }
          </div>

          <!-- Telegram Info Box -->
          <div style="background: var(--info-card-bg); border: 1px solid var(--info-card-border); padding: 10px 12px; border-radius: 10px; display: flex; align-items: center; gap: 10px;">
            <div style="width: 30px; height: 30px; border-radius: 8px; background: rgba(99, 102, 241, 0.15); color: var(--accent-primary); display: flex; align-items: center; justify-content: center; flex-shrink: 0;">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
            </div>
            <div style="flex: 1;">
              <div style="font-size: 0.8rem; font-weight: 600; color: var(--text-main);">Telegram Dispatch Ready</div>
              <div style="font-size: 0.72rem; color: var(--text-muted);">Alerts automatically send to your Telegram Chat ID upon price drop</div>
            </div>
          </div>

        </div>

        <!-- Fixed Submit Controls -->
        <div style="flex-shrink: 0; display: flex; justify-content: flex-end; gap: 10px; border-top: 1px solid var(--card-border); padding-top: 14px;">
          <button (click)="close.emit()" class="btn-secondary" style="padding: 6px 16px; font-size: 0.82rem;">Cancel</button>
          <button (click)="submitTracker()" [disabled]="submitting || !targetUrl.trim()" class="btn-primary" style="padding: 8px 20px; font-size: 0.85rem;">
            @if (submitting) {
              <div class="spinner"></div><span>Creating...</span>
            } @else {
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
              <span>Start Tracking</span>
            }
          </button>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .field-label {
      font-size: 0.82rem;
      color: var(--text-muted);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(-8px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class AddTrackerWizardComponent {
  @Output() close = new EventEmitter<void>();

  private readonly trackerStore = inject(TrackerStore);
  private readonly toast = inject(ToastService);

  readonly pricePresets = PRICE_PRESETS;
  readonly pollIntervalOptions = POLL_INTERVAL_OPTIONS;
  readonly cooldownOptions = COOLDOWN_OPTIONS;

  selectedRuleType: RuleType = 'TARGET_PRICE';
  percentDrop = 15;
  targetUrl = '';
  targetPrice = DEFAULT_TARGET_PRICE;
  pollIntervalSeconds = DEFAULT_POLL_INTERVAL_SECONDS;
  cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
  submitting = false;
  showAdvanced = false;

  submitTracker(): void {
    const url = this.targetUrl.trim();
    if (!url) return;

    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      this.toast.error('Invalid URL', 'Please enter a valid HTTP or HTTPS product link');
      return;
    }

    let rules: TrackerRule[] = [];
    if (this.selectedRuleType === 'TARGET_PRICE') {
      rules = [{ type: 'TARGET_PRICE', targetPrice: Number(this.targetPrice) }];
    } else if (this.selectedRuleType === 'PERCENT_DROP') {
      rules = [{ type: 'PERCENT_DROP', percentDrop: Number(this.percentDrop) }];
    } else {
      rules = [{ type: 'BACK_IN_STOCK' }];
    }

    this.submitting = true;

    this.trackerStore.addTracker(
      {
        targetUrl: url,
        rules,
        channelIds: ['chan-telegram'],
        pollIntervalSeconds: Number(this.pollIntervalSeconds),
        cooldownSeconds: Number(this.cooldownSeconds),
      },
      () => {
        this.submitting = false;
        this.toast.success('Tracker Created!', 'Background scraper is fetching the latest details.');
        this.close.emit();
      },
      () => {
        this.submitting = false;
      }
    );
  }
}
