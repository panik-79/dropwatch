import { PriceSnapshot, PriceAnalyticsResult, BuyConfidenceTier } from '../models/tracker.models';

/**
 * Calculates comprehensive price intelligence metrics and Buy Confidence Score.
 * Zero-dependency pure function.
 */
export function calculatePriceAnalytics(
  snapshots: PriceSnapshot[] = [],
  currentPrice: number = 0,
  mrp: number = 0
): PriceAnalyticsResult {
  const safeCurrentPrice = Math.max(0, Number(currentPrice || 0));
  const safeMrp = Math.max(safeCurrentPrice, Number(mrp || 0));
  const savingsVsMrp = safeMrp > safeCurrentPrice ? safeMrp - safeCurrentPrice : 0;

  // Extract valid non-zero prices from historical snapshots
  const validHistoryPrices = snapshots
    .map((s) => Number(s.sellingPrice))
    .filter((p) => !isNaN(p) && p > 0);

  // If no current price is available
  if (safeCurrentPrice <= 0) {
    return {
      score: 50,
      tier: 'FAIR',
      headline: '⚖️ Standard Listing Price',
      isAllTimeLow: false,
      is30DayLow: false,
      allTimeLow: 0,
      allTimeHigh: 0,
      avg30DayPrice: 0,
      dropFromAvgPercent: 0,
      savingsVsMrp: 0,
    };
  }

  // Combine history with current price for complete window analysis
  const allPrices = validHistoryPrices.length > 0
    ? [...validHistoryPrices, safeCurrentPrice]
    : [safeCurrentPrice];

  const allTimeLow = Math.min(...allPrices);
  const allTimeHigh = Math.max(...allPrices);

  const sumPrices = allPrices.reduce((acc, p) => acc + p, 0);
  const avg30DayPrice = Math.round(sumPrices / allPrices.length);

  const isAllTimeLow = safeCurrentPrice <= allTimeLow + 1 && allPrices.length > 1;
  const is30DayLow = safeCurrentPrice <= allTimeLow + 1;

  const dropFromAvgPercent = avg30DayPrice > safeCurrentPrice
    ? Math.round(((avg30DayPrice - safeCurrentPrice) / avg30DayPrice) * 100)
    : 0;

  const discountVsMrpPercent = safeMrp > safeCurrentPrice
    ? Math.round(((safeMrp - safeCurrentPrice) / safeMrp) * 100)
    : 0;

  // ─── Score Algorithm (0 to 100) ───────────────────────────────────────────
  let score = 50;

  if (isAllTimeLow) {
    score += 35;
  } else if (is30DayLow) {
    score += 20;
  }

  if (dropFromAvgPercent > 0) {
    score += Math.min(25, Math.round(dropFromAvgPercent * 1.2));
  } else if (safeCurrentPrice >= allTimeHigh && allPrices.length > 2) {
    score -= 25;
  }

  if (discountVsMrpPercent > 0) {
    score += Math.min(15, Math.round(discountVsMrpPercent * 0.25));
  }

  // Clamp score between 15 and 99
  score = Math.max(15, Math.min(99, Math.round(score)));

  // Determine Tier and Headline
  let tier: BuyConfidenceTier = 'FAIR';
  let headline = '⚖️ Fair Price — Near 30-day average';

  if (score >= 80) {
    tier = 'EXCELLENT';
    headline = isAllTimeLow
      ? '🔥 Steal Deal — All-Time Lowest Price Recorded!'
      : `🔥 Exceptional Value — ${dropFromAvgPercent}% below 30-day average!`;
  } else if (score >= 65) {
    tier = 'GOOD';
    headline = dropFromAvgPercent > 0
      ? `✅ Good Deal — ${dropFromAvgPercent}% below 30-day average`
      : `✅ Solid Value — ${discountVsMrpPercent}% off MRP`;
  } else if (score >= 45) {
    tier = 'FAIR';
    headline = '⚖️ Fair Market Price — Normal historical range';
  } else {
    tier = 'WEAK';
    headline = '⚠️ Hold Off — Price is near recent peak high';
  }

  return {
    score,
    tier,
    headline,
    isAllTimeLow,
    is30DayLow,
    allTimeLow,
    allTimeHigh,
    avg30DayPrice,
    dropFromAvgPercent,
    savingsVsMrp,
  };
}
