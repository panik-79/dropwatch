package com.dropwatch.app.service;

import com.dropwatch.core.domain.PriceSnapshot;
import com.dropwatch.core.domain.Product;
import com.dropwatch.core.domain.SnapshotMeta;
import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.domain.Variant;
import com.dropwatch.core.repository.PriceSnapshotRepository;
import com.dropwatch.core.repository.ProductRepository;
import com.dropwatch.core.repository.TrackerRepository;
import com.dropwatch.core.repository.VariantRepository;
import com.dropwatch.messaging.consumer.ScrapeTaskConsumer;
import com.dropwatch.messaging.dto.ScrapeTask;
import com.dropwatch.messaging.reconciler.ScrapeLoopReconciler;
import com.dropwatch.notify.pipeline.NotificationOrchestratorService;
import com.dropwatch.scraper.engine.ScraperEngineService;
import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.ProductRef;
import com.dropwatch.scraper.spi.ProductSnapshot;
import com.dropwatch.scraper.spi.ScrapeResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScrapeTaskExecutionService implements ScrapeTaskConsumer.TaskExecutionDelegate {

    private static final Logger log = LoggerFactory.getLogger(ScrapeTaskExecutionService.class);

    private final ScrapeTaskConsumer scrapeTaskConsumer;
    private final ProductRepository productRepository;
    private final TrackerRepository trackerRepository;
    private final VariantRepository variantRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ScraperEngineService scraperEngineService;
    private final NotificationOrchestratorService notificationOrchestratorService;
    private final ScrapeLoopReconciler scrapeLoopReconciler;

    public ScrapeTaskExecutionService(
            ScrapeTaskConsumer scrapeTaskConsumer,
            ProductRepository productRepository,
            TrackerRepository trackerRepository,
            VariantRepository variantRepository,
            PriceSnapshotRepository priceSnapshotRepository,
            ScraperEngineService scraperEngineService,
            NotificationOrchestratorService notificationOrchestratorService,
            ScrapeLoopReconciler scrapeLoopReconciler
    ) {
        this.scrapeTaskConsumer = scrapeTaskConsumer;
        this.productRepository = productRepository;
        this.trackerRepository = trackerRepository;
        this.variantRepository = variantRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.scraperEngineService = scraperEngineService;
        this.notificationOrchestratorService = notificationOrchestratorService;
        this.scrapeLoopReconciler = scrapeLoopReconciler;
    }

    @PostConstruct
    public void init() {
        scrapeTaskConsumer.setTaskExecutionDelegate(this);
        log.info("Registered ScrapeTaskExecutionService delegate with ScrapeTaskConsumer!");
    }

    @Override
    public boolean execute(ScrapeTask task) {
        log.info("Executing background scrape task for productId={}", task.productId());
        try {
            Product product = productRepository.findById(task.productId()).orElse(null);
            if (product == null) {
                log.warn("Product not found for taskId={}, productId={}", task.taskId(), task.productId());
                return true;
            }

            List<Tracker> trackers = trackerRepository.findByProductId(task.productId());
            if (trackers.isEmpty()) {
                log.info("No active trackers for productId={}, skipping evaluation.", task.productId());
                return true;
            }

            List<Variant> existingVariants = variantRepository.findByProductId(product.getId());
            Map<String, BigDecimal> oldPricesMap = new HashMap<>();
            for (Variant v : existingVariants) {
                if (v.getAttributes() != null && v.getAttributes().containsKey("sellingPrice")) {
                    try {
                        oldPricesMap.put(v.getSiteSkuId(), new BigDecimal(String.valueOf(v.getAttributes().get("sellingPrice"))));
                    } catch (Exception ignored) {}
                }
            }

            BigDecimal currentPrice = null;
            try {
                ProductRef ref = new ProductRef(product.getSite(), product.getSiteProductId(), URI.create(product.getCanonicalUrl()));
                ScrapeResult result = scraperEngineService.scrapeProduct(ref, FetchContext.defaultContext("scrape-job-" + task.taskId()));
                if (result instanceof ScrapeResult.Success success) {
                    ProductSnapshot snap = success.product();
                    if (snap != null && snap.variants() != null && !snap.variants().isEmpty()) {
                        for (var vSnap : snap.variants()) {
                            if (vSnap.sellingPrice() != null && vSnap.sellingPrice().compareTo(BigDecimal.ZERO) > 0) {
                                Variant existingVar = variantRepository.findByProductIdAndSiteSkuId(product.getId(), vSnap.skuId()).orElse(null);
                                if (existingVar != null) {
                                    if (vSnap.label() != null && !vSnap.label().isBlank()) {
                                        existingVar.setLabel(vSnap.label());
                                    }
                                    existingVar.setAttributes(Map.of(
                                            "mrp", vSnap.mrp() != null ? vSnap.mrp() : BigDecimal.ZERO,
                                            "sellingPrice", vSnap.sellingPrice(),
                                            "inStock", vSnap.inStock(),
                                            "discountPercent", vSnap.discountPercent()
                                    ));
                                    variantRepository.save(existingVar);
                                } else {
                                    Variant newVar = Variant.builder()
                                            .productId(product.getId())
                                            .siteSkuId(vSnap.skuId())
                                            .label(vSnap.label())
                                            .attributes(Map.of(
                                                    "mrp", vSnap.mrp() != null ? vSnap.mrp() : BigDecimal.ZERO,
                                                    "sellingPrice", vSnap.sellingPrice(),
                                                    "inStock", vSnap.inStock(),
                                                    "discountPercent", vSnap.discountPercent()
                                            ))
                                            .build();
                                    variantRepository.save(newVar);
                                }

                                // Persist PriceSnapshot for historical tracking
                                try {
                                    priceSnapshotRepository.save(PriceSnapshot.builder()
                                            .ts(Instant.now())
                                            .meta(new SnapshotMeta(vSnap.skuId(), product.getId(), product.getSite()))
                                            .mrp(vSnap.mrp() != null ? vSnap.mrp() : BigDecimal.ZERO)
                                            .sellingPrice(vSnap.sellingPrice())
                                            .discountPercent(vSnap.discountPercent())
                                            .inStock(vSnap.inStock())
                                            .quantityHint(vSnap.quantityHint())
                                            .sourceStrategy(success.strategyUsed() != null ? success.strategyUsed() : "DIRECT_HTTP")
                                            .parserVersion(snap.parserVersion() != null ? snap.parserVersion() : "1.0.0")
                                            .build());
                                } catch (Exception ex) {
                                    log.warn("Failed to persist PriceSnapshot for variant {}: {}", vSnap.skuId(), ex.getMessage());
                                }
                            }
                        }
                        currentPrice = snap.variants().get(0).sellingPrice();
                    }
                }
            } catch (Exception e) {
                log.warn("Scraper error during background task execution: {}", e.getMessage());
            }

            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Scrape result yielded no valid current price for productId={}, skipping alert evaluation.", task.productId());
                return true;
            }

            for (Tracker tracker : trackers) {
                if (!tracker.isActive()) continue;

                BigDecimal targetVarPrice = currentPrice;
                Variant trackerVar = variantRepository.findByProductIdAndSiteSkuId(product.getId(), tracker.getVariantId()).orElse(null);
                if (trackerVar != null && trackerVar.getAttributes() != null) {
                    Object spObj = trackerVar.getAttributes().get("sellingPrice");
                    if (spObj != null) {
                        try {
                            targetVarPrice = new BigDecimal(String.valueOf(spObj));
                        } catch (Exception ignored) {}
                    }
                }

                BigDecimal previousVarPrice = oldPricesMap.getOrDefault(tracker.getVariantId(), targetVarPrice);

                if (targetVarPrice != null && targetVarPrice.compareTo(BigDecimal.ZERO) > 0) {
                    String titleWithLabel = product.getTitle();
                    if (trackerVar != null && trackerVar.getLabel() != null && !trackerVar.getLabel().isBlank() && !"Standard".equalsIgnoreCase(trackerVar.getLabel()) && !"Default".equalsIgnoreCase(trackerVar.getLabel())) {
                        titleWithLabel += " (" + trackerVar.getLabel() + ")";
                    }

                    notificationOrchestratorService.processPriceUpdate(
                            tracker,
                            titleWithLabel,
                            product.getCanonicalUrl(),
                            previousVarPrice,
                            targetVarPrice,
                            true,
                            true
                    );
                }
            }

            return true;
        } finally {
            scrapeLoopReconciler.markProductCompleted(task.productId());
        }
    }
}
