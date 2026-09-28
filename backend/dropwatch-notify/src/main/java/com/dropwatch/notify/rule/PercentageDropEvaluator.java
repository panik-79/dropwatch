package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class PercentageDropEvaluator implements AlertRuleEvaluator {

    public static final String TYPE = "PERCENT_DROP";

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
        if (!(rule instanceof TrackerRule.PercentDropRule pctRule) || previousPrice == null || currentPrice == null || previousPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleEvaluationResult.notTriggered(TYPE);
        }

        if (currentPrice.compareTo(previousPrice) < 0 && currentlyInStock) {
            double actualDropPct = previousPrice.subtract(currentPrice)
                    .divide(previousPrice, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;

            if (actualDropPct >= pctRule.percent()) {
                BigDecimal savings = previousPrice.subtract(currentPrice);
                return RuleEvaluationResult.triggered(
                        TYPE,
                        String.format("Price dropped by %.2f%% (threshold: %.2f%%)", actualDropPct, pctRule.percent()),
                        previousPrice,
                        currentPrice,
                        savings,
                        Math.round(actualDropPct * 100.0) / 100.0
                );
            }
        }

        return RuleEvaluationResult.notTriggered(TYPE);
    }
}
