import { Injectable, signal, inject } from '@angular/core';
import { ApiService } from './api.service';
import { type FeatureFlag } from '../models/tracker.models';
import { FLAG_KEYS } from '../constants';

export type { FeatureFlag };

/** Default flag definitions loaded before API responds. */
const DEFAULT_FLAGS: FeatureFlag[] = [
  {
    id: 'flag-charts',
    key: FLAG_KEYS.PRICE_CHARTS,
    description: 'Enable interactive ECharts price history graphs',
    enabled: false,
    rolloutPercentage: 0,
    targetUsers: [],
  },
  {
    id: 'flag-telegram',
    key: FLAG_KEYS.ALERTS_TELEGRAM,
    description: 'Enable Telegram notification dispatch channel',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
  {
    id: 'flag-confetti',
    key: FLAG_KEYS.CONFETTI_ALERT,
    description: 'Burst confetti animation when a live price drop alert is received',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
  {
    id: 'flag-sound',
    key: FLAG_KEYS.ALERT_SOUND,
    description: 'Play a musical ding sound when a price drop alert fires',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
  {
    id: 'flag-compact',
    key: FLAG_KEYS.COMPACT_MODE,
    description: 'Enable compact card layout with denser grid (4 columns instead of 3)',
    enabled: false,
    rolloutPercentage: 0,
    targetUsers: [],
  },
  {
    id: 'flag-quick-actions',
    key: FLAG_KEYS.QUICK_ACTIONS_BAR,
    description: 'Show floating quick-actions bar for bulk Pause All / Resume All',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
  {
    id: 'flag-price-trend',
    key: FLAG_KEYS.PRICE_TREND,
    description: 'Show price trend arrow on product cards (discount vs MRP)',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
  {
    id: 'flag-analytics',
    key: FLAG_KEYS.PRICE_ANALYTICS,
    description: 'Enable AI Buy Confidence Score and All-Time Low price analytics badges',
    enabled: true,
    rolloutPercentage: 100,
    targetUsers: [],
  },
];

@Injectable({ providedIn: 'root' })
export class FlagStore {
  private readonly api = inject(ApiService);

  readonly flags = signal<FeatureFlag[]>([...DEFAULT_FLAGS]);

  loadFlags(): void {
    this.api.getFlags().subscribe({
      next: (data) => {
        const apiFlags = data as FeatureFlag[];
        if (apiFlags && apiFlags.length > 0) {
          // Merge API flags with local defaults (API wins, but keep any local-only flags)
          const apiKeys = new Set(apiFlags.map(f => f.key));
          const localOnly = DEFAULT_FLAGS.filter(f => !apiKeys.has(f.key));
          this.flags.set([...apiFlags, ...localOnly]);
        }
      },
      error: () => { /* keep default flags */ }
    });
  }

  saveFlag(flag: FeatureFlag, onSuccess?: () => void): void {
    this.api.saveFlag(flag).subscribe({
      next: (saved) => {
        const savedFlag = saved as FeatureFlag;
        this.flags.update(items => {
          const idx = items.findIndex(f => f.key === savedFlag.key);
          if (idx !== -1) {
            const updated = [...items];
            updated[idx] = savedFlag;
            return updated;
          }
          return [savedFlag, ...items];
        });
        onSuccess?.();
      },
      error: () => { /* silently fail — flag not critical */ }
    });
  }

  /** Toggle a flag locally and persist to backend. */
  toggleFlag(flag: FeatureFlag, onSuccess?: () => void): void {
    const updated = { ...flag, enabled: !flag.enabled };
    // Optimistic local update
    this.flags.update(items => items.map(f => f.key === flag.key ? updated : f));
    this.saveFlag(updated, onSuccess);
  }

  isEnabled(key: string): boolean {
    const flag = this.flags().find(f => f.key === key);
    return flag ? flag.enabled : false;
  }

  createFlag(key: string, description: string, enabled = true): void {
    const newFlag: FeatureFlag = {
      id: '',
      key: key.trim(),
      description: description.trim(),
      enabled,
      rolloutPercentage: enabled ? 100 : 0,
      targetUsers: [],
    };
    this.saveFlag(newFlag);
  }
}
