package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;

import java.math.BigDecimal;

public sealed interface AlertRuleEvaluator
        permits PriceThresholdEvaluator, PercentageDropEvaluator, BackInStockEvaluator, AnyDiscountEvaluator {

    String getSupportedRuleType();

    RuleEvaluationResult evaluate(
            TrackerRule rule,
            BigDecimal previousPrice,
            BigDecimal currentPrice,
            boolean previouslyInStock,
            boolean currentlyInStock
    );
}
