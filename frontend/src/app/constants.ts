/**
 * Application-wide constants for DropWatch.
 * All magic numbers, URLs, and default values live here.
 * Never hardcode these values inline in components.
 */

// ─── API ──────────────────────────────────────────────────────────────────────
/** @deprecated Use environment.apiBaseUrl from environments/environment.ts instead */
export const API_BASE_URL = '';
/** @deprecated Use environment.sseStreamUrl from environments/environment.ts instead */
export const SSE_STREAM_URL = 'http://localhost:8080/api/v1/stream/live';

// ─── Placeholder Assets ───────────────────────────────────────────────────────
export const PLACEHOLDER_IMAGE_URL = 'https://api.invena.pl/images/product_image_placeholder.png';

// ─── Tracker Defaults ─────────────────────────────────────────────────────────
export const DEFAULT_POLL_INTERVAL_SECONDS = 3600;   // 1 hour
export const DEFAULT_COOLDOWN_SECONDS = 3600;          // 1 hour
export const DEFAULT_TARGET_PRICE = 4999;
export const FALLBACK_PRICE = 0;                       // shown when no price data available

// ─── Price Presets (₹) ───────────────────────────────────────────────────────
export const PRICE_PRESETS: readonly number[] = [9999, 4999, 3499, 1999, 999] as const;

// ─── Poll Interval Options ────────────────────────────────────────────────────
export interface IntervalOption {
  readonly label: string;
  readonly value: number;
}

export const POLL_INTERVAL_OPTIONS: readonly IntervalOption[] = [
  { label: 'Every 1 Minute (High Priority)', value: 60 },
  { label: 'Every 5 Minutes (Standard)', value: 300 },
  { label: 'Every 15 Minutes (Relaxed)', value: 900 },
  { label: 'Every 1 Hour (Default & Recommended)', value: 3600 },
  { label: 'Every 24 Hours (Low Priority)', value: 86400 },
] as const;

export const COOLDOWN_OPTIONS: readonly IntervalOption[] = [
  { label: 'Every 1 Minute', value: 60 },
  { label: 'Every 5 Minutes', value: 300 },
  { label: 'Every 15 Minutes', value: 900 },
  { label: 'Every 30 Minutes', value: 1800 },
  { label: 'Every 1 Hour (Default)', value: 3600 },
  { label: 'Every 3 Hours', value: 10800 },
  { label: 'Every 6 Hours', value: 21600 },
  { label: 'Every 24 Hours', value: 86400 },
] as const;

// ─── Feature Flag Keys ────────────────────────────────────────────────────────
export const FLAG_KEYS = {
  PRICE_CHARTS: 'ui.price-charts',
  ALERTS_TELEGRAM: 'alerts.telegram',
  CONFETTI_ALERT: 'ui.confetti-alert',
  ALERT_SOUND: 'ui.alert-sound',
  COMPACT_MODE: 'ui.compact-mode',
  QUICK_ACTIONS_BAR: 'ui.quick-actions-bar',
  PRICE_TREND: 'ui.price-trend',
  PRICE_ANALYTICS: 'ui.price-analytics',
} as const;

// ─── SSE Reconnect Config ─────────────────────────────────────────────────────
export const SSE_RECONNECT_BASE_MS = 2000;
export const SSE_RECONNECT_MAX_MS = 60_000;
export const SSE_RECONNECT_MULTIPLIER = 2;

// ─── Theme ────────────────────────────────────────────────────────────────────
export const THEME_STORAGE_KEY = 'dropwatch_theme';
export type Theme = 'dark' | 'light';

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const JWT_STORAGE_KEY = 'dropwatch_jwt';
