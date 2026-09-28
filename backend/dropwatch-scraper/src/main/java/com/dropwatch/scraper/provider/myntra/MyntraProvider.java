package com.dropwatch.scraper.provider.myntra;

import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.ProductRef;
import com.dropwatch.scraper.spi.ProductSnapshot;
import com.dropwatch.scraper.spi.ScrapeResult;
import com.dropwatch.scraper.spi.SiteProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MyntraProvider implements SiteProvider {

    private static final Logger log = LoggerFactory.getLogger(MyntraProvider.class);

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("/(\\d+)(?:/buy|\\?|$)");

    private final MyntraParser parser;

    public MyntraProvider() {
        this.parser = new MyntraParser();
    }

    @Override
    public Site getSupportedSite() {
        return Site.MYNTRA;
    }

    @Override
    public boolean supportsUrl(String url) {
        return url != null && (url.contains("myntra.com") || url.contains("mynt.in"));
    }

    @Override
    public String extractSiteProductId(String url) {
        if (url == null) return null;
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Pattern numPattern = Pattern.compile("(\\d{5,12})");
        Matcher numMatcher = numPattern.matcher(url);
        if (numMatcher.find()) {
            return numMatcher.group(1);
        }
        return "UNKNOWN";
    }

    @Override
    public ScrapeResult parse(ProductRef productRef, FetchContext context, FetchResponse response) {
        long startTime = System.currentTimeMillis();
        String targetUrl = productRef.canonicalUrl().toString();

        if (response.statusCode() == 404) {
            return new ScrapeResult.NotFound("UNKNOWN", targetUrl);
        }

        if (response.statusCode() == 403 || response.statusCode() == 429) {
            return new ScrapeResult.Blocked(
                    "Received status " + response.statusCode() + " from Myntra",
                    response.statusCode(),
                    response.strategyUsed()
            );
        }

        if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
            return new ScrapeResult.TransientError(
                    "HTTP " + response.statusCode() + " with empty or invalid response body",
                    response.strategyUsed(),
                    true
            );
        }

        try {
            String siteProductId = extractSiteProductId(targetUrl);
            ProductSnapshot snapshot = parser.parse(response.body(), targetUrl, siteProductId);
            long elapsed = System.currentTimeMillis() - startTime;

            return new ScrapeResult.Success(
                    snapshot,
                    Instant.now(),
                    response.strategyUsed(),
                    elapsed
            );
        } catch (Exception e) {
            log.error("Failed to parse Myntra page: {}", e.getMessage(), e);
            String snippet = response.body().length() > 200 ? response.body().substring(0, 200) : response.body();
            return new ScrapeResult.ParseFailure(
                    e.getMessage(),
                    snippet,
                    response.strategyUsed()
            );
        }
    }
}
