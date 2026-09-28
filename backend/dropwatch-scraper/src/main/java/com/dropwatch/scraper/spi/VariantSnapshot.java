package com.dropwatch.scraper.spi;

import java.io.Serializable;
import java.math.BigDecimal;

public record VariantSnapshot(
        String label,
        String skuId,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        double discountPercent,
        boolean inStock,
        Integer quantityHint
) implements Serializable {}
