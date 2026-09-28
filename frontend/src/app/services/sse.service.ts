import { Injectable, signal, inject, OnDestroy } from '@angular/core';
import { TrackerStore } from './tracker.store';
import { FlagStore } from './flag.store';
import { SoundService } from './sound.service';
import { ConfettiService } from './confetti.service';
import { FLAG_KEYS, SSE_STREAM_URL, SSE_RECONNECT_BASE_MS, SSE_RECONNECT_MAX_MS, SSE_RECONNECT_MULTIPLIER } from '../constants';
import type { AlertItem } from '../models/tracker.models';

@Injectable({ providedIn: 'root' })
export class SseService {
  private readonly trackerStore = inject(TrackerStore);
  private readonly flagStore = inject(FlagStore);
  private readonly soundService = inject(SoundService);
  private readonly confettiService = inject(ConfettiService);

  readonly isConnected = signal<boolean>(false);
  readonly reconnectAttempts = signal<number>(0);

  private eventSource: EventSource | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private currentBackoffMs = SSE_RECONNECT_BASE_MS;
  private destroyed = false;

  connect(): void {
    if (typeof window === 'undefined' || !('EventSource' in window)) return;
    if (this.eventSource?.readyState === EventSource.OPEN) return;

    this.destroyed = false;
    this.openConnection();
  }

  disconnect(): void {
    this.destroyed = true;
    this.clearReconnectTimer();
    this.closeEventSource();
    this.isConnected.set(false);
    this.reconnectAttempts.set(0);
  }

  private openConnection(): void {
    this.closeEventSource();

    try {
      this.eventSource = new EventSource(SSE_STREAM_URL);

      this.eventSource.onopen = () => {
        this.isConnected.set(true);
        this.reconnectAttempts.set(0);
        this.currentBackoffMs = SSE_RECONNECT_BASE_MS;
        console.info('[SSE] Connected to live price stream');
      };

      this.eventSource.addEventListener('INIT', () => {
        this.isConnected.set(true);
      });

      this.eventSource.addEventListener('PRICE_DROP_ALERT', (event: MessageEvent) => {
        try {
          const alert = JSON.parse(event.data) as AlertItem;
          this.trackerStore.pushLiveAlert(alert);
          this.triggerAlertEffects();
        } catch {
          console.warn('[SSE] Failed to parse PRICE_DROP_ALERT payload');
        }
      });

      this.eventSource.onerror = () => {
        this.isConnected.set(false);
        this.closeEventSource();

        if (!this.destroyed) {
          this.scheduleReconnect();
        }
      };
    } catch (e) {
      console.error('[SSE] Failed to create EventSource:', e);
      this.isConnected.set(false);
      if (!this.destroyed) this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    this.clearReconnectTimer();
    const attempt = this.reconnectAttempts();
    this.reconnectAttempts.set(attempt + 1);

    console.info(
      `[SSE] Connection lost. Reconnecting in ${this.currentBackoffMs}ms (attempt ${attempt + 1})`
    );

    this.reconnectTimer = setTimeout(() => {
      if (!this.destroyed) {
        this.openConnection();
      }
    }, this.currentBackoffMs);

    this.currentBackoffMs = Math.min(
      this.currentBackoffMs * SSE_RECONNECT_MULTIPLIER,
      SSE_RECONNECT_MAX_MS
    );
  }

  private triggerAlertEffects(): void {
    if (this.flagStore.isEnabled(FLAG_KEYS.ALERT_SOUND)) {
      this.soundService.playPriceDropAlert();
    }
    if (this.flagStore.isEnabled(FLAG_KEYS.CONFETTI_ALERT)) {
      this.confettiService.burst();
    }
  }

  private closeEventSource(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }
}
