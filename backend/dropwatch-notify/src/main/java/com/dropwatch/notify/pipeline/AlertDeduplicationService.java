package com.dropwatch.notify.pipeline;

import com.dropwatch.core.repository.AlertEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AlertDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(AlertDeduplicationService.class);
    private static final long DEFAULT_COOLDOWN_SECONDS = 3600L;

    private final AlertEventRepository alertEventRepository;

    public AlertDeduplicationService(AlertEventRepository alertEventRepository) {
        this.alertEventRepository = alertEventRepository;
    }

    /**
     * Returns true if a recent alert already fired for this tracker within the cooldown window.
     * Uses a single indexed MongoDB query (findTop1) instead of streaming all events.
     */
    public boolean isDuplicateAlert(String trackerId, long cooldownSeconds) {
        if (trackerId == null) return false;

        long effectiveCooldown = cooldownSeconds > 0 ? cooldownSeconds : DEFAULT_COOLDOWN_SECONDS;
        Instant cutoff = Instant.now().minus(effectiveCooldown, ChronoUnit.SECONDS);

        MDC.put("trackerId", trackerId);
        MDC.put("cooldownSeconds", String.valueOf(effectiveCooldown));
        try {
            boolean recentExists = alertEventRepository
                    .findTop1ByTrackerIdAndCreatedAtAfter(trackerId, cutoff)
                    .isPresent();

            if (recentExists) {
                log.info("[Dedup] Alert suppressed for tracker — cooldown active ({}s)", effectiveCooldown);
                return true;
            }

            return false;
        } finally {
            MDC.remove("trackerId");
            MDC.remove("cooldownSeconds");
        }
    }
}
