import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrackerStore } from '../../services/tracker.store';
import { TrackerSelectionService } from '../../services/tracker-selection.service';
import { FlagStore } from '../../services/flag.store';
import { ToastService } from '../../services/toast.service';
import { ProductDetailModalComponent } from '../product-detail-modal/product-detail-modal.component';
import { type TrackerItem } from '../../models/tracker.models';
import {
  PLACEHOLDER_IMAGE_URL,
  FALLBACK_PRICE,
  FLAG_KEYS,
} from '../../constants';
import {
  getPrimaryVariantSellingPrice,
  getPrimaryVariantMrp,
  calcDiscountPercent,
  getVariantLabel,
  type PriceAnalyticsResult,
} from '../../models/tracker.models';
import { calculatePriceAnalytics } from '../../utils/price-analytics.utility';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ProductDetailModalComponent],
  template: `
    <div style="max-width: 1320px; margin: 28px auto; padding: 0 24px;">

      <!-- ── Metrics Bar ─────────────────────────────────────────────── -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 32px;">

        <div class="glass-card metric-card" style="padding: 22px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
            <span class="metric-label">Active Trackers</span>
            <div class="metric-icon" style="background: rgba(99, 102, 241, 0.12); color: #6366F1;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 2v20M2 12h20"/></svg>
            </div>
          </div>
          <h2 class="metric-value" style="color: var(--text-main);">{{ trackerStore.activeTrackersCount() }}</h2>
          <span class="metric-sub">Out of {{ trackerStore.totalTrackersCount() }} total</span>
        </div>

        <div class="glass-card metric-card" style="padding: 22px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
            <span class="metric-label">Price Drop Alerts</span>
            <div class="metric-icon" style="background: rgba(244, 63, 94, 0.12); color: #F43F5E;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            </div>
          </div>
          <h2 class="metric-value" style="color: #F43F5E;">{{ trackerStore.totalAlertsCount() }}</h2>
          <span class="metric-sub">Dispatched via Telegram</span>
        </div>

        <div class="glass-card metric-card" style="padding: 22px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
            <span class="metric-label">Estimated Savings</span>
            <div class="metric-icon" style="background: rgba(16, 185, 129, 0.12); color: #10B981;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            </div>
          </div>
          <h2 class="metric-value" style="color: #10B981;">₹{{ trackerStore.totalEstimatedSavings().toLocaleString('en-IN') }}</h2>
          <span class="metric-sub">Saved vs MRP baselines</span>
        </div>

        <div class="glass-card metric-card" style="padding: 22px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
            <span class="metric-label">Scraper Health</span>
            <div class="metric-icon" style="background: rgba(99, 102, 241, 0.12); color: #6366F1;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
            </div>
          </div>
          <h2 class="metric-value" style="color: #6366F1;">99.8%</h2>
          <span class="metric-sub">Virtual Threads · RabbitMQ</span>
        </div>

      </div>

      <!-- ── Main Grid ──────────────────────────────────────────────── -->
      <div style="display: grid; grid-template-columns: 1fr 340px; gap: 28px; align-items: start;">

        <!-- Left: Trackers Catalog -->
        <div>
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h3 style="font-size: 1.2rem; font-weight: 700; display: flex; align-items: center; gap: 10px; color: var(--text-main);">
              <span>Tracked Products</span>
              <span style="font-size: 0.78rem; background: var(--btn-secondary-bg); padding: 2px 9px; border-radius: 12px; color: var(--text-muted); font-family: var(--font-mono);">
                {{ trackerStore.filteredTrackers().length }}
              </span>
            </h3>

            <div style="display: flex; align-items: center; gap: 8px;">
              <!-- Compact Mode Toggle -->
              @if (flagStore.isEnabled(flags.COMPACT_MODE)) {
                <button (click)="compactMode = !compactMode" class="btn-ghost" style="font-size: 0.78rem;" [title]="compactMode ? 'Switch to normal view' : 'Switch to compact view'">
                  @if (compactMode) {
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
                  } @else {
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="4" rx="1"/><rect x="3" y="10" width="18" height="4" rx="1"/><rect x="3" y="17" width="18" height="4" rx="1"/></svg>
                  }
                </button>
              }

              <!-- Site Filter Tabs -->
              <div class="filter-bar">
                <button (click)="trackerStore.filterSite.set('ALL')" [class.active-filter]="trackerStore.filterSite() === 'ALL'" class="filter-pill">All</button>
                <button (click)="trackerStore.filterSite.set('MYNTRA')" [class.active-filter]="trackerStore.filterSite() === 'MYNTRA'" class="filter-pill">Myntra</button>
                <button (click)="trackerStore.filterSite.set('FLIPKART')" [class.active-filter]="trackerStore.filterSite() === 'FLIPKART'" class="filter-pill">Flipkart</button>
                <button (click)="trackerStore.filterSite.set('PAUSED')" [class.active-filter]="trackerStore.filterSite() === 'PAUSED'" class="filter-pill">Paused</button>
              </div>
            </div>
          </div>

          <!-- Loading Skeleton -->
          @if (trackerStore.loading() && trackerStore.trackers().length === 0) {
            <div [style.gridTemplateColumns]="cardGridCols" style="display: grid; gap: 20px;">
              @for (i of [1, 2, 3]; track i) {
                <div class="glass-card" style="padding: 20px; height: 190px;">
                  <div class="skeleton" style="height: 16px; width: 40%; margin-bottom: 14px;"></div>
                  <div style="display: flex; gap: 14px;">
                    <div class="skeleton" style="width: 76px; height: 76px; border-radius: 10px;"></div>
                    <div style="flex: 1;">
                      <div class="skeleton" style="height: 16px; width: 80%; margin-bottom: 8px;"></div>
                      <div class="skeleton" style="height: 13px; width: 50%; margin-bottom: 12px;"></div>
                      <div class="skeleton" style="height: 22px; width: 55%;"></div>
                    </div>
                  </div>
                </div>
              }
            </div>
          } @else if (trackerStore.filteredTrackers().length === 0) {
            <!-- Empty State -->
            <div class="glass-card" style="padding: 56px; text-align: center;">
              <div style="width: 60px; height: 60px; border-radius: 18px; background: rgba(99, 102, 241, 0.1); color: var(--accent-primary); display: flex; align-items: center; justify-content: center; margin: 0 auto 18px;">
                <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
              </div>
              <h4 style="font-size: 1.1rem; font-weight: 700; color: var(--text-main); margin-bottom: 6px;">No Active Price Trackers</h4>
              <p style="font-size: 0.88rem; color: var(--text-muted); max-width: 380px; margin: 0 auto 20px;">Paste a product URL from Myntra or Flipkart to start monitoring price drops automatically.</p>
            </div>
          } @else {
            <!-- Product Cards Grid -->
            <div [style.gridTemplateColumns]="cardGridCols" style="display: grid; gap: 20px;">
              @for (t of trackerStore.filteredTrackers(); track t.id) {
                <div class="glass-card product-card" (click)="openDetail(t)" style="cursor: pointer; padding: 18px; display: flex; flex-direction: column; justify-content: space-between;">

                  <div>
                    <!-- Site Badge & Status -->
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 6px; flex-wrap: wrap;">
                      <div style="display: flex; align-items: center; gap: 6px;">
                        <span [class]="t.product?.site === 'MYNTRA' ? 'badge badge-myntra' : 'badge badge-flipkart'">{{ t.product?.site || 'MERCHANT' }}</span>
                        @if (getVariantBadge(t)) {
                          <span class="badge" style="background: rgba(99, 102, 241, 0.14); color: var(--accent-primary); border: 1px solid rgba(99, 102, 241, 0.25);">
                            {{ getVariantBadge(t) }}
                          </span>
                        }
                      </div>

                      <div style="display: flex; align-items: center; gap: 6px;">
                        <!-- Buy Confidence Score Pill (feature-flagged) -->
                        @if (flagStore.isEnabled(flags.PRICE_ANALYTICS)) {
                          @let analytics = getAnalytics(t);
                          <span
                            [title]="analytics.headline"
                            style="font-size: 0.68rem; font-weight: 800; padding: 2px 7px; border-radius: 6px; display: flex; align-items: center; gap: 3px;"
                            [style.background]="analytics.tier === 'EXCELLENT' ? 'rgba(16, 185, 129, 0.16)' : analytics.tier === 'GOOD' ? 'rgba(59, 130, 246, 0.16)' : analytics.tier === 'FAIR' ? 'rgba(245, 158, 11, 0.16)' : 'rgba(239, 68, 68, 0.16)'"
                            [style.color]="analytics.tier === 'EXCELLENT' ? '#10B981' : analytics.tier === 'GOOD' ? '#3B82F6' : analytics.tier === 'FAIR' ? '#F59E0B' : '#EF4444'"
                            [style.border]="'1px solid ' + (analytics.tier === 'EXCELLENT' ? 'rgba(16, 185, 129, 0.3)' : analytics.tier === 'GOOD' ? 'rgba(59, 130, 246, 0.3)' : analytics.tier === 'FAIR' ? 'rgba(245, 158, 11, 0.3)' : 'rgba(239, 68, 68, 0.3)')"
                          >
                            @if (analytics.isAllTimeLow) {
                              <span>🔥 ATL</span>
                            } @else {
                              <span>{{ analytics.score }}% SCORE</span>
                            }
                          </span>
                        }

                        <span [class]="t.active ? 'badge badge-success' : 'badge'" [style.opacity]="t.active ? '1' : '0.7'">
                          {{ t.active ? 'ACTIVE' : 'PAUSED' }}
                        </span>
                      </div>
                    </div>

                    <!-- Image & Info -->
                    <div style="display: flex; gap: 12px; margin-bottom: 14px;">
                      <img
                        [src]="t.product?.imageUrl || placeholderImage"
                        (error)="onImageError($event)"
                        style="width: 72px; height: 72px; object-fit: cover; border-radius: 10px; border: 1px solid var(--card-border); background: var(--section-card-bg); flex-shrink: 0;"
                      />
                      <div style="flex: 1; min-width: 0;">
                        <h4 style="font-size: 0.92rem; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 3px; color: var(--text-main);" [title]="t.product?.title">
                          {{ t.product?.title || 'Initializing Product...' }}
                        </h4>
                        <p style="font-size: 0.76rem; color: var(--text-muted); margin-bottom: 10px;">{{ t.product?.brand || 'E-Commerce Item' }}</p>

                        <!-- Price Row -->
                        <div style="display: flex; align-items: baseline; gap: 7px; flex-wrap: wrap;">
                          <span style="font-size: 1.25rem; font-weight: 800; color: var(--text-main); font-family: var(--font-mono);">
                            ₹{{ getSellingPrice(t) > 0 ? getSellingPrice(t).toLocaleString('en-IN') : '—' }}
                          </span>
                          @if (getMrp(t) > getSellingPrice(t)) {
                            <span style="font-size: 0.8rem; color: var(--text-dim); text-decoration: line-through; font-family: var(--font-mono);">
                              ₹{{ getMrp(t).toLocaleString('en-IN') }}
                            </span>
                          }
                          @if (getDiscount(t) > 0) {
                            <span class="discount-pill">{{ getDiscount(t) }}% OFF</span>
                          }
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Footer Actions -->
                  <div style="border-top: 1px solid var(--card-border); padding-top: 12px; display: flex; justify-content: space-between; align-items: center;">
                    <button (click)="$event.stopPropagation(); openDetail(t)" class="btn-secondary" style="padding: 5px 11px; font-size: 0.76rem;">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                      <span>Manage</span>
                    </button>

                    <div style="display: flex; gap: 6px;">
                      @if (t.active) {
                        <button (click)="$event.stopPropagation(); pauseTracker(t)" class="btn-secondary" style="padding: 5px 10px; font-size: 0.76rem;">Pause</button>
                      } @else {
                        <button (click)="$event.stopPropagation(); resumeTracker(t)" class="btn-secondary" style="padding: 5px 10px; font-size: 0.76rem;">Resume</button>
                      }
                      <button (click)="$event.stopPropagation(); deleteTracker(t)" class="btn-secondary" style="padding: 5px 10px; font-size: 0.76rem; color: #F43F5E;" title="Delete Tracker">&times;</button>
                    </div>
                  </div>

                </div>
              }
            </div>
          }
        </div>

        <!-- Right: Real-Time SSE Alert Timeline -->
        <div>
          <div class="glass-card" style="padding: 20px; position: sticky; top: 90px;">
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; border-bottom: 1px solid var(--card-border); padding-bottom: 12px;">
              <h4 style="font-size: 1rem; font-weight: 700; display: flex; align-items: center; gap: 8px; color: var(--text-main);">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#F43F5E" stroke-width="2.2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                Live Price Drops
              </h4>
              <div style="display: flex; align-items: center; gap: 6px; font-size: 0.7rem; color: #10B981; font-weight: 700;">
                <div class="pulse-dot"></div>
                SSE LIVE
              </div>
            </div>

            <div style="max-height: 480px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px;">
              @for (alert of trackerStore.alerts(); track alert.id) {
                <div class="alert-card" style="animation: slideUp 0.3s ease;">
                  <div style="display: flex; justify-content: space-between; font-size: 0.72rem; color: #F43F5E; font-weight: 700; margin-bottom: 3px;">
                    <span>{{ alert.ruleType === 'TARGET_PRICE' ? 'TARGET PRICE HIT' : 'PRICE DROP' }}</span>
                    <span style="font-family: var(--font-mono); font-size: 0.88rem;">₹{{ alert.priceAtTrigger.toLocaleString('en-IN') }}</span>
                  </div>
                  <p style="font-size: 0.83rem; font-weight: 600; color: var(--text-main); margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                    {{ alert.product?.title || 'Tracked Product' }}
                  </p>
                  <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.69rem; color: var(--text-dim);">
                    <span>{{ alert.channel || 'Telegram' }}</span>
                    <span>{{ alert.createdAt | date:'shortTime' }}</span>
                  </div>
                </div>
              }
              @if (trackerStore.alerts().length === 0) {
                <div style="text-align: center; color: var(--text-dim); padding: 36px 12px; font-size: 0.84rem;">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin-bottom: 8px; opacity: 0.4;"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                  <div>Listening for live price drops...</div>
                </div>
              }
            </div>
          </div>
        </div>

      </div>

      <!-- ── Quick Actions Bar (feature-flagged) ──────────────────── -->
      @if (flagStore.isEnabled(flags.QUICK_ACTIONS_BAR) && trackerStore.trackers().length > 0) {
        <div class="quick-actions-bar">
          <span style="font-size: 0.78rem; font-weight: 600; color: var(--text-muted);">
            {{ trackerStore.activeTrackersCount() }}/{{ trackerStore.totalTrackersCount() }} active
          </span>
          <div style="display: flex; gap: 8px;">
            <button (click)="pauseAll()" class="btn-secondary" style="font-size: 0.78rem; padding: 6px 14px;">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>
              Pause All
            </button>
            <button (click)="resumeAll()" class="btn-secondary" style="font-size: 0.78rem; padding: 6px 14px; color: #10B981; border-color: rgba(16,185,129,0.3);">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
              Resume All
            </button>
          </div>
        </div>
      }

      <!-- Detail Modal -->
      @if (selectedTracker()) {
        <app-product-detail-modal
          [tracker]="selectedTracker()!"
          (close)="trackerSelectionService.clear()"
        ></app-product-detail-modal>
      }

    </div>
  `,
  styles: [`
    .metric-label {
      font-size: 0.8rem;
      color: var(--text-muted);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .metric-value {
      font-size: 2.2rem;
      font-weight: 800;
      font-family: var(--font-heading);
      line-height: 1.1;
      margin-bottom: 4px;
    }
    .metric-sub {
      font-size: 0.78rem;
      color: var(--text-dim);
      font-weight: 500;
    }
    .metric-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .metric-card {
      transition: transform 0.2s ease;
    }
    .product-card:hover {
      border-color: var(--card-border-hover);
    }
    .discount-pill {
      font-size: 0.66rem;
      background: rgba(16, 185, 129, 0.12);
      color: #10B981;
      border: 1px solid rgba(16, 185, 129, 0.25);
      padding: 2px 6px;
      border-radius: 6px;
      font-weight: 700;
    }
    .alert-card {
      background: var(--alert-card-bg);
      border: 1px solid var(--alert-card-border);
      padding: 11px 13px;
      border-radius: 10px;
      transition: border-color 0.2s ease;
    }
    .alert-card:hover {
      border-color: rgba(244, 63, 94, 0.4);
    }
    .quick-actions-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: fixed;
      bottom: 28px;
      left: 50%;
      transform: translateX(-50%);
      background: var(--card-bg);
      border: 1px solid var(--card-border-hover);
      border-radius: 20px;
      padding: 10px 20px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3), 0 0 20px var(--accent-glow);
      backdrop-filter: blur(16px);
      z-index: 50;
      min-width: 280px;
      animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
      transition: background-color 0.3s ease;
    }
    @keyframes slideUp {
      from { opacity: 0; transform: translateX(-50%) translateY(20px); }
      to { opacity: 1; transform: translateX(-50%) translateY(0); }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly trackerStore = inject(TrackerStore);
  readonly trackerSelectionService = inject(TrackerSelectionService);
  readonly flagStore = inject(FlagStore);
  private readonly toast = inject(ToastService);

  readonly flags = FLAG_KEYS;
  readonly placeholderImage = PLACEHOLDER_IMAGE_URL;

  // Expose the selection signal to template
  readonly selectedTracker = this.trackerSelectionService.selectedTracker;

  compactMode = false;

  get cardGridCols(): string {
    return this.compactMode
      ? 'repeat(auto-fill, minmax(260px, 1fr))'
      : 'repeat(auto-fill, minmax(320px, 1fr))';
  }

  ngOnInit(): void {
    this.trackerStore.loadTrackers();
    this.trackerStore.loadAlerts();
    this.flagStore.loadFlags();
  }

  ngOnDestroy(): void {
    // Clean up any selected tracker state when navigating away
    this.trackerSelectionService.clear();
  }

  getSellingPrice(t: TrackerItem): number {
    return getPrimaryVariantSellingPrice(t.product, t.variantId) || FALLBACK_PRICE;
  }

  getMrp(t: TrackerItem): number {
    return getPrimaryVariantMrp(t.product, t.variantId) || this.getSellingPrice(t);
  }

  getDiscount(t: TrackerItem): number {
    return calcDiscountPercent(this.getMrp(t), this.getSellingPrice(t));
  }

  getAnalytics(t: TrackerItem): PriceAnalyticsResult {
    const selling = this.getSellingPrice(t);
    const mrp = this.getMrp(t);
    return calculatePriceAnalytics([], selling, mrp);
  }

  getVariantBadge(t: TrackerItem): string {
    if (!t.product?.variants?.length) return '';
    const v = t.product.variants.find(varItem => varItem.siteSkuId === t.variantId) || t.product.variants[0];
    const lbl = getVariantLabel(v);
    return (lbl && lbl !== 'Standard Variant' && lbl !== 'Standard') ? lbl : '';
  }

  openDetail(t: TrackerItem): void {
    this.trackerSelectionService.select(t);
  }

  pauseTracker(t: TrackerItem): void {
    this.trackerStore.pauseTracker(t.id);
    this.toast.info('Tracker Paused', `Monitoring paused for ${t.product?.title || 'product'}`);
  }

  resumeTracker(t: TrackerItem): void {
    this.trackerStore.resumeTracker(t.id);
    this.toast.success('Tracker Resumed', `Monitoring active for ${t.product?.title || 'product'}`);
  }

  deleteTracker(t: TrackerItem): void {
    this.trackerStore.deleteTracker(t.id);
    this.toast.warning('Tracker Removed', `Removed ${t.product?.title || 'product'} from tracking list`);
  }

  pauseAll(): void {
    const active = this.trackerStore.trackers().filter(t => t.active);
    active.forEach(t => this.trackerStore.pauseTracker(t.id));
    this.toast.info('All Paused', `${active.length} trackers paused`);
  }

  resumeAll(): void {
    const paused = this.trackerStore.trackers().filter(t => !t.active);
    paused.forEach(t => this.trackerStore.resumeTracker(t.id));
    this.toast.success('All Resumed', `${paused.length} trackers resumed`);
  }

  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && img.src !== PLACEHOLDER_IMAGE_URL) {
      img.src = PLACEHOLDER_IMAGE_URL;
    }
  }
}
