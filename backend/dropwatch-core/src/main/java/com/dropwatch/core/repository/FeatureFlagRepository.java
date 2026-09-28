package com.dropwatch.core.repository;

import com.dropwatch.core.domain.FeatureFlagDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends MongoRepository<FeatureFlagDoc, String> {
    Optional<FeatureFlagDoc> findByKey(String key);
}
