package com.dropwatch.scraper.engine.strategy;

import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.FetchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FetchStrategyChain {

    private static final Logger log = LoggerFactory.getLogger(FetchStrategyChain.class);

    private final Map<String, FetchStrategy> strategies;
    private final List<String> fallbackOrder;

    public FetchStrategyChain(List<FetchStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(FetchStrategy::getStrategyName, Function.identity()));
        this.fallbackOrder = List.of(
                DirectHttpStrategy.NAME,
                ProxyStrategy.NAME,
                HeadlessBrowserStrategy.NAME
        );
    }

    public FetchResponse executeChain(String url, FetchContext context) throws Exception {
        boolean isInteractive = context != null && context.traceId() != null && context.traceId().startsWith("api-parse-");
        boolean isMyntra = url != null && (url.contains("myntra.com") || url.contains("mynt.in"));

        // Interactive parse requests from UI modal: try fast single-pass Direct HTTP first
        if (isInteractive) {
            FetchStrategy strat = strategies.get(DirectHttpStrategy.NAME);
            if (strat != null && strat.isAvailable()) {
                try {
                    FetchResponse resp = strat.fetch(url, context);
                    if (isSuccessfulOrNotFound(resp.statusCode())
                            && resp.body() != null
                            && !resp.body().isBlank()
                            && isUsableBody(resp.body(), isMyntra)) {
                        return resp;
                    }
                    log.warn("Interactive DirectHttp returned status {} with unusable body. Cascading through fallback chain...", resp.statusCode());
                } catch (Exception e) {
                    log.warn("Interactive fetch error for {}: {}", url, e.getMessage());
                }
            }
        }

        String preferred = context != null ? context.preferredStrategy() : null;
        if (preferred != null && strategies.containsKey(preferred)) {
            FetchStrategy strat = strategies.get(preferred);
            if (strat.isAvailable()) {
                try {
                    FetchResponse resp = strat.fetch(url, context);
                    if (isSuccessfulOrNotFound(resp.statusCode()) && isUsableBody(resp.body(), isMyntra)) {
                        return resp;
                    }
                    log.warn("Preferred strategy {} returned status {}. Cascading...", preferred, resp.statusCode());
                } catch (Exception e) {
                    log.warn("Preferred strategy {} failed for {}: {}", preferred, url, e.getMessage());
                }
            }
        }

        // Execute fallback chain for background workers
        Exception lastException = null;
        for (String stratName : fallbackOrder) {
            FetchStrategy strat = strategies.get(stratName);
            if (strat == null || !strat.isAvailable()) {
                continue;
            }

            try {
                FetchResponse resp = strat.fetch(url, context);
                if (isSuccessfulOrNotFound(resp.statusCode()) && isUsableBody(resp.body(), isMyntra)) {
                    return resp;
                }
                log.warn("Strategy {} returned status code {} with unusable body for {}. Trying next...", stratName, resp.statusCode(), url);
            } catch (Exception e) {
                log.warn("Strategy {} threw exception for {}: {}", stratName, url, e.getMessage());
                lastException = e;
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IllegalStateException("All fetch strategies in chain failed to fetch URL: " + url);
    }

    /**
     * Detects bot-block / login-redirect pages that return HTTP 200 but contain no product data.
     * Myntra specifically returns a minimal React shell (~10KB) when blocking cloud IPs.
     */
    private boolean isUsableBody(String body, boolean isMyntra) {
        if (body == null || body.isBlank()) return false;
        if (!isMyntra) return true; // Only apply quality check for Myntra

        // A real Myntra PDP page is typically 400KB+. A bot-block page is <50KB.
        // Also check for key data markers.
        boolean hasProductData = body.contains("window.__myx") || body.contains("pdpData")
                || body.contains("application/ld+json") || body.contains("pdp-title")
                || body.contains("pdp-price");
        boolean isBlockPage = body.contains("loginModal") && body.length() < 80_000
                || body.contains("Please verify you are a human") || body.contains("captcha")
                || body.contains("Access Denied");

        if (isBlockPage) {
            log.warn("[FetchStrategyChain] Myntra bot-detection page detected (body {}KB)", body.length() / 1024);
            return false;
        }
        if (!hasProductData && body.length() < 50_000) {
            log.warn("[FetchStrategyChain] Myntra response has no product data markers and is too small ({}KB) — likely bot block", body.length() / 1024);
            return false;
        }
        return true;
    }

    private boolean isSuccessfulOrNotFound(int statusCode) {
        return (statusCode >= 200 && statusCode < 300) || statusCode == 404;
    }
}
