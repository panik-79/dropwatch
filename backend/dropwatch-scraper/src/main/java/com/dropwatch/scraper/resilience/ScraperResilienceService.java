package com.dropwatch.scraper.resilience;

import com.dropwatch.core.domain.Site;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ScraperResilienceService {

    private static final Logger log = LoggerFactory.getLogger(ScraperResilienceService.class);

    private final CircuitBreakerRegistry registry;
    private final Map<Site, CircuitBreaker> siteCircuitBreakers = new ConcurrentHashMap<>();

    public ScraperResilienceService() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
        this.registry = CircuitBreakerRegistry.of(config);
    }

    public <T> T executeWithCircuitBreaker(Site site, Supplier<T> supplier) {
        CircuitBreaker cb = siteCircuitBreakers.computeIfAbsent(site,
                s -> registry.circuitBreaker("scraper-" + s.name().toLowerCase()));

        return CircuitBreaker.decorateSupplier(cb, supplier).get();
    }
}
