package com.dropwatch.notify;

import com.dropwatch.core.domain.TrackerRule;
import com.dropwatch.notify.rule.AnyDiscountEvaluator;
import com.dropwatch.notify.rule.BackInStockEvaluator;
import com.dropwatch.notify.rule.PercentageDropEvaluator;
import com.dropwatch.notify.rule.PriceThresholdEvaluator;
import com.dropwatch.notify.rule.RuleEngineService;
import com.dropwatch.notify.rule.RuleEvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService(List.of(
                new PriceThresholdEvaluator(),
                new PercentageDropEvaluator(),
                new BackInStockEvaluator(),
                new AnyDiscountEvaluator()
        ));
    }

    @Test
    void testPriceThresholdTriggered() {
        TrackerRule targetPriceRule = new TrackerRule.TargetPriceRule("r1", new BigDecimal("500.00"));
        Optional<RuleEvaluationResult> result = ruleEngineService.evaluateRules(
                List.of(targetPriceRule),
                new BigDecimal("600.00"),
                new BigDecimal("450.00"),
                true,
                true
        );

        assertTrue(result.isPresent());
        assertTrue(result.get().triggered());
        assertEquals("TARGET_PRICE", result.get().ruleType());
        assertEquals(new BigDecimal("150.00"), result.get().savingsAmount());
    }

    @Test
    void testPercentageDropTriggered() {
        TrackerRule pctDropRule = new TrackerRule.PercentDropRule("r2", 20.0, TrackerRule.Baseline.LAST);
        Optional<RuleEvaluationResult> result = ruleEngineService.evaluateRules(
                List.of(pctDropRule),
                new BigDecimal("1000.00"),
                new BigDecimal("750.00"),
                true,
                true
        );

        assertTrue(result.isPresent());
        assertTrue(result.get().triggered());
        assertEquals(25.0, result.get().percentageDrop());
    }

    @Test
    void testBackInStockTriggered() {
        TrackerRule stockRule = new TrackerRule.BackInStockRule("r3");
        Optional<RuleEvaluationResult> result = ruleEngineService.evaluateRules(
                List.of(stockRule),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                false,
                true
        );

        assertTrue(result.isPresent());
        assertTrue(result.get().triggered());
        assertEquals("BACK_IN_STOCK", result.get().ruleType());
    }
}
