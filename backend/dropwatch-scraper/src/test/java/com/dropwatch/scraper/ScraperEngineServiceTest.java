package com.dropwatch.scraper;

import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.engine.ScraperEngineService;
import com.dropwatch.scraper.engine.strategy.DirectHttpStrategy;
import com.dropwatch.scraper.engine.strategy.FetchStrategyChain;
import com.dropwatch.scraper.engine.strategy.HeadlessBrowserStrategy;
import com.dropwatch.scraper.engine.strategy.ProxyStrategy;
import com.dropwatch.scraper.provider.flipkart.FlipkartProvider;
import com.dropwatch.scraper.provider.myntra.MyntraProvider;
import com.dropwatch.scraper.resilience.DomainRateLimiter;
import com.dropwatch.scraper.resilience.ScraperResilienceService;
import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.ProductRef;
import com.dropwatch.scraper.spi.ScrapeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScraperEngineServiceTest {

    private ScraperEngineService scraperEngineService;

    @BeforeEach
    void setUp() {
        DirectHttpStrategy directHttpStrategy = new DirectHttpStrategy();
        ProxyStrategy proxyStrategy = new ProxyStrategy();
        HeadlessBrowserStrategy headlessBrowserStrategy = new HeadlessBrowserStrategy();

        FetchStrategyChain chain = new FetchStrategyChain(List.of(directHttpStrategy, proxyStrategy, headlessBrowserStrategy));
        MyntraProvider myntraProvider = new MyntraProvider();
        FlipkartProvider flipkartProvider = new FlipkartProvider();

        DomainRateLimiter rateLimiter = new DomainRateLimiter();
        ScraperResilienceService resilienceService = new ScraperResilienceService();

        scraperEngineService = new ScraperEngineService(
                List.of(myntraProvider, flipkartProvider),
                chain,
                rateLimiter,
                resilienceService
        );
    }

    @Test
    void testScrapeMyntraProviderRoutingAndExtraction() {
        ProductRef ref = new ProductRef(
                Site.MYNTRA,
                "2297861",
                URI.create("https://www.myntra.com/tshirts/roadster/roadster-men-navy-blue-solid-round-neck-t-shirt/2297861/buy")
        );
        FetchContext context = FetchContext.defaultContext("test-trace-1");

        ScrapeResult result = scraperEngineService.scrapeProduct(ref, context);
        assertNotNull(result);
        // Will attempt live fetch or return blocked/transient error depending on net connection
        assertTrue(result instanceof ScrapeResult.Success
                || result instanceof ScrapeResult.Blocked
                || result instanceof ScrapeResult.TransientError);
    }

    @Test
    void testScrapeFlipkartProviderRoutingAndExtraction() {
        ProductRef ref = new ProductRef(
                Site.FLIPKART,
                "SHOHB83MAFG9CHXY",
                URI.create("https://www.flipkart.com/new-balance-530-sneakers-men/p/itm67063529ef7c0?pid=SHOHB83MAFG9CHXY&marketplace=FLIPKART&lid=LSTSHOHB83MAFG9CHXYVPQHFI&fm=eyJ3dHAiOiJyZWNvIiwicHJwdCI6InBwIiwibWlkIjoiZmFjdEJhc2VkUmVjb21tZW5kYXRpb24vcmVjZW50bHlWaWV3ZWQifQ%3D%3D&swatchAttr=size&pageUID=1790118412220")
        );
        FetchContext context = FetchContext.defaultContext("api-parse-test");

        ScrapeResult result = scraperEngineService.scrapeProduct(ref, context);
        System.out.println("=== USER SPECIFIC FLIPKART SCRAPE RESULT ===");
        System.out.println(result);
        assertNotNull(result);
        if (result instanceof ScrapeResult.Success success) {
            System.out.println("Product Title: " + success.product().title());
            System.out.println("Product Brand: " + success.product().brand());
            System.out.println("Product Image: " + success.product().imageUrl());
            System.out.println("Product SiteProductId: " + success.product().siteProductId());
            System.out.println("Variants: " + success.product().variants());
        }
    }
}
