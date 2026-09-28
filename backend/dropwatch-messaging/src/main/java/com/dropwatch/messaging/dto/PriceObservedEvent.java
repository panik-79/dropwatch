package com.dropwatch.messaging.dto;

import com.dropwatch.core.domain.PriceSnapshot;
import com.dropwatch.core.domain.Product;
import com.dropwatch.core.domain.Variant;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public record PriceObservedEvent(
        String eventId,
        Product product,
        List<Variant> variants,
        List<PriceSnapshot> snapshots,
        String traceId,
        Instant observedAt
) implements Serializable {

    public static PriceObservedEvent of(Product product, List<Variant> variants, List<PriceSnapshot> snapshots, String traceId) {
        return new PriceObservedEvent(
                java.util.UUID.randomUUID().toString(),
                product,
                variants,
                snapshots,
                traceId,
                Instant.now()
        );
    }
}
