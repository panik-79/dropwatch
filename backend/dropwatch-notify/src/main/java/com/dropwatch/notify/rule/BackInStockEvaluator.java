package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public final class BackInStockEvaluator implements AlertRuleEvaluator {

    public static final String TYPE = "BACK_IN_STOCK";

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
        if (rule instanceof TrackerRule.BackInStockRule && !previouslyInStock && currentlyInStock) {
            return RuleEvaluationResult.triggered(
                    TYPE,
                    "Product is back in stock!",
                    previousPrice != null ? previousPrice : BigDecimal.ZERO,
                    currentPrice != null ? currentPrice : BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0.0
            );
        }
        return RuleEvaluationResult.notTriggered(TYPE);
    }
}
