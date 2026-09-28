import { Injectable, signal, computed, inject } from '@angular/core';
import { ApiService } from './api.service';
import {
  type TrackerItem,
  type AlertItem,
  type TrackerRule,
  getPrimaryVariantSellingPrice,
  getPrimaryVariantMrp,
} from '../models/tracker.models';

export type { TrackerItem, AlertItem };

@Injectable({ providedIn: 'root' })
export class TrackerStore {
  private readonly api = inject(ApiService);

  readonly trackers = signal<TrackerItem[]>([]);
  readonly alerts = signal<AlertItem[]>([]);
  readonly filterSite = signal<string>('ALL');
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  readonly filteredTrackers = computed(() => {
    const list = this.trackers();
    const site = this.filterSite();
    if (site === 'ALL') return list;
    if (site === 'PAUSED') return list.filter(t => !t.active);
    return list.filter(t => t.product?.site === site);
  });

  readonly totalTrackersCount = computed(() => this.trackers().length);
  readonly activeTrackersCount = computed(() => this.trackers().filter(t => t.active).length);
  readonly totalAlertsCount = computed(() => this.alerts().length);

  readonly totalEstimatedSavings = computed(() => {
    let savings = 0;
    for (const t of this.trackers()) {
      if (t.product?.variants?.length) {
        const mrp = getPrimaryVariantMrp(t.product, t.variantId);
        const price = getPrimaryVariantSellingPrice(t.product, t.variantId);
        if (mrp > price && price > 0) {
          savings += (mrp - price);
        }
      }
    }
    return Math.round(savings);
  });

  loadTrackers(): void {
    this.loading.set(true);
    this.api.getTrackers().subscribe({
      next: (data) => {
        this.trackers.set((data as TrackerItem[]) || []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load trackers from server');
        this.loading.set(false);
      }
    });
  }

  loadAlerts(): void {
    this.api.getAlerts().subscribe({
      next: (data) => this.alerts.set((data as AlertItem[]) || []),
      error: () => { /* silently fail — not critical */ }
    });
  }

  addTracker(data: unknown, onSuccess?: () => void, onError?: (err: unknown) => void): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.createTracker(data).subscribe({
      next: (newTracker) => {
        this.trackers.update(items => [newTracker as TrackerItem, ...items]);
        this.loading.set(false);
        onSuccess?.();
      },
      error: (err: { error?: { message?: string } }) => {
        const msg = err?.error?.message || 'Failed to create tracker';
        this.error.set(msg);
        this.loading.set(false);
        onError?.(err);
      }
    });
  }

  pauseTracker(id: string): void {
    // Optimistic update
    this.trackers.update(items => items.map(t => t.id === id ? { ...t, active: false } : t));
    this.api.pauseTracker(id).subscribe({
      error: () => {
        // Roll back optimistic update on failure
        this.trackers.update(items => items.map(t => t.id === id ? { ...t, active: true } : t));
      }
    });
  }

  resumeTracker(id: string): void {
    this.trackers.update(items => items.map(t => t.id === id ? { ...t, active: true } : t));
    this.api.resumeTracker(id).subscribe({
      error: () => {
        this.trackers.update(items => items.map(t => t.id === id ? { ...t, active: false } : t));
      }
    });
  }

  updateTracker(
    id: string,
    updatedFields: Partial<TrackerItem>,
    onSuccess?: () => void,
    onError?: (err: unknown) => void
  ): void {
    // Snapshot previous state for rollback
    const prev = this.trackers().find(t => t.id === id);
    this.trackers.update(items => items.map(t => t.id === id ? { ...t, ...updatedFields } : t));
    this.api.updateTracker(id, updatedFields).subscribe({
      next: () => onSuccess?.(),
      error: (err) => {
        // Rollback to previous state
        if (prev) {
          this.trackers.update(items => items.map(t => t.id === id ? prev : t));
        }
        onError?.(err);
      }
    });
  }

  deleteTracker(id: string): void {
    const prev = this.trackers().find(t => t.id === id);
    this.trackers.update(items => items.filter(t => t.id !== id));
    this.api.deleteTracker(id).subscribe({
      error: () => {
        // Rollback — re-insert at original position isn't possible cleanly, append
        if (prev) {
          this.trackers.update(items => [...items, prev]);
        }
      }
    });
  }

  pushLiveAlert(alert: AlertItem): void {
    this.alerts.update(items => [alert, ...items]);
  }
}
