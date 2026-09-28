package com.dropwatch.scraper.engine;

import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.engine.strategy.FetchStrategyChain;
import com.dropwatch.scraper.resilience.DomainRateLimiter;
import com.dropwatch.scraper.resilience.ScraperResilienceService;
import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.ProductRef;
import com.dropwatch.scraper.spi.ScrapeResult;
import com.dropwatch.scraper.spi.SiteProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScraperEngineService {

    private static final Logger log = LoggerFactory.getLogger(ScraperEngineService.class);

    private final Map<Site, SiteProvider> siteProviders;
    private final FetchStrategyChain strategyChain;
    private final DomainRateLimiter rateLimiter;
    private final ScraperResilienceService resilienceService;

    public ScraperEngineService(
            List<SiteProvider> providers,
            FetchStrategyChain strategyChain,
            DomainRateLimiter rateLimiter,
            ScraperResilienceService resilienceService
    ) {
        this.siteProviders = providers.stream()
                .collect(Collectors.toMap(SiteProvider::getSupportedSite, Function.identity()));
        this.strategyChain = strategyChain;
        this.rateLimiter = rateLimiter;
        this.resilienceService = resilienceService;
    }

    public ScrapeResult scrapeProduct(ProductRef productRef, FetchContext context) {
        Site site = productRef.site();
        SiteProvider provider = siteProviders.get(site);
        if (provider == null) {
            return new ScrapeResult.TransientError(
                    "No SiteProvider registered for site: " + site,
                    "NONE",
                    false
            );
        }

        try {
            rateLimiter.acquire(site);

            return resilienceService.executeWithCircuitBreaker(site, () -> {
                try {
                    FetchResponse fetchResponse = strategyChain.executeChain(productRef.canonicalUrl().toString(), context);
                    return provider.parse(productRef, context, fetchResponse);
                } catch (Exception e) {
                    log.error("Scrape fetch failed for URL {}: {}", productRef.canonicalUrl(), e.getMessage());
                    return new ScrapeResult.TransientError(
                            "Fetch exception: " + e.getMessage(),
                            "CHAIN_FAILED",
                            true
                    );
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ScrapeResult.TransientError("Rate limiting interrupted", "NONE", true);
        } catch (Exception e) {
            log.error("Scraper engine error: {}", e.getMessage(), e);
            return new ScrapeResult.TransientError("Engine error: " + e.getMessage(), "NONE", true);
        }
    }
}
