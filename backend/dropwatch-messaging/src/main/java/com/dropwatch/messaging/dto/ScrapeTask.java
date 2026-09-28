package com.dropwatch.messaging.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record ScrapeTask(
        String taskId,
        String productId,
        List<String> variantIds,
        int attempt,
        String traceId,
        Instant createdAt
) implements Serializable {

    public static ScrapeTask create(String productId, List<String> variantIds, String traceId) {
        return new ScrapeTask(
                java.util.UUID.randomUUID().toString(),
                productId,
                variantIds,
                1,
                traceId != null ? traceId : java.util.UUID.randomUUID().toString(),
                Instant.now()
        );
    }

    public ScrapeTask nextAttempt() {
        return new ScrapeTask(
                java.util.UUID.randomUUID().toString(),
                productId,
                variantIds,
                attempt + 1,
                traceId,
                Instant.now()
        );
    }
}
