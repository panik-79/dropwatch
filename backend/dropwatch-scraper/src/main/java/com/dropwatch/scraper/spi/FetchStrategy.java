package com.dropwatch.scraper.spi;

public interface FetchStrategy {
    String getStrategyName();
    boolean isAvailable();
    FetchResponse fetch(String url, FetchContext context) throws Exception;
}
