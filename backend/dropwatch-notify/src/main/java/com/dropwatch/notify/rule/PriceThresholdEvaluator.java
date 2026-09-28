package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class PriceThresholdEvaluator implements AlertRuleEvaluator {

    public static final String TYPE = "TARGET_PRICE";

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
        if (!(rule instanceof TrackerRule.TargetPriceRule targetRule) || currentPrice == null) {
            return RuleEvaluationResult.notTriggered(TYPE);
        }

        if (currentPrice.compareTo(targetRule.targetPrice()) <= 0 && currentlyInStock) {
            BigDecimal savings = previousPrice != null ? previousPrice.subtract(currentPrice).max(BigDecimal.ZERO) : BigDecimal.ZERO;
            double dropPct = 0.0;
            if (previousPrice != null && previousPrice.compareTo(BigDecimal.ZERO) > 0) {
                dropPct = previousPrice.subtract(currentPrice)
                        .divide(previousPrice, 4, RoundingMode.HALF_UP)
                        .doubleValue() * 100;
            }

            return RuleEvaluationResult.triggered(
                    TYPE,
                    "Price fell to " + currentPrice + " (target: " + targetRule.targetPrice() + ")",
                    previousPrice != null ? previousPrice : BigDecimal.ZERO,
                    currentPrice,
                    savings,
                    Math.round(dropPct * 100.0) / 100.0
            );
        }

        return RuleEvaluationResult.notTriggered(TYPE);
    }
}
