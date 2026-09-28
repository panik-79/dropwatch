/**
 * Strict typed models for the DropWatch domain.
 * Replaces all `any` types in components and services.
 */

// ─── Product & Variants ───────────────────────────────────────────────────────
export type MerchantSite = 'MYNTRA' | 'FLIPKART' | 'AMAZON' | 'OTHER';

export interface VariantAttributes {
  mrp: number;
  sellingPrice: number;
  inStock: boolean;
  discountPercent: number;
  [key: string]: unknown;
}

export interface ProductVariant {
  id: string;
  productId: string;
  siteSkuId: string;
  label: string;
  mrp?: number;
  sellingPrice?: number;
  inStock?: boolean;
  attributes?: VariantAttributes;
}

export interface ProductInfo {
  id: string;
  site: MerchantSite;
  siteProductId: string;
  title: string;
  brand: string;
  imageUrl: string;
  category: string;
  canonicalUrl: string;
  variants: ProductVariant[];
}

// ─── Tracker Rules ────────────────────────────────────────────────────────────
export type RuleType = 'TARGET_PRICE' | 'PERCENT_DROP' | 'BACK_IN_STOCK';

export interface TrackerRule {
  id?: string;
  type: RuleType;
  targetPrice?: number;
  percentDrop?: number;
}

// ─── Tracker ──────────────────────────────────────────────────────────────────
export interface TrackerItem {
  id: string;
  userId: string;
  productId: string;
  variantId: string;
  rules: TrackerRule[];
  active: boolean;
  pollIntervalSeconds: number;
  channelIds: string[];
  cooldownSeconds: number;
  lastAlertAt?: string;
  createdAt: string;
  product: ProductInfo;
}

// ─── Alerts ───────────────────────────────────────────────────────────────────
export interface AlertItem {
  id: string;
  trackerId: string;
  ruleId?: string;
  ruleType: RuleType;
  priceAtTrigger: number;
  channel: string;
  deliveryStatus: 'PENDING' | 'SENT' | 'FAILED';
  createdAt: string;
  product?: Partial<ProductInfo>;
}

// ─── Price Snapshot & Analytics ───────────────────────────────────────────────
export interface PriceSnapshotMeta {
  productId: string;
  variantId: string;
  siteSkuId?: string;
  [key: string]: unknown;
}

export interface PriceSnapshot {
  id?: string;
  ts: string;
  mrp: number;
  sellingPrice: number;
  discountPercent: number;
  inStock: boolean;
  meta?: PriceSnapshotMeta;
}

export type BuyConfidenceTier = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'WEAK';

export interface PriceAnalyticsResult {
  score: number; // 0 - 100
  tier: BuyConfidenceTier;
  headline: string;
  isAllTimeLow: boolean;
  is30DayLow: boolean;
  allTimeLow: number;
  allTimeHigh: number;
  avg30DayPrice: number;
  dropFromAvgPercent: number;
  savingsVsMrp: number;
}

// ─── Feature Flags ────────────────────────────────────────────────────────────
export interface FeatureFlag {
  id: string;
  key: string;
  description: string;
  enabled: boolean;
  rolloutPercentage: number;
  targetUsers: string[];
}

// ─── Auth ─────────────────────────────────────────────────────────────────────
export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: string;
  createdAt: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Safely extract selling price from a variant, trying all known attribute shapes */
export function getVariantSellingPrice(variant: ProductVariant): number {
  return Number(variant.sellingPrice ?? variant.attributes?.sellingPrice ?? 0);
}

export function getVariantMrp(variant: ProductVariant): number {
  return Number(variant.mrp ?? variant.attributes?.mrp ?? getVariantSellingPrice(variant));
}

export function calcDiscountPercent(mrp: number, sellingPrice: number): number {
  if (mrp > sellingPrice && sellingPrice > 0) {
    return Math.round(((mrp - sellingPrice) / mrp) * 100);
  }
  return 0;
}

export function getSelectedVariant(product: ProductInfo | null | undefined, variantId?: string | null): ProductVariant | null {
  if (!product?.variants?.length) return null;
  if (variantId) {
    const found = product.variants.find(v => v.siteSkuId === variantId);
    if (found) return found;
  }
  return product.variants[0];
}

export function getPrimaryVariantSellingPrice(product: ProductInfo | null | undefined, variantId?: string | null): number {
  const v = getSelectedVariant(product, variantId);
  return v ? getVariantSellingPrice(v) : 0;
}

export function getPrimaryVariantMrp(product: ProductInfo | null | undefined, variantId?: string | null): number {
  const v = getSelectedVariant(product, variantId);
  return v ? getVariantMrp(v) : 0;
}

/** Dynamically extracts clean human-readable variant label (e.g., S, M, L, 32, 34, 256GB, Size 9) */
export function getVariantLabel(variant: ProductVariant | null | undefined): string {
  if (!variant) return '';
  if (variant.label) {
    if (variant.label === 'Standard' || variant.label === 'DEFAULT') {
      return 'Default';
    }
    return variant.label;
  }
  const attrs = variant.attributes;
  if (attrs) {
    if (attrs['size']) return String(attrs['size']);
    if (attrs['color']) return String(attrs['color']);
    if (attrs['label']) return String(attrs['label']);
    if (attrs['title']) return String(attrs['title']);
  }
  return 'Default';
}
