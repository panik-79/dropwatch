package com.dropwatch.core.repository;

import com.dropwatch.core.domain.AlertEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertEventRepository extends MongoRepository<AlertEvent, String> {
    Optional<AlertEvent> findByDedupeKey(String dedupeKey);
    boolean existsByDedupeKey(String dedupeKey);

    /**
     * Efficient single-document lookup to check if any alert fired for this
     * tracker after the cutoff instant. Used by AlertDeduplicationService
     * to avoid streaming all events into memory.
     */
    Optional<AlertEvent> findTop1ByTrackerIdAndCreatedAtAfter(String trackerId, Instant cutoff);

    /** Paginated access for alert history display. */
    List<AlertEvent> findByTrackerIdOrderByCreatedAtDesc(String trackerId, Pageable pageable);

    List<AlertEvent> findByTrackerIdOrderByCreatedAtDesc(String trackerId);
}
