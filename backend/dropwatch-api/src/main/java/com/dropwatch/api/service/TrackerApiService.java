package com.dropwatch.api.service;

import com.dropwatch.api.dto.ProductDto;
import com.dropwatch.api.dto.TrackerDto;
import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.domain.TrackerPriority;
import com.dropwatch.core.repository.TrackerRepository;
import com.dropwatch.messaging.dto.ScrapeTask;
import com.dropwatch.messaging.producer.ScrapeTaskProducer;
import com.dropwatch.notify.pipeline.NotificationOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TrackerApiService {

    private static final Logger log = LoggerFactory.getLogger(TrackerApiService.class);

    private final TrackerRepository trackerRepository;
    private final ProductService productService;
    private final ScrapeTaskProducer scrapeTaskProducer;
    private final NotificationOrchestratorService notificationOrchestratorService;

    public TrackerApiService(
            TrackerRepository trackerRepository,
            ProductService productService,
            ScrapeTaskProducer scrapeTaskProducer,
            NotificationOrchestratorService notificationOrchestratorService
    ) {
        this.trackerRepository = trackerRepository;
        this.productService = productService;
        this.scrapeTaskProducer = scrapeTaskProducer;
        this.notificationOrchestratorService = notificationOrchestratorService;
    }

    public TrackerDto.TrackerResponse createTracker(String userId, TrackerDto.CreateTrackerRequest req) {
        MDC.put("userId", userId);
        MDC.put("targetUrl", req.targetUrl());
        try {
            ProductDto.ProductResponse product = productService.parseAndSaveProductUrl(req.targetUrl());

            String targetVariantId = (req.variantId() != null && !req.variantId().isBlank())
                    ? req.variantId()
                    : (!product.variants().isEmpty() ? product.variants().get(0).getSiteSkuId() : "STANDARD");

            // Deduplication: Check if active tracker already exists for same user, product & variant
            Tracker existingTracker = trackerRepository.findByUserId(userId).stream()
                    .filter(t -> t.getProductId().equals(product.id()) && targetVariantId.equalsIgnoreCase(t.getVariantId()))
                    .findFirst()
                    .orElse(null);

            if (existingTracker != null) {
                log.info("Found existing active tracker id={} for userId={} productId={}. Updating rules.", existingTracker.getId(), userId, product.id());
                if (req.rules() != null && !req.rules().isEmpty()) existingTracker.setRules(req.rules());
                if (req.channelIds() != null && !req.channelIds().isEmpty()) existingTracker.setChannelIds(req.channelIds());
                if (req.pollIntervalSeconds() > 0) existingTracker.setPollIntervalSeconds(req.pollIntervalSeconds());
                if (req.cooldownSeconds() > 0) existingTracker.setCooldownSeconds(req.cooldownSeconds());
                existingTracker.setActive(true);
                Tracker updated = trackerRepository.save(existingTracker);
                return toDto(updated, product);
            }

            Tracker tracker = Tracker.builder()
                    .userId(userId)
                    .productId(product.id())
                    .variantId(targetVariantId)
                    .rules(req.rules())
                    .channelIds(req.channelIds())
                    .pollIntervalSeconds(req.pollIntervalSeconds() > 0 ? req.pollIntervalSeconds() : 3600)
                    .cooldownSeconds(req.cooldownSeconds() > 0 ? req.cooldownSeconds() : 3600)
                    .active(true)
                    .priority(TrackerPriority.NORMAL)
                    .createdAt(Instant.now())
                    .build();

            Tracker saved = trackerRepository.save(tracker);

            // Enqueue immediate initial scrape task via ScrapeTaskProducer
            ScrapeTask initialTask = ScrapeTask.create(saved.getProductId(), List.of(saved.getVariantId()), UUID.randomUUID().toString());
            scrapeTaskProducer.publishToWorkQueue(initialTask);

            // Immediately evaluate price rules against current product price upon tracker creation
            if (!product.variants().isEmpty()) {
                Object spObj = product.variants().get(0).getAttributes() != null ?
                        product.variants().get(0).getAttributes().get("sellingPrice") : null;
                if (spObj != null) {
                    java.math.BigDecimal currentPrice = new java.math.BigDecimal(String.valueOf(spObj));
                    notificationOrchestratorService.processPriceUpdate(
                            saved,
                            product.title(),
                            product.canonicalUrl(),
                            currentPrice,
                            currentPrice,
                            true,
                            true
                    );
                }
            }

            return toDto(saved, product);
        } finally {
            MDC.clear();
        }
    }

    public List<TrackerDto.TrackerResponse> getUserTrackers(String userId) {
        return trackerRepository.findByUserId(userId).stream()
                .map(t -> {
                    ProductDto.ProductResponse prod = productService.getProductById(t.getProductId());
                    return toDto(t, prod);
                })
                .collect(Collectors.toList());
    }

    public TrackerDto.TrackerResponse updateTracker(String userId, String trackerId, TrackerDto.UpdateTrackerRequest req) {
        Tracker t = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

        if (!t.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized tracker modification");
        }

        if (req.variantId() != null && !req.variantId().isBlank()) t.setVariantId(req.variantId());
        if (req.rules() != null) t.setRules(req.rules());
        if (req.channelIds() != null) t.setChannelIds(req.channelIds());
        if (req.pollIntervalSeconds() > 0) t.setPollIntervalSeconds(req.pollIntervalSeconds());
        if (req.cooldownSeconds() > 0) t.setCooldownSeconds(req.cooldownSeconds());
        if (req.active() != null) t.setActive(req.active());

        Tracker updated = trackerRepository.save(t);
        ProductDto.ProductResponse prod = productService.getProductById(updated.getProductId());
        return toDto(updated, prod);
    }

    public void deleteTracker(String userId, String trackerId) {
        Tracker t = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));

        if (!t.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized tracker deletion");
        }

        trackerRepository.deleteById(trackerId);
    }

    public TrackerDto.TrackerResponse pauseTracker(String userId, String trackerId) {
        Tracker t = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));
        t.setActive(false);
        Tracker saved = trackerRepository.save(t);
        return toDto(saved, productService.getProductById(saved.getProductId()));
    }

    public TrackerDto.TrackerResponse resumeTracker(String userId, String trackerId) {
        Tracker t = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new IllegalArgumentException("Tracker not found: " + trackerId));
        t.setActive(true);
        t.setPausedUntil(null);
        Tracker saved = trackerRepository.save(t);
        return toDto(saved, productService.getProductById(saved.getProductId()));
    }

    private TrackerDto.TrackerResponse toDto(Tracker t, ProductDto.ProductResponse product) {
        return new TrackerDto.TrackerResponse(
                t.getId(),
                t.getUserId(),
                t.getProductId(),
                t.getVariantId(),
                t.getRules(),
                t.isActive(),
                t.getPollIntervalSeconds(),
                t.getChannelIds(),
                t.getCooldownSeconds(),
                t.getLastAlertAt(),
                t.getCreatedAt(),
                product
        );
    }
}
