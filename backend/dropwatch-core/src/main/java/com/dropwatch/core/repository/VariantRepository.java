package com.dropwatch.core.repository;

import com.dropwatch.core.domain.Variant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariantRepository extends MongoRepository<Variant, String> {
    List<Variant> findByProductId(String productId);
    Optional<Variant> findByProductIdAndSiteSkuId(String productId, String siteSkuId);
}
