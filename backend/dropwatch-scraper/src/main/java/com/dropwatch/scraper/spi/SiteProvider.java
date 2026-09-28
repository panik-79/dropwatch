package com.dropwatch.scraper.spi;

import com.dropwatch.core.domain.Site;

public interface SiteProvider {
    Site getSupportedSite();
    boolean supportsUrl(String url);
    String extractSiteProductId(String url);
    ScrapeResult parse(ProductRef productRef, FetchContext context, FetchResponse fetchResponse);
}
