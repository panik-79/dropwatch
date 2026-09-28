package com.dropwatch.core.domain;

import java.io.Serializable;

public record SnapshotMeta(
        String variantId,
        String productId,
        Site site
) implements Serializable {}
