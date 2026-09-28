package com.dropwatch.core.repository;

import com.dropwatch.core.domain.Tracker;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackerRepository extends MongoRepository<Tracker, String> {
    List<Tracker> findByUserId(String userId);
    List<Tracker> findByProductId(String productId);
    List<Tracker> findByVariantId(String variantId);
    List<Tracker> findByVariantIdAndActiveTrue(String variantId);
    List<Tracker> findByActiveTrue();
}
