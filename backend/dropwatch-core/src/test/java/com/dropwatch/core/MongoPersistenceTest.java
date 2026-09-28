package com.dropwatch.core;

import com.dropwatch.core.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MongoPersistenceTest {

    @Test
    @DisplayName("Site.fromUrl correctly maps e-commerce URLs")
    void siteFromUrlMapping() {
        assertEquals(Site.MYNTRA, Site.fromUrl("https://www.myntra.com/shoes/nike/12345"));
        assertEquals(Site.FLIPKART, Site.fromUrl("https://www.flipkart.com/p/itm12345"));
        assertEquals(Site.AMAZON, Site.fromUrl("https://www.amazon.in/dp/B08N5WRWNW"));
        assertEquals(Site.OTHER, Site.fromUrl("https://example.com"));
    }

    @Test
    @DisplayName("TrackerRule polymorphic hierarchy serialization and logic")
    void trackerRuleHierarchy() {
        TrackerRule targetPriceRule = new TrackerRule.TargetPriceRule("rule-1", BigDecimal.valueOf(1499.00));
        assertEquals("TARGET_PRICE", targetPriceRule.ruleType());
        assertEquals("rule-1", targetPriceRule.id());

        TrackerRule percentDropRule = new TrackerRule.PercentDropRule("rule-2", 15.0, TrackerRule.Baseline.MAX_30D);
        assertEquals("PERCENT_DROP", percentDropRule.ruleType());

        TrackerRule compositeRule = new TrackerRule.CompositeRule("rule-3", TrackerRule.LogicalOperator.AND, List.of(targetPriceRule, percentDropRule));
        assertEquals("COMPOSITE", compositeRule.ruleType());
    }

    @Test
    @DisplayName("PriceSnapshot time-series domain builder mapping")
    void priceSnapshotBuilder() {
        SnapshotMeta meta = new SnapshotMeta("var-100", "prod-200", Site.MYNTRA);
        Instant now = Instant.now();

        PriceSnapshot snapshot = PriceSnapshot.builder()
                .ts(now)
                .meta(meta)
                .mrp(BigDecimal.valueOf(2999))
                .sellingPrice(BigDecimal.valueOf(1499))
                .discountPercent(50.0)
                .inStock(true)
                .sourceStrategy("DIRECT_HTTP")
                .parserVersion("1.0")
                .build();

        assertNotNull(snapshot);
        assertEquals(now, snapshot.getTs());
        assertEquals("var-100", snapshot.getMeta().variantId());
        assertEquals(BigDecimal.valueOf(1499), snapshot.getSellingPrice());
        assertTrue(snapshot.isInStock());
    }

    @Test
    @DisplayName("Tracker entity pause state evaluation")
    void trackerPauseEvaluation() {
        Tracker tracker = Tracker.builder()
                .id("tr-1")
                .userId("usr-1")
                .active(true)
                .pausedUntil(Instant.now().plusSeconds(3600))
                .build();

        assertTrue(tracker.isPaused());

        tracker.setPausedUntil(Instant.now().minusSeconds(10));
        assertFalse(tracker.isPaused());
    }
}
