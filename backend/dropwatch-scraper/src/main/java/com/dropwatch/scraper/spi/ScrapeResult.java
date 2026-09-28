package com.dropwatch.scraper.spi;

import java.io.Serializable;
import java.time.Instant;

/**
 * Sealed hierarchy representing all possible outcomes of a scraping attempt.
 */
public sealed interface ScrapeResult extends Serializable {

    record Success(
            ProductSnapshot product,
            Instant fetchedAt,
            String strategyUsed,
            long executionTimeMs
    ) implements ScrapeResult {}

    record Blocked(
            String reason,
            int httpStatusCode,
            String strategyUsed
    ) implements ScrapeResult {}

    record ParseFailure(
            String errorMsg,
            String rawSampleSnippet,
            String strategyUsed
    ) implements ScrapeResult {}

    record NotFound(
            String siteProductId,
            String canonicalUrl
    ) implements ScrapeResult {}

    record TransientError(
            String errorMsg,
            String strategyUsed,
            boolean retryable
    ) implements ScrapeResult {}
}
