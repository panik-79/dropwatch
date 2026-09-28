package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class AnyDiscountEvaluator implements AlertRuleEvaluator {

    public static final String TYPE = "DISCOUNT_THRESHOLD";

    @Override
    public String getSupportedRuleType() {
        return TYPE;
    }

    @Override
    public RuleEvaluationResult evaluate(
            TrackerRule rule,
            BigDecimal previousPrice,
            BigDecimal currentPrice,
            boolean previouslyInStock,
            boolean currentlyInStock
    ) {
        if (!(rule instanceof TrackerRule.DiscountThresholdRule discRule) || previousPrice == null || currentPrice == null) {
            return RuleEvaluationResult.notTriggered(TYPE);
        }

        if (currentPrice.compareTo(previousPrice) < 0 && currentlyInStock) {
            BigDecimal savings = previousPrice.subtract(currentPrice);
            double dropPct = previousPrice.subtract(currentPrice)
                    .divide(previousPrice, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;

            if (dropPct >= discRule.minDiscountPercent()) {
                return RuleEvaluationResult.triggered(
                        TYPE,
                        "Price dropped from " + previousPrice + " to " + currentPrice,
                        previousPrice,
                        currentPrice,
                        savings,
                        Math.round(dropPct * 100.0) / 100.0
                );
            }
        }
        return RuleEvaluationResult.notTriggered(TYPE);
    }
}
