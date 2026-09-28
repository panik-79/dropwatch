package com.dropwatch.scraper.spi;

import com.dropwatch.core.domain.Site;

import java.io.Serializable;
import java.net.URI;

public record ProductRef(
        Site site,
        String siteProductId,
        URI canonicalUrl
) implements Serializable {}
