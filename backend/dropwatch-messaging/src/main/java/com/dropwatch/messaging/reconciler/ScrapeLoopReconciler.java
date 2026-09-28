package com.dropwatch.messaging.reconciler;

import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.repository.TrackerRepository;
import com.dropwatch.messaging.dto.ScrapeTask;
import com.dropwatch.messaging.producer.ScrapeTaskProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ScrapeLoopReconciler {

    private static final Logger log = LoggerFactory.getLogger(ScrapeLoopReconciler.class);

    private final TrackerRepository trackerRepository;
    private final ScrapeTaskProducer producer;
    private final Set<String> activeProductsInFlight = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastScrapedTimestampMap = new ConcurrentHashMap<>();

    public ScrapeLoopReconciler(TrackerRepository trackerRepository, ScrapeTaskProducer producer) {
        this.trackerRepository = trackerRepository;
        this.producer = producer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("ScrapeLoopReconciler initializing on application startup...");
        reconcileActiveTrackers();
    }

    // Run ticker loop every 10 seconds to evaluate due poll intervals
    @Scheduled(fixedRate = 10000)
    public void scheduledReconcile() {
        reconcileActiveTrackers();
    }

    public void reconcileActiveTrackers() {
        try {
            List<Tracker> activeTrackers = trackerRepository.findByActiveTrue();
            if (activeTrackers.isEmpty()) {
                return;
            }

            // Group active trackers by product ID
            Map<String, List<Tracker>> productTrackerMap = activeTrackers.stream()
                    .filter(t -> !t.isPaused())
                    .collect(Collectors.groupingBy(Tracker::getProductId));

            long now = System.currentTimeMillis();

            for (Map.Entry<String, List<Tracker>> entry : productTrackerMap.entrySet()) {
                String productId = entry.getKey();
                List<Tracker> trackers = entry.getValue();

                // Minimum poll interval across active trackers for this product
                int minIntervalSec = trackers.stream()
                        .mapToInt(t -> t.getPollIntervalSeconds() > 0 ? t.getPollIntervalSeconds() : 60)
                        .min()
                        .orElse(60);

                long minIntervalMs = minIntervalSec * 1000L;
                long lastScraped = lastScrapedTimestampMap.getOrDefault(productId, 0L);

                if ((now - lastScraped >= minIntervalMs) && !activeProductsInFlight.contains(productId)) {
                    List<String> variantIds = trackers.stream()
                            .map(Tracker::getVariantId)
                            .distinct()
                            .collect(Collectors.toList());

                    ScrapeTask task = ScrapeTask.create(productId, variantIds, null);
                    producer.publishToWorkQueue(task);
                    activeProductsInFlight.add(productId);
                    lastScrapedTimestampMap.put(productId, now);
                    log.info("Enqueued periodic ScrapeTask for productId={}, pollInterval={}s", productId, minIntervalSec);
                }
            }
        } catch (Exception e) {
            log.error("Failed during ScrapeLoopReconciler execution", e);
        }
    }

    public void markProductCompleted(String productId) {
        if (productId != null) {
            activeProductsInFlight.remove(productId);
        }
    }
}
