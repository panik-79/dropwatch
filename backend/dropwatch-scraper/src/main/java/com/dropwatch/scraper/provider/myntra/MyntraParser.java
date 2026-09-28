package com.dropwatch.scraper.provider.myntra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.spi.ProductSnapshot;
import com.dropwatch.scraper.spi.VariantSnapshot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyntraParser {

    private static final Logger log = LoggerFactory.getLogger(MyntraParser.class);
    private final ObjectMapper objectMapper;

    public MyntraParser() {
        this.objectMapper = new ObjectMapper();
    }

    public ProductSnapshot parse(String html, String canonicalUrl, String siteProductId) throws Exception {
        Document doc = Jsoup.parse(html, canonicalUrl);

        // Try window.__myx script first
        Elements scripts = doc.select("script");
        for (Element script : scripts) {
            String data = script.html();
            if (data.contains("window.__myx =") || data.contains("pdpData")) {
                try {
                    int jsonStart = data.indexOf('{');
                    int jsonEnd = data.lastIndexOf('}');
                    if (jsonStart != -1 && jsonEnd > jsonStart) {
                        String jsonStr = data.substring(jsonStart, jsonEnd + 1);
                        JsonNode root = objectMapper.readTree(jsonStr);
                        JsonNode pdpData = root.has("pdpData") ? root.get("pdpData") : root;
                        if (pdpData != null && pdpData.has("id")) {
                            return buildFromMyxJson(pdpData, siteProductId, canonicalUrl);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse __myx script, falling back to HTML/JSON-LD: {}", e.getMessage());
                }
            }
        }

        // Fallback to JSON-LD
        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts) {
            try {
                JsonNode root = objectMapper.readTree(script.html());
                if (root.has("@type") && "Product".equalsIgnoreCase(root.get("@type").asText())) {
                    return buildFromJsonLd(root, siteProductId, canonicalUrl, doc);
                }
            } catch (Exception ignored) {}
        }

        // Fallback to HTML meta & DOM elements
        return buildFromDom(doc, siteProductId, canonicalUrl);
    }

    private ProductSnapshot buildFromMyxJson(JsonNode pdpData, String siteProductId, String canonicalUrl) {
        String title = pdpData.has("name") ? pdpData.get("name").asText() : pdpData.path("title").asText("Myntra Product");
        String brand = pdpData.path("brand").path("name").asText(pdpData.path("brand").asText("Unknown Brand"));
        String category = pdpData.path("analytics").path("articleType").asText("Fashion");

        String imageUrl = "";
        if (pdpData.has("media") && pdpData.get("media").has("albums")) {
            JsonNode albums = pdpData.get("media").get("albums");
            if (albums.isArray() && !albums.isEmpty() && albums.get(0).has("images")) {
                JsonNode images = albums.get(0).get("images");
                if (images.isArray() && !images.isEmpty()) {
                    imageUrl = images.get(0).path("src").asText("");
                }
            }
        }

        BigDecimal baseMrp = BigDecimal.valueOf(pdpData.path("price").path("mrp").asDouble(pdpData.path("mrp").asDouble(0.0)));
        BigDecimal baseSellingPrice = BigDecimal.valueOf(pdpData.path("price").path("discounted").asDouble(pdpData.path("discountedPrice").asDouble(baseMrp.doubleValue())));

        List<VariantSnapshot> variants = new ArrayList<>();
        if (pdpData.has("sizes") && pdpData.get("sizes").isArray()) {
            for (JsonNode sizeNode : pdpData.get("sizes")) {
                String label = sizeNode.path("label").asText("Standard");
                String skuId = String.valueOf(sizeNode.path("skuId").asLong(0));
                boolean inStock = sizeNode.path("sellerData").path("outOfStock").asBoolean(false) == false
                        && sizeNode.path("available").asBoolean(true);

                BigDecimal vMrp = (sizeNode.has("mrp") && sizeNode.get("mrp").asDouble() > 0) ? BigDecimal.valueOf(sizeNode.get("mrp").asDouble()) : baseMrp;
                BigDecimal vPrice = (sizeNode.has("discountedPrice") && sizeNode.get("discountedPrice").asDouble() > 0) ? BigDecimal.valueOf(sizeNode.get("discountedPrice").asDouble()) : baseSellingPrice;
                if (vMrp.compareTo(vPrice) < 0) vMrp = vPrice;

                double discount = 0.0;
                if (vMrp.compareTo(BigDecimal.ZERO) > 0 && vMrp.compareTo(vPrice) > 0) {
                    discount = vMrp.subtract(vPrice).divide(vMrp, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
                }

                variants.add(new VariantSnapshot(
                        label,
                        skuId.equals("0") ? siteProductId + "-" + label : skuId,
                        vMrp,
                        vPrice,
                        Math.round(discount * 100.0) / 100.0,
                        inStock,
                        inStock ? 10 : 0
                ));
            }
        }

        if (variants.isEmpty()) {
            double discount = 0.0;
            if (baseMrp.compareTo(BigDecimal.ZERO) > 0) {
                discount = baseMrp.subtract(baseSellingPrice).divide(baseMrp, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
            }
            variants.add(new VariantSnapshot(
                    "Standard",
                    siteProductId,
                    baseMrp,
                    baseSellingPrice,
                    Math.round(discount * 100.0) / 100.0,
                    true,
                    10
            ));
        }

        return new ProductSnapshot(
                Site.MYNTRA,
                siteProductId,
                title,
                brand,
                imageUrl,
                category,
                canonicalUrl,
                variants,
                Map.of("extractedVia", "MYX_JSON"),
                "1.0.0"
        );
    }

    private ProductSnapshot buildFromJsonLd(JsonNode root, String siteProductId, String canonicalUrl, Document doc) {
        String title = root.path("name").asText("Myntra Product");
        String brand = root.path("brand").path("name").asText(root.path("brand").asText("Myntra"));
        String imageUrl = root.path("image").isArray() ? root.path("image").get(0).asText("") : root.path("image").asText("");

        JsonNode offers = root.path("offers");
        BigDecimal price = BigDecimal.valueOf(offers.path("price").asDouble(0.0));
        BigDecimal mrp = offers.has("priceValidUntil") ? price : price; // fallback
        boolean inStock = "http://schema.org/InStock".equalsIgnoreCase(offers.path("availability").asText());

        List<VariantSnapshot> variants = List.of(new VariantSnapshot(
                "Standard",
                siteProductId,
                mrp,
                price,
                0.0,
                inStock,
                inStock ? 1 : 0
        ));

        return new ProductSnapshot(
                Site.MYNTRA,
                siteProductId,
                title,
                brand,
                imageUrl,
                "Fashion",
                canonicalUrl,
                variants,
                Map.of("extractedVia", "JSON_LD"),
                "1.0.0"
        );
    }

    private ProductSnapshot buildFromDom(Document doc, String siteProductId, String canonicalUrl) {
        String title = doc.select("h1.pdp-title, h1.pdp-name, meta[property=og:title]").attr("content");
        if (title.isBlank()) title = doc.select("h1").text();
        if (title.isBlank()) title = "Myntra Item " + siteProductId;

        String brand = doc.select("h1.pdp-title").text();
        if (brand.isBlank()) brand = "Myntra";

        String imageUrl = doc.select("meta[property=og:image]").attr("content");

        String priceStr = doc.select(".pdp-price strong, .pdp-selling-price").text().replaceAll("[^0-9.]", "");
        BigDecimal price = priceStr.isBlank() ? BigDecimal.ZERO : new BigDecimal(priceStr);

        String mrpStr = doc.select(".pdp-mrp s, .pdp-mrp").text().replaceAll("[^0-9.]", "");
        BigDecimal mrp = mrpStr.isBlank() ? price : new BigDecimal(mrpStr);

        double discount = 0.0;
        if (mrp.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(BigDecimal.ZERO) > 0) {
            discount = mrp.subtract(price).divide(mrp, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        }

        VariantSnapshot variant = new VariantSnapshot(
                "Standard",
                siteProductId,
                mrp,
                price,
                Math.round(discount * 100.0) / 100.0,
                price.compareTo(BigDecimal.ZERO) > 0,
                1
        );

        return new ProductSnapshot(
                Site.MYNTRA,
                siteProductId,
                title,
                brand,
                imageUrl,
                "Fashion",
                canonicalUrl,
                List.of(variant),
                Map.of("extractedVia", "DOM_FALLBACK"),
                "1.0.0"
        );
    }
}
