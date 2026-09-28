package com.dropwatch.scraper.spi;

import java.io.Serializable;
import java.util.Map;

public record FetchContext(
        String traceId,
        int attempt,
        String preferredStrategy,
        Map<String, String> customHeaders
) implements Serializable {

    public static FetchContext defaultContext(String traceId) {
        return new FetchContext(traceId, 1, "DIRECT_HTTP", Map.of());
    }
}
