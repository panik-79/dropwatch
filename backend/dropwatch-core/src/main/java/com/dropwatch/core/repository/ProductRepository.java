package com.dropwatch.core.repository;

import com.dropwatch.core.domain.Product;
import com.dropwatch.core.domain.Site;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySiteAndSiteProductId(Site site, String siteProductId);
    Optional<Product> findByCanonicalUrl(String canonicalUrl);

    /**
     * Case-insensitive title search using MongoDB regex.
     * Requires a text index on `title` field for production-scale performance.
     */
    List<Product> findByTitleContainingIgnoreCase(String titleFragment);
}
