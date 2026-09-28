package com.dropwatch.scraper.spi;

import java.io.Serializable;
import java.util.Map;

public record FetchResponse(
        int statusCode,
        String body,
        Map<String, String> headers,
        String strategyUsed
) implements Serializable {}
