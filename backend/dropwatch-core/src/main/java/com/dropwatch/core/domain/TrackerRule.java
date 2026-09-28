package com.dropwatch.core.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TrackerRule.TargetPriceRule.class, name = "TARGET_PRICE"),
        @JsonSubTypes.Type(value = TrackerRule.PercentDropRule.class, name = "PERCENT_DROP"),
        @JsonSubTypes.Type(value = TrackerRule.BackInStockRule.class, name = "BACK_IN_STOCK"),
        @JsonSubTypes.Type(value = TrackerRule.AllTimeLowRule.class, name = "ALL_TIME_LOW"),
        @JsonSubTypes.Type(value = TrackerRule.DiscountThresholdRule.class, name = "DISCOUNT_THRESHOLD"),
        @JsonSubTypes.Type(value = TrackerRule.CompositeRule.class, name = "COMPOSITE")
})
public sealed interface TrackerRule extends Serializable {

    String id();
    String ruleType();

    record TargetPriceRule(
            String id,
            BigDecimal targetPrice
    ) implements TrackerRule {
        @Override public String ruleType() { return "TARGET_PRICE"; }
    }

    enum Baseline {
        LAST,
        MAX_30D,
        MRP
    }

    record PercentDropRule(
            String id,
            double percent,
            Baseline baseline
    ) implements TrackerRule {
        @Override public String ruleType() { return "PERCENT_DROP"; }
    }

    record BackInStockRule(
            String id
    ) implements TrackerRule {
        @Override public String ruleType() { return "BACK_IN_STOCK"; }
    }

    record AllTimeLowRule(
            String id
    ) implements TrackerRule {
        @Override public String ruleType() { return "ALL_TIME_LOW"; }
    }

    record DiscountThresholdRule(
            String id,
            double minDiscountPercent
    ) implements TrackerRule {
        @Override public String ruleType() { return "DISCOUNT_THRESHOLD"; }
    }

    enum LogicalOperator {
        AND,
        OR
    }

    record CompositeRule(
            String id,
            LogicalOperator operator,
            List<TrackerRule> rules
    ) implements TrackerRule {
        @Override public String ruleType() { return "COMPOSITE"; }
    }
}
