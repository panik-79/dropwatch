package com.dropwatch.notify.rule;

import com.dropwatch.core.domain.TrackerRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);

    private final Map<String, AlertRuleEvaluator> evaluators;

    public RuleEngineService(List<AlertRuleEvaluator> evaluatorList) {
        this.evaluators = evaluatorList.stream()
                .collect(Collectors.toMap(AlertRuleEvaluator::getSupportedRuleType, Function.identity()));
    }

    public Optional<RuleEvaluationResult> evaluateRules(
            List<TrackerRule> rules,
            BigDecimal previousPrice,
            BigDecimal currentPrice,
            boolean previouslyInStock,
            boolean currentlyInStock
    ) {
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }

        for (TrackerRule rule : rules) {
            String ruleType = rule.ruleType();
            AlertRuleEvaluator evaluator = evaluators.get(ruleType);

            if (evaluator != null) {
                RuleEvaluationResult result = evaluator.evaluate(
                        rule, previousPrice, currentPrice, previouslyInStock, currentlyInStock
                );
                if (result.triggered()) {
                    log.info("Rule triggered: type={}, reason={}", ruleType, result.reason());
                    return Optional.of(result);
                }
            }
        }

        return Optional.empty();
    }
}
