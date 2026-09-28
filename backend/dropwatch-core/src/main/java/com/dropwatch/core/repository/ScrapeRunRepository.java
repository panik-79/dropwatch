package com.dropwatch.core.repository;

import com.dropwatch.core.domain.ScrapeRun;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapeRunRepository extends MongoRepository<ScrapeRun, String> {
    List<ScrapeRun> findTop50ByHostOrderByTimestampDesc(String host);
    List<ScrapeRun> findTop50ByProductIdOrderByTimestampDesc(String productId);
}
