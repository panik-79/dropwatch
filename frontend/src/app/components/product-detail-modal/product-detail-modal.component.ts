import { Component, Input, Output, EventEmitter, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TrackerStore } from '../../services/tracker.store';
import { FlagStore } from '../../services/flag.store';
import { PriceHistoryChartComponent } from '../price-history-chart/price-history-chart.component';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import {
  getPrimaryVariantSellingPrice,
  getPrimaryVariantMrp,
  calcDiscountPercent,
  getVariantLabel,
  type TrackerItem,
  type PriceSnapshot,
  type PriceAnalyticsResult,
  type RuleType,
  type TrackerRule,
  type ProductVariant,
} from '../../models/tracker.models';
import { calculatePriceAnalytics } from '../../utils/price-analytics.utility';
import {
  PLACEHOLDER_IMAGE_URL,
  DEFAULT_TARGET_PRICE,
  DEFAULT_POLL_INTERVAL_SECONDS,
  DEFAULT_COOLDOWN_SECONDS,
  PRICE_PRESETS,
  POLL_INTERVAL_OPTIONS,
  COOLDOWN_OPTIONS,
  FLAG_KEYS,
} from '../../constants';

@Component({
  selector: 'app-product-detail-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, PriceHistoryChartComponent],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="glass-card modal-content" (click)="$event.stopPropagation()" style="max-width: 760px; max-height: 88vh; display: flex; flex-direction: column; padding: 22px; overflow: hidden;">

        <!-- Fixed Header -->
        <div style="flex-shrink: 0; display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1px solid var(--card-border); padding-bottom: 14px;">
          <div style="display: flex; gap: 14px; min-width: 0; flex: 1;">
            <img
              [src]="tracker.product?.imageUrl || placeholderImage"
              (error)="onImageError($event)"
              style="width: 72px; height: 72px; object-fit: cover; border-radius: 12px; border: 1px solid var(--card-border); background: var(--section-card-bg); flex-shrink: 0;"
            />
            <div style="flex: 1; min-width: 0;">
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
                <span [class]="tracker.product?.site === 'MYNTRA' ? 'badge badge-myntra' : 'badge badge-flipkart'">
                  {{ tracker.product?.site || 'MERCHANT' }}
                </span>
                <span [class]="tracker.active ? 'badge badge-success' : 'badge'">
                  {{ tracker.active ? 'ACTIVE' : 'PAUSED' }}
                </span>
              </div>

              <h2 style="font-size: 1.05rem; font-weight: 700; color: var(--text-main); line-height: 1.25; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px;" [title]="tracker.product?.title">
                {{ tracker.product?.title || 'Tracked Product Details' }}
              </h2>

              <p style="font-size: 0.78rem; color: var(--text-muted); margin-bottom: 6px;">{{ tracker.product?.brand || 'E-Commerce Merchant' }}</p>

              <!-- Price Row -->
              <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                <div style="display: flex; align-items: baseline; gap: 8px;">
                  <span style="font-size: 1.25rem; font-weight: 800; color: var(--text-main); font-family: var(--font-mono);">
                    ₹{{ currentPrice > 0 ? currentPrice.toLocaleString('en-IN') : '—' }}
                  </span>
                  @if (mrpPrice > currentPrice) {
                    <span style="font-size: 0.8rem; color: var(--text-dim); text-decoration: line-through; font-family: var(--font-mono);">
                      ₹{{ mrpPrice.toLocaleString('en-IN') }}
                    </span>
                  }
                  @if (discountPercent > 0) {
                    <span style="font-size: 0.68rem; background: rgba(16,185,129,0.12); color: #10B981; border: 1px solid rgba(16,185,129,0.25); padding: 2px 6px; border-radius: 5px; font-weight: 700;">
                      {{ discountPercent }}% OFF
                    </span>
                  }
                </div>
                <a [href]="productLink" target="_blank" rel="noopener" class="btn-secondary" style="padding: 4px 10px; font-size: 0.75rem; border-color: rgba(99, 102, 241, 0.4); color: var(--accent-primary);">
                  <span>Open Link</span>
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                </a>
              </div>
            </div>
          </div>
          <button (click)="close.emit()" class="close-btn" style="margin-left: 12px;">&times;</button>
        </div>

        <!-- Scrollable Body Container -->
        <div class="modal-body">

          <!-- Dynamic Variant Selector (Horizontal Scrollable Pill Bar) -->
          @if (tracker.product?.variants?.length && tracker.product.variants.length > 1) {
            <div class="section-card" style="padding: 10px 14px;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                <div style="display: flex; align-items: center; gap: 6px;">
                  <span style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px;">Product Option / Size</span>
                  <span class="badge" style="font-size: 0.65rem; padding: 1px 6px;">{{ tracker.product.variants.length }} available</span>
                </div>
                <span style="font-size: 0.68rem; color: var(--accent-primary); font-weight: 600;">Scroll horizontally & click to switch</span>
              </div>
              
              <div class="variant-scroll-container">
                @for (v of tracker.product.variants; track v.siteSkuId) {
                  <button
                    type="button"
                    (click)="selectVariant(v)"
                    class="filter-pill variant-pill"
                    [class.active-filter]="tracker.variantId === v.siteSkuId"
                  >
                    <span>{{ getVariantLabel(v) }}</span>
                    @if (tracker.variantId === v.siteSkuId) {
                      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                    }
                  </button>
                }
              </div>
            </div>
          }

          <!-- Price Intelligence Widget -->
          @if (flagStore.isEnabled(FLAG_KEYS.PRICE_ANALYTICS)) {
            @let analytics = priceAnalytics;
            <div class="section-card" style="padding: 12px 16px; border-left: 4px solid;" [style.borderLeftColor]="analytics.tier === 'EXCELLENT' ? '#10B981' : analytics.tier === 'GOOD' ? '#3B82F6' : analytics.tier === 'FAIR' ? '#F59E0B' : '#EF4444'">
              <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; margin-bottom: 8px;">
                <div>
                  <div style="font-size: 0.68rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 2px;">
                    AI Buy Confidence Score
                  </div>
                  <div style="font-size: 0.88rem; font-weight: 700; color: var(--text-main); display: flex; align-items: center; gap: 6px;">
                    <span>{{ analytics.headline }}</span>
                  </div>
                </div>

                <!-- Score Badge -->
                <div style="display: flex; align-items: center; gap: 10px;">
                  <div style="text-align: right;">
                    <div style="font-size: 1.3rem; font-weight: 800; font-family: var(--font-mono); line-height: 1;" [style.color]="analytics.tier === 'EXCELLENT' ? '#10B981' : analytics.tier === 'GOOD' ? '#3B82F6' : analytics.tier === 'FAIR' ? '#F59E0B' : '#EF4444'">
                      {{ analytics.score }}<span style="font-size: 0.85rem;">%</span>
                    </div>
                    <div style="font-size: 0.65rem; font-weight: 700; color: var(--text-dim); text-transform: uppercase;">Confidence</div>
                  </div>
                </div>
              </div>

              <!-- Price Metrics Row -->
              <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; background: var(--section-card-bg); padding: 8px 12px; border-radius: 8px; border: 1px solid var(--card-border);">
                <div>
                  <div style="font-size: 0.65rem; color: var(--text-dim); font-weight: 600;">All-Time Low</div>
                  <div style="font-size: 0.85rem; font-weight: 700; color: #10B981; font-family: var(--font-mono);">
                    ₹{{ (analytics.allTimeLow > 0 ? analytics.allTimeLow : currentPrice).toLocaleString('en-IN') }}
                  </div>
                </div>
                <div>
                  <div style="font-size: 0.65rem; color: var(--text-dim); font-weight: 600;">30-Day Avg</div>
                  <div style="font-size: 0.85rem; font-weight: 700; color: var(--text-main); font-family: var(--font-mono);">
                    ₹{{ (analytics.avg30DayPrice > 0 ? analytics.avg30DayPrice : currentPrice).toLocaleString('en-IN') }}
                  </div>
                </div>
                <div>
                  <div style="font-size: 0.65rem; color: var(--text-dim); font-weight: 600;">Highest</div>
                  <div style="font-size: 0.85rem; font-weight: 700; color: var(--text-muted); font-family: var(--font-mono);">
                    ₹{{ (analytics.allTimeHigh > 0 ? analytics.allTimeHigh : mrpPrice).toLocaleString('en-IN') }}
                  </div>
                </div>
              </div>
            </div>
          }

          <!-- Navigation Tabs -->
          <div class="tab-bar">
            <button (click)="activeTab = 'triggers'" [class.active-tab]="activeTab === 'triggers'" class="tab-btn">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              Alert Triggers
            </button>

            @if (flagStore.isEnabled('ui.price-charts')) {
              <button (click)="activeTab = 'history'; loadChartIfNeeded()" [class.active-tab]="activeTab === 'history'" class="tab-btn">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M3 3v18h18"/><path d="m19 9-5 5-4-4-3 3"/></svg>
                Price History
              </button>
            }

            <button (click)="activeTab = 'specs'" [class.active-tab]="activeTab === 'specs'" class="tab-btn">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
              Product Info
            </button>
          </div>

          <!-- Tab 1: Alert Triggers & Rules -->
          @if (activeTab === 'triggers') {
            <div style="display: flex; flex-direction: column; gap: 12px;">

              <!-- Rule Type Selector -->
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

              <!-- Target Price -->
              @if (selectedRuleType === 'TARGET_PRICE') {
                <div class="section-card" style="padding: 14px 16px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <label style="font-size: 0.78rem; font-weight: 700; color: var(--text-main); text-transform: uppercase; letter-spacing: 0.5px;">Price Drop Alert Limit (₹)</label>
                    <span style="font-size: 0.72rem; color: var(--accent-primary); font-weight: 600;">Triggers when price ≤ target</span>
                  </div>

                  <div style="display: flex; gap: 10px; align-items: center;">
                    <input
                      type="number"
                      [(ngModel)]="targetPrice"
                      placeholder="e.g. 4999"
                      class="glass-input"
                      min="1"
                      style="flex: 1; font-family: var(--font-mono); font-size: 1.05rem; font-weight: 700; padding: 8px 14px;"
                    />
                    <button (click)="saveRules()" [disabled]="saving()" class="btn-primary" style="padding: 8px 18px; font-size: 0.85rem;">
                      @if (saving()) {
                        <div class="spinner"></div><span>Saving...</span>
                      } @else {
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        <span>Save Trigger</span>
                      }
                    </button>
                  </div>

                  <!-- Quick Presets -->
                  <div style="display: flex; gap: 6px; margin-top: 10px; align-items: center; flex-wrap: wrap;">
                    <span style="font-size: 0.72rem; color: var(--text-dim);">Quick Presets:</span>
                    @for (preset of pricePresets; track preset) {
                      <button type="button" (click)="targetPrice = preset" class="btn-secondary" style="padding: 3px 8px; font-size: 0.72rem;">
                        ₹{{ preset.toLocaleString('en-IN') }}
                      </button>
                    }
                  </div>
                </div>
              }

              <!-- Percent Drop -->
              @if (selectedRuleType === 'PERCENT_DROP') {
                <div class="section-card" style="padding: 14px 16px;">
                  <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                    <label style="font-size: 0.78rem; font-weight: 700; color: var(--text-main); text-transform: uppercase; letter-spacing: 0.5px;">Price Drop Threshold (%)</label>
                    <span style="font-size: 0.72rem; color: #10B981; font-weight: 600;">Triggers when price drops ≥ threshold</span>
                  </div>

                  <div style="display: flex; gap: 10px; align-items: center;">
                    <input
                      type="number"
                      [(ngModel)]="percentDrop"
                      placeholder="e.g. 15"
                      class="glass-input"
                      min="1"
                      max="99"
                      style="flex: 1; font-family: var(--font-mono); font-size: 1.05rem; font-weight: 700; padding: 8px 14px;"
                    />
                    <button (click)="saveRules()" [disabled]="saving()" class="btn-primary" style="padding: 8px 18px; font-size: 0.85rem;">
                      @if (saving()) {
                        <div class="spinner"></div><span>Saving...</span>
                      } @else {
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        <span>Save Trigger</span>
                      }
                    </button>
                  </div>

                  <!-- Quick Presets -->
                  <div style="display: flex; gap: 6px; margin-top: 10px; align-items: center; flex-wrap: wrap;">
                    <span style="font-size: 0.72rem; color: var(--text-dim);">Quick Presets:</span>
                    @for (pct of [10, 15, 20, 25, 30, 40, 50]; track pct) {
                      <button type="button" (click)="percentDrop = pct" class="btn-secondary" style="padding: 3px 8px; font-size: 0.72rem;">
                        {{ pct }}% Drop
                      </button>
                    }
                  </div>
                </div>
              }

              <!-- Back in Stock -->
              @if (selectedRuleType === 'BACK_IN_STOCK') {
                <div class="section-card" style="padding: 14px 16px;">
                  <div style="font-size: 0.84rem; font-weight: 700; color: #10B981; margin-bottom: 2px;">📦 Back In Stock Trigger</div>
                  <p style="font-size: 0.75rem; color: var(--text-muted); margin-bottom: 10px;">Receive an alert when this item becomes available for order again.</p>
                  <button (click)="saveRules()" [disabled]="saving()" class="btn-primary" style="padding: 8px 18px; font-size: 0.85rem;">
                    @if (saving()) {
                      <div class="spinner"></div><span>Saving...</span>
                    } @else {
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                      <span>Save Back-in-Stock Trigger</span>
                    }
                  </button>
                </div>
              }

              <!-- Poll Interval & Cooldown -->
              <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                <div class="section-card" style="padding: 12px 14px;">
                  <label style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.4px;">Scraper Poll Interval</label>
                  <select [(ngModel)]="pollIntervalSeconds" class="glass-input" style="width: 100%; font-size: 0.82rem; padding: 6px 10px;">
                    @for (opt of pollIntervalOptions; track opt.value) {
                      <option [value]="opt.value">{{ opt.label }}</option>
                    }
                  </select>
                </div>

                <div class="section-card" style="padding: 12px 14px;">
                  <label style="font-size: 0.72rem; font-weight: 700; color: var(--text-muted); display: block; margin-bottom: 6px; text-transform: uppercase; letter-spacing: 0.4px;">Alert Repeat Cooldown</label>
                  <select [(ngModel)]="cooldownSeconds" class="glass-input" style="width: 100%; font-size: 0.82rem; padding: 6px 10px;">
                    @for (opt of cooldownOptions; track opt.value) {
                      <option [value]="opt.value">{{ opt.label }}</option>
                    }
                  </select>
                </div>
              </div>
            </div>
          }

          <!-- Tab 2: Price History Chart -->
          @if (activeTab === 'history' && flagStore.isEnabled('ui.price-charts')) {
            <div style="padding: 4px 0;">
              <app-price-history-chart [snapshots]="chartSnapshots" [targetPrice]="targetPrice"></app-price-history-chart>
            </div>
          }

          <!-- Tab 3: Product Specs -->
          @if (activeTab === 'specs') {
            <div style="padding: 4px 0; display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
              @for (spec of productSpecs; track spec.label) {
                <div class="section-card" style="padding: 12px 14px;">
                  <span style="font-size: 0.68rem; color: var(--text-dim); text-transform: uppercase; font-weight: 600; letter-spacing: 0.4px;">{{ spec.label }}</span>
                  <div style="font-size: 0.82rem; font-weight: 700; color: var(--text-main); margin-top: 2px; font-family: {{ spec.mono ? 'var(--font-mono)' : 'inherit' }};">{{ spec.value }}</div>
                </div>
              }
            </div>
          }

        </div>

        <!-- Fixed Footer Actions -->
        <div style="flex-shrink: 0; display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--card-border); padding-top: 14px;">
          <div style="display: flex; gap: 8px;">
            @if (tracker.active) {
              <button (click)="pauseTracker()" class="btn-secondary" style="font-size: 0.78rem; padding: 6px 14px;">Pause</button>
            } @else {
              <button (click)="resumeTracker()" class="btn-secondary" style="font-size: 0.78rem; padding: 6px 14px;">Resume</button>
            }
            <button (click)="deleteTracker()" class="btn-secondary" style="font-size: 0.78rem; padding: 6px 14px; color: #F43F5E;">Remove</button>
          </div>
          <button (click)="close.emit()" class="btn-secondary" style="padding: 6px 18px; font-size: 0.82rem;">Close</button>
        </div>

      </div>
    </div>
  `
})
export class ProductDetailModalComponent implements OnInit {
  @Input({ required: true }) tracker!: TrackerItem;
  @Output() close = new EventEmitter<void>();

  private readonly trackerStore = inject(TrackerStore);
  readonly flagStore = inject(FlagStore);
  private readonly toast = inject(ToastService);
  private readonly api = inject(ApiService);

  readonly placeholderImage = PLACEHOLDER_IMAGE_URL;
  readonly pricePresets = PRICE_PRESETS;
  readonly pollIntervalOptions = POLL_INTERVAL_OPTIONS;
  readonly cooldownOptions = COOLDOWN_OPTIONS;

  selectedRuleType: RuleType = 'TARGET_PRICE';
  percentDrop = 15;
  activeTab: 'triggers' | 'history' | 'specs' = 'triggers';
  targetPrice = DEFAULT_TARGET_PRICE;
  pollIntervalSeconds: number = DEFAULT_POLL_INTERVAL_SECONDS;
  cooldownSeconds: number = DEFAULT_COOLDOWN_SECONDS;
  saving = signal<boolean>(false);
  chartSnapshots: unknown[] = [];
  private chartLoaded = false;

  readonly FLAG_KEYS = FLAG_KEYS;

  ngOnInit(): void {
    this.flagStore.loadFlags();

    if (this.tracker.rules?.length) {
      const first = this.tracker.rules[0];
      this.selectedRuleType = first.type || 'TARGET_PRICE';
      if (first.targetPrice) this.targetPrice = first.targetPrice;
      if (first.percentDrop) this.percentDrop = first.percentDrop;
    }
    this.pollIntervalSeconds = this.tracker.pollIntervalSeconds ?? DEFAULT_POLL_INTERVAL_SECONDS;
    this.cooldownSeconds = this.tracker.cooldownSeconds ?? DEFAULT_COOLDOWN_SECONDS;

    if (this.flagStore.isEnabled(FLAG_KEYS.PRICE_ANALYTICS)) {
      this.loadChartIfNeeded();
    }
  }

  getVariantLabel(v: ProductVariant | null | undefined): string {
    return getVariantLabel(v);
  }

  selectVariant(v: ProductVariant): void {
    if (!v || !v.siteSkuId) return;
    this.tracker.variantId = v.siteSkuId;
    this.trackerStore.updateTracker(
      this.tracker.id,
      { variantId: v.siteSkuId },
      () => {
        this.toast.info('Variant Updated', `Switched monitoring to ${this.getVariantLabel(v)}`);
      },
      (err: any) => {
        this.toast.error('Update Failed', 'Failed to update variant selection on server');
      }
    );
  }

  get currentPrice(): number {
    return getPrimaryVariantSellingPrice(this.tracker?.product, this.tracker?.variantId) || 0;
  }

  get mrpPrice(): number {
    return getPrimaryVariantMrp(this.tracker?.product, this.tracker?.variantId) || this.currentPrice;
  }

  get discountPercent(): number {
    return calcDiscountPercent(this.mrpPrice, this.currentPrice);
  }

  get priceAnalytics(): PriceAnalyticsResult {
    return calculatePriceAnalytics(this.chartSnapshots as PriceSnapshot[], this.currentPrice, this.mrpPrice);
  }

  get productLink(): string {
    return this.tracker.product?.canonicalUrl || 'https://www.myntra.com';
  }

  get productSpecs() {
    return [
      { label: 'Product ID', value: this.tracker.productId, mono: true },
      { label: 'Variant SKU', value: this.tracker.variantId || 'DEFAULT_SKU', mono: true },
      { label: 'Merchant', value: this.tracker.product?.site || 'Unknown', mono: false },
      { label: 'Category', value: this.tracker.product?.category || 'E-Commerce', mono: false },
      { label: 'Tracking Since', value: this.tracker.createdAt ? new Date(this.tracker.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—', mono: false },
      { label: 'Poll Interval', value: this.formatSeconds(this.tracker.pollIntervalSeconds), mono: false },
    ];
  }

  loadChartIfNeeded(): void {
    if (this.chartLoaded) return;
    this.chartLoaded = true;
    this.api.getPriceHistory(this.tracker.productId, this.tracker.variantId).subscribe({
      next: (res) => {
        const r = res as { snapshots?: unknown[] };
        this.chartSnapshots = r?.snapshots || [];
      },
      error: () => { this.chartSnapshots = []; }
    });
  }

  saveRules(): void {
    this.saving.set(true);
    let updatedRules: TrackerRule[] = [];
    if (this.selectedRuleType === 'TARGET_PRICE') {
      updatedRules = [{ type: 'TARGET_PRICE', targetPrice: Number(this.targetPrice) }];
    } else if (this.selectedRuleType === 'PERCENT_DROP') {
      updatedRules = [{ type: 'PERCENT_DROP', percentDrop: Number(this.percentDrop) }];
    } else {
      updatedRules = [{ type: 'BACK_IN_STOCK' }];
    }

    const payload = {
      rules: updatedRules,
      pollIntervalSeconds: Number(this.pollIntervalSeconds),
      cooldownSeconds: Number(this.cooldownSeconds),
    };

    this.trackerStore.updateTracker(
      this.tracker.id,
      payload,
      () => {
        this.saving.set(false);
        this.tracker.rules = updatedRules;
        this.tracker.pollIntervalSeconds = Number(this.pollIntervalSeconds);
        this.tracker.cooldownSeconds = Number(this.cooldownSeconds);
        this.toast.success('Trigger Saved!', 'Alert trigger rule updated successfully');
      },
      (err: any) => {
        this.saving.set(false);
        this.toast.error('Save Failed', err?.error?.message || 'Failed to update trigger rules');
      }
    );
  }

  pauseTracker(): void {
    this.trackerStore.pauseTracker(this.tracker.id);
    this.tracker.active = false;
    this.toast.info('Tracker Paused', `Monitoring paused for ${this.tracker.product?.title || 'product'}`);
  }

  resumeTracker(): void {
    this.trackerStore.resumeTracker(this.tracker.id);
    this.tracker.active = true;
    this.toast.success('Tracker Resumed', `Monitoring active for ${this.tracker.product?.title || 'product'}`);
  }

  deleteTracker(): void {
    this.trackerStore.deleteTracker(this.tracker.id);
    this.toast.warning('Tracker Removed', `Removed ${this.tracker.product?.title || 'product'}`);
    this.close.emit();
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== PLACEHOLDER_IMAGE_URL) {
      img.src = PLACEHOLDER_IMAGE_URL;
    }
  }

  private formatSeconds(seconds: number): string {
    if (!seconds) return '—';
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${seconds / 60}m`;
    return `${seconds / 3600}h`;
  }
}
