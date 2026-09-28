package com.dropwatch.api.dto;

import com.dropwatch.core.domain.PriceSnapshot;
import com.dropwatch.core.domain.Site;
import com.dropwatch.core.domain.Variant;

import java.io.Serializable;
import java.util.List;

public class ProductDto {

    public record ParseUrlRequest(
            String url
    ) implements Serializable {}

    public record ProductResponse(
            String id,
            Site site,
            String siteProductId,
            String title,
            String brand,
            String imageUrl,
            String category,
            String canonicalUrl,
            List<Variant> variants
    ) implements Serializable {}

    public record PriceHistoryResponse(
            String productId,
            String variantSku,
            List<PriceSnapshot> snapshots
    ) implements Serializable {}
}
