package com.dropwatch.scraper.resilience;

import com.dropwatch.core.domain.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket rate limiter for per-domain target request shaping.
 * Default limits: Myntra (2 req/sec), Flipkart (1 req/sec).
 */
@Component
public class DomainRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DomainRateLimiter.class);

    private final Map<Site, Semaphore> permitsMap = new ConcurrentHashMap<>();
    private final Map<Site, Integer> ratesMap = Map.of(
            Site.MYNTRA, 2,
            Site.FLIPKART, 1,
            Site.AMAZON, 1,
            Site.OTHER, 1
    );

    private final ScheduledExecutorService replenisher;

    public DomainRateLimiter() {
        ratesMap.forEach((site, rate) -> permitsMap.put(site, new Semaphore(rate)));
        this.replenisher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-replenisher");
            t.setDaemon(true);
            return t;
        });

        // Replenish permits every second
        this.replenisher.scheduleAtFixedRate(this::replenish, 1, 1, TimeUnit.SECONDS);
    }

    public void acquire(Site site) throws InterruptedException {
        Semaphore semaphore = permitsMap.computeIfAbsent(site, k -> new Semaphore(1));
        log.trace("Acquiring rate limit permit for {}", site);
        semaphore.acquire();
    }

    public boolean tryAcquire(Site site, long timeoutMs) throws InterruptedException {
        Semaphore semaphore = permitsMap.computeIfAbsent(site, k -> new Semaphore(1));
        return semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void replenish() {
        ratesMap.forEach((site, targetRate) -> {
            Semaphore sem = permitsMap.get(site);
            if (sem != null) {
                int missing = targetRate - sem.availablePermits();
                if (missing > 0) {
                    sem.release(missing);
                }
            }
        });
    }
}
