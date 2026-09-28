package com.dropwatch.scraper.spi;

import com.dropwatch.core.domain.Site;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ProductSnapshot(
        Site site,
        String siteProductId,
        String title,
        String brand,
        String imageUrl,
        String category,
        String canonicalUrl,
        List<VariantSnapshot> variants,
        Map<String, Object> attributes,
        String parserVersion
) implements Serializable {}
