package com.dropwatch.core.repository;

import com.dropwatch.core.domain.PriceSnapshot;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceSnapshotRepository extends MongoRepository<PriceSnapshot, String> {

    @Query("{ 'meta.variantId': ?0, 'ts': { $gte: ?1, $lte: ?2 } }")
    List<PriceSnapshot> findByVariantIdAndDateRange(String variantId, Instant start, Instant end, Sort sort);

    @Query("{ 'meta.variantId': ?0 }")
    Optional<PriceSnapshot> findLatestByVariantId(String variantId, Sort sort);

    @Query(value = "{ 'meta.variantId': ?0 }", sort = "{ 'sellingPrice': 1 }")
    List<PriceSnapshot> findLowestPriceByVariantId(String variantId);
}
