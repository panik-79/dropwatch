package com.dropwatch.notify.rule;

import java.io.Serializable;
import java.math.BigDecimal;

public record RuleEvaluationResult(
        boolean triggered,
        String ruleType,
        String reason,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal savingsAmount,
        double percentageDrop
) implements Serializable {

    public static RuleEvaluationResult notTriggered(String ruleType) {
        return new RuleEvaluationResult(false, ruleType, "Rule criteria not met", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
    }

    public static RuleEvaluationResult triggered(
            String ruleType,
            String reason,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            BigDecimal savingsAmount,
            double percentageDrop
    ) {
        return new RuleEvaluationResult(true, ruleType, reason, oldPrice, newPrice, savingsAmount, percentageDrop);
    }
}
