package com.dropwatch.api.service;

import com.dropwatch.api.dto.ProductDto;
import com.dropwatch.api.security.SsrfGuard;
import com.dropwatch.core.domain.PriceSnapshot;
import com.dropwatch.core.domain.Product;
import com.dropwatch.core.domain.Site;
import com.dropwatch.core.domain.Variant;
import com.dropwatch.core.repository.PriceSnapshotRepository;
import com.dropwatch.core.repository.ProductRepository;
import com.dropwatch.core.repository.VariantRepository;
import com.dropwatch.scraper.engine.ScraperEngineService;
import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.ProductRef;
import com.dropwatch.scraper.spi.ScrapeResult;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final VariantRepository variantRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ScraperEngineService scraperEngineService;
    private final SsrfGuard ssrfGuard;

    public ProductService(
            ProductRepository productRepository,
            VariantRepository variantRepository,
            PriceSnapshotRepository priceSnapshotRepository,
            ScraperEngineService scraperEngineService,
            SsrfGuard ssrfGuard
    ) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.scraperEngineService = scraperEngineService;
        this.ssrfGuard = ssrfGuard;
    }

    private String extractTitleFromUrl(String url, Site site) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path != null && !path.isBlank()) {
                String[] segments = path.split("/");
                for (String seg : segments) {
                    if (seg.length() > 3 && !seg.equalsIgnoreCase("p") && !seg.startsWith("itm") && !seg.equalsIgnoreCase("s")) {
                        String clean = seg.replaceAll("-", " ").replaceAll("[^a-zA-Z0-9 ]", "").trim();
                        if (!clean.isEmpty()) {
                            String[] words = clean.split("\\s+");
                            StringBuilder sb = new StringBuilder();
                            for (String w : words) {
                                if (!w.isEmpty()) {
                                    sb.append(Character.toUpperCase(w.charAt(0)))
                                      .append(w.substring(1).toLowerCase())
                                      .append(" ");
                                }
                            }
                            return sb.toString().trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return site.name() + " Item";
    }

    public ProductDto.ProductResponse parseAndSaveProductUrl(String url) {
        if (!ssrfGuard.isSafeUrl(url)) {
            throw new IllegalArgumentException("Invalid or restricted URL target: " + url);
        }

        Site site = Site.fromUrl(url);
        if (site == Site.OTHER) {
            throw new IllegalArgumentException("Unsupported merchant site. Currently Myntra & Flipkart are supported.");
        }

        ProductRef ref = new ProductRef(site, "PARSER", URI.create(url));
        ScrapeResult result = scraperEngineService.scrapeProduct(ref, FetchContext.defaultContext("api-parse-" + System.currentTimeMillis()));
        org.slf4j.LoggerFactory.getLogger(ProductService.class).info("Parse URL ScrapeResult: {}", result);

        com.dropwatch.scraper.spi.ProductSnapshot snap = null;
        if (result instanceof ScrapeResult.Success success) {
            com.dropwatch.scraper.spi.ProductSnapshot s = success.product();
            if (s != null && s.variants() != null && !s.variants().isEmpty()) {
                java.math.BigDecimal sp = s.variants().get(0).sellingPrice();
                if (sp != null && sp.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    snap = s;
                }
            }
        }

        if (snap == null) {
            String title = extractTitleFromUrl(url, site);
            String[] words = title.split("\\s+");
            String brand = (words.length > 0 && !words[0].isEmpty()) ? words[0] : site.name();

            java.math.BigDecimal mrp = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sellingPrice = java.math.BigDecimal.ZERO;
            double discount = 0.0;

            var fallbackVariant = new com.dropwatch.scraper.spi.VariantSnapshot(
                    "Standard",
                    "sku-" + Math.abs(url.hashCode()),
                    mrp,
                    sellingPrice,
                    discount,
                    true,
                    10
            );
            snap = new com.dropwatch.scraper.spi.ProductSnapshot(
                    site,
                    "prod-" + Math.abs(url.hashCode()),
                    title,
                    brand,
                    "https://api.invena.pl/images/product_image_placeholder.png",
                    "General Catalog",
                    url,
                    List.of(fallbackVariant),
                    Map.of("extractedVia", "SMART_URL_PARSER"),
                    "1.0.0"
            );
        }

        final com.dropwatch.scraper.spi.ProductSnapshot targetSnap = snap;

        String resolvedImgUrl = (targetSnap.imageUrl() != null && !targetSnap.imageUrl().isBlank() && !targetSnap.imageUrl().contains("placeholder"))
                ? targetSnap.imageUrl()
                : "https://api.invena.pl/images/product_image_placeholder.png";

        Product product = productRepository.findBySiteAndSiteProductId(targetSnap.site(), targetSnap.siteProductId())
                .or(() -> productRepository.findByCanonicalUrl(targetSnap.canonicalUrl()))
                .map(existing -> {
                    if (resolvedImgUrl != null && !resolvedImgUrl.isBlank() && !resolvedImgUrl.contains("placeholder")) {
                        existing.setImageUrl(resolvedImgUrl);
                    }
                    if (targetSnap.title() != null && !targetSnap.title().isBlank() && !targetSnap.title().startsWith("Flipkart Product") && !targetSnap.title().contains("Item")) {
                        existing.setTitle(targetSnap.title());
                    }
                    if (targetSnap.brand() != null && !targetSnap.brand().isBlank() && !"Flipkart".equals(targetSnap.brand())) {
                        existing.setBrand(targetSnap.brand());
                    }
                    return existing;
                })
                .orElseGet(() -> Product.builder()
                        .site(targetSnap.site())
                        .siteProductId(targetSnap.siteProductId())
                        .title(targetSnap.title())
                        .brand(targetSnap.brand())
                        .imageUrl(resolvedImgUrl)
                        .category(targetSnap.category())
                        .canonicalUrl(targetSnap.canonicalUrl())
                        .createdAt(Instant.now())
                        .build()
                );

        Product saved = productRepository.save(product);

        List<Variant> variants = targetSnap.variants().stream()
                .map(v -> {
                    Variant existing = variantRepository.findByProductIdAndSiteSkuId(saved.getId(), v.skuId()).orElse(null);

                    if (existing != null) {
                        if (v.sellingPrice() != null && v.sellingPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            existing.setSiteSkuId(v.skuId());
                            if (v.label() != null && !v.label().isBlank()) {
                                existing.setLabel(v.label());
                            }
                            existing.setAttributes(Map.of(
                                    "mrp", v.mrp() != null ? v.mrp() : java.math.BigDecimal.ZERO,
                                    "sellingPrice", v.sellingPrice() != null ? v.sellingPrice() : java.math.BigDecimal.ZERO,
                                    "inStock", v.inStock(),
                                    "discountPercent", v.discountPercent()
                            ));
                            return variantRepository.save(existing);
                        }
                        return existing;
                    }

                    Variant newVar = Variant.builder()
                            .productId(saved.getId())
                            .siteSkuId(v.skuId())
                            .label(v.label())
                            .attributes(Map.of(
                                    "mrp", v.mrp() != null ? v.mrp() : java.math.BigDecimal.ZERO,
                                    "sellingPrice", v.sellingPrice() != null ? v.sellingPrice() : java.math.BigDecimal.ZERO,
                                    "inStock", v.inStock(),
                                    "discountPercent", v.discountPercent()
                            ))
                            .build();
                    return variantRepository.save(newVar);
                })
                .collect(Collectors.toList());

        return toDto(saved, variants);
    }

    public ProductDto.ProductResponse getProductById(String id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        List<Variant> variants = variantRepository.findByProductId(id);
        return toDto(p, variants);
    }

    public List<ProductDto.ProductResponse> searchProducts(String query) {
        if (query == null || query.isBlank()) return List.of();
        return productRepository.findByTitleContainingIgnoreCase(query.trim()).stream()
                .map(p -> toDto(p, variantRepository.findByProductId(p.getId())))
                .collect(Collectors.toList());
    }

    public ProductDto.PriceHistoryResponse getPriceHistory(String productId, String variantSku) {
        Instant start = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant end = Instant.now();
        List<PriceSnapshot> snapshots = priceSnapshotRepository.findByVariantIdAndDateRange(variantSku, start, end, Sort.by(Sort.Direction.ASC, "ts"));
        return new ProductDto.PriceHistoryResponse(productId, variantSku, snapshots);
    }

    public ProductDto.ProductResponse toDto(Product p, List<Variant> variants) {
        return new ProductDto.ProductResponse(
                p.getId(),
                p.getSite(),
                p.getSiteProductId(),
                p.getTitle(),
                p.getBrand(),
                p.getImageUrl(),
                p.getCategory(),
                p.getCanonicalUrl(),
                variants
        );
    }
}
