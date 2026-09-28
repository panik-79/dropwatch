package com.dropwatch.scraper.provider.flipkart;

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
import java.util.List;
import java.util.Map;

public class FlipkartParser {

    private static final Logger log = LoggerFactory.getLogger(FlipkartParser.class);
    private final ObjectMapper objectMapper;

    public FlipkartParser() {
        this.objectMapper = new ObjectMapper();
    }

    public ProductSnapshot parse(String html, String canonicalUrl, String siteProductId) throws Exception {
        Document doc = Jsoup.parse(html, canonicalUrl);

        JsonNode productJsonLd = null;
        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts) {
            try {
                JsonNode root = objectMapper.readTree(script.html());
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        if (item.has("@type") && "Product".equalsIgnoreCase(item.get("@type").asText())) {
                            productJsonLd = item;
                            break;
                        }
                    }
                } else if (root.has("@type") && "Product".equalsIgnoreCase(root.get("@type").asText())) {
                    productJsonLd = root;
                    break;
                }
            } catch (Exception e) {
                log.debug("Failed to parse Flipkart JSON-LD script: {}", e.getMessage());
            }
        }

        String title = extractTitle(doc, productJsonLd);
        String brand = extractBrand(doc, productJsonLd, title);
        String imageUrl = extractImage(doc, productJsonLd);
        BigDecimal[] prices = extractPrices(doc, productJsonLd);
        BigDecimal mrp = prices[0];
        BigDecimal sellingPrice = prices[1];

        double discount = 0.0;
        if (mrp.compareTo(BigDecimal.ZERO) > 0 && sellingPrice.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(sellingPrice) > 0) {
            discount = mrp.subtract(sellingPrice).divide(mrp, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        }

        boolean outOfStock = doc.select("div._16FRp0, div._16bRSp:contains(Sold Out)").first() != null;
        boolean inStock = !outOfStock && sellingPrice.compareTo(BigDecimal.ZERO) > 0;

        // Extract dynamic variant label (size, color, specs) from page context
        String variantLabel = extractVariantLabel(doc, canonicalUrl);

        List<VariantSnapshot> variants = extractAllVariants(doc, canonicalUrl, siteProductId, mrp, sellingPrice, discount, inStock, variantLabel);

        return new ProductSnapshot(
                Site.FLIPKART,
                siteProductId != null ? siteProductId : "prod-" + Math.abs(canonicalUrl.hashCode()),
                title,
                brand,
                imageUrl,
                "Electronics & General",
                canonicalUrl,
                variants,
                Map.of("extractedVia", productJsonLd != null ? "JSON_LD" : "DOM_PARSER"),
                "1.0.0"
        );
    }

    private List<VariantSnapshot> extractAllVariants(Document doc, String canonicalUrl, String siteProductId, BigDecimal mrp, BigDecimal sellingPrice, double discount, boolean inStock, String primaryLabel) {
        List<VariantSnapshot> variants = new java.util.ArrayList<>();
        java.util.Set<String> addedSkus = new java.util.HashSet<>();

        String primarySku = siteProductId != null ? siteProductId : "sku-" + Math.abs(canonicalUrl.hashCode());
        variants.add(new VariantSnapshot(
                primaryLabel,
                primarySku,
                mrp,
                sellingPrice,
                Math.round(discount * 100.0) / 100.0,
                inStock,
                inStock ? 5 : 0
        ));
        addedSkus.add(primarySku.toLowerCase());

        // Extract DOM swatch/size links containing pid=
        Elements links = doc.select("a[href*='pid='], div[data-pid]");
        for (Element el : links) {
            String href = el.attr("href");
            String pid = null;
            if (href.contains("pid=")) {
                int idx = href.indexOf("pid=");
                pid = href.substring(idx + 4);
                int ampersand = pid.indexOf("&");
                if (ampersand != -1) pid = pid.substring(0, ampersand);
                pid = pid.trim();
            } else if (el.hasAttr("data-pid")) {
                pid = el.attr("data-pid").trim();
            }

            if (pid != null && !pid.isBlank() && !addedSkus.contains(pid.toLowerCase())) {
                String text = el.text().trim();
                if (!text.isBlank() && text.length() < 20
                        && !text.equalsIgnoreCase("Size Chart")
                        && !text.equalsIgnoreCase("Buy Now")
                        && !text.equalsIgnoreCase("Add to Cart")
                        && !text.toLowerCase().contains("out of stock")
                        && !text.toLowerCase().contains("flipkart")) {

                    addedSkus.add(pid.toLowerCase());
                    String label = text.matches("^[0-9]+(?:\\.[0-9]+)?$") ? "Size " + text : text;
                    variants.add(new VariantSnapshot(
                            label,
                            pid,
                            mrp,
                            sellingPrice,
                            Math.round(discount * 100.0) / 100.0,
                            true,
                            5
                    ));
                }
            }
        }

        return variants;
    }

    private String extractVariantLabel(Document doc, String canonicalUrl) {
        // 1. Check title parenthesized specs: e.g. "New Balance 530 Sneakers For Men (Beige, 4.5)"
        String pageTitle = doc.title();
        if (pageTitle != null && pageTitle.contains("(") && pageTitle.contains(")")) {
            int start = pageTitle.lastIndexOf("(");
            int end = pageTitle.indexOf(")", start);
            if (start != -1 && end > start) {
                String spec = pageTitle.substring(start + 1, end).trim();
                if (!spec.isBlank() && !spec.toLowerCase().contains("flipkart")) return spec;
            }
        }

        // 2. Check meta description for specs inside parentheses or after hyphens
        String desc = doc.select("meta[name=Description], meta[name=description], meta[property=og:description]").attr("content");
        if (!desc.isBlank() && desc.contains("(") && desc.contains(")")) {
            int start = desc.indexOf("(");
            int end = desc.indexOf(")", start);
            if (start != -1 && end > start) {
                String spec = desc.substring(start + 1, end).trim();
                if (!spec.isBlank() && spec.length() < 40) return spec;
            }
        }

        // 3. Selected pill or swatch in DOM
        Element selectedPill = doc.select("a._1fQflm._3O_Rko, a[class*='selected'], li[class*='selected'], div[class*='_3O_Rko'], [aria-selected='true']").first();
        if (selectedPill != null && !selectedPill.text().isBlank()) {
            return selectedPill.text().trim();
        }

        return "Standard";
    }

    private String extractTitle(Document doc, JsonNode jsonLd) {
        if (jsonLd != null && jsonLd.has("name") && !jsonLd.path("name").asText().isBlank()) {
            return cleanTitle(jsonLd.path("name").asText());
        }

        for (Element meta : doc.select("meta")) {
            String prop = meta.attr("property");
            String name = meta.attr("name");
            String content = meta.attr("content");
            if (("og:title".equalsIgnoreCase(prop) || "og:title".equalsIgnoreCase(name) || "twitter:title".equalsIgnoreCase(name) || "twitter:title".equalsIgnoreCase(prop))
                    && content != null && !content.isBlank()) {
                return cleanTitle(content);
            }
        }

        Element firstH1 = doc.select("h1._6ERy96, h1.VU-485, h1._2NdhYo, span.VU-485, span.B_Nu61").first();
        if (firstH1 != null && !firstH1.text().isBlank()) {
            return cleanTitle(firstH1.text());
        }

        String pageTitle = doc.title();
        if (!pageTitle.isBlank()) {
            return cleanTitle(pageTitle);
        }

        return "Flipkart Product";
    }

    private String cleanTitle(String raw) {
        if (raw == null) return "Flipkart Product";
        return raw.replaceAll("(?i)\\s*-\\s*Buy\\s+.*", "")
                  .replaceAll("(?i)\\s*\\|\\s*Flipkart.*", "")
                  .replaceAll("(?i)\\s*:\\s*Buy Online.*", "")
                  .trim();
    }

    private String extractBrand(Document doc, JsonNode jsonLd, String title) {
        if (jsonLd != null && jsonLd.has("brand")) {
            JsonNode b = jsonLd.get("brand");
            String bName = b.isObject() ? b.path("name").asText("") : b.asText("");
            if (!bName.isBlank() && !"Flipkart".equalsIgnoreCase(bName)) return bName.trim();
        }
        Element brandEl = doc.select("span.G62F8f, span._2Wk-jV, a._2whKao, span.VU-485, div._2Wk-jV").first();
        if (brandEl != null && !brandEl.text().isBlank()) {
            String bText = brandEl.text().trim();
            if (!bText.equalsIgnoreCase("Flipkart") && bText.length() < 30) return bText;
        }
        if (title != null && !title.isBlank()) {
            String[] words = title.split("\\s+");
            if (words.length >= 2 && (words[0].equalsIgnoreCase("New") || words[0].equalsIgnoreCase("Air") || words[0].equalsIgnoreCase("US") || words[0].equalsIgnoreCase("Puma") || words[0].equalsIgnoreCase("Nike") || words[0].equalsIgnoreCase("Adidas"))) {
                return words[0] + " " + words[1];
            }
            if (words.length > 0) return words[0];
        }
        return "Flipkart";
    }

    private String extractImage(Document doc, JsonNode jsonLd) {
        if (jsonLd != null && jsonLd.has("image")) {
            JsonNode img = jsonLd.get("image");
            if (img.isArray() && !img.isEmpty() && !img.get(0).asText().isBlank()) return img.get(0).asText();
            if (img.isTextual() && !img.asText().isBlank()) return img.asText();
        }

        for (Element meta : doc.select("meta")) {
            String prop = meta.attr("property");
            String name = meta.attr("name");
            String content = meta.attr("content");
            if (("og:image".equalsIgnoreCase(prop) || "og:image".equalsIgnoreCase(name) || "twitter:image".equalsIgnoreCase(name) || "twitter:image".equalsIgnoreCase(prop) || "og:image:url".equalsIgnoreCase(prop))
                    && content != null && !content.isBlank() && !content.contains("placeholder")) {
                return content.trim();
            }
        }

        Element linkEl = doc.select("link[rel=image_src]").first();
        if (linkEl != null && linkEl.hasAttr("href")) {
            String href = linkEl.attr("href").trim();
            if (!href.isBlank()) return href;
        }

        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            if (src == null || src.isBlank()) src = img.attr("data-src");
            if (src != null && (src.contains("rukminim") || src.contains("flixcart")) && !src.contains("placeholder") && !src.contains("svg")) {
                return src.trim();
            }
        }

        return "";
    }

    private BigDecimal[] extractPrices(Document doc, JsonNode jsonLd) {
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal mrp = BigDecimal.ZERO;

        // 1. Try JSON-LD
        if (jsonLd != null && jsonLd.has("offers")) {
            JsonNode offers = jsonLd.get("offers");
            if (offers.isArray() && !offers.isEmpty()) {
                offers = offers.get(0);
            }
            if (offers.has("price")) {
                double pVal = offers.path("price").asDouble(0.0);
                if (pVal > 0) {
                    price = BigDecimal.valueOf(pVal);
                }
            }
        }

        // 2. Try DOM price elements in primary product section
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            Element mainPriceContainer = doc.select("div._25b18c, div.C22v1I, div.xT3p5p, div.a1rA25").first();
            Element priceEl = null;
            if (mainPriceContainer != null) {
                priceEl = mainPriceContainer.select("div.Nx9bqj, div.C22v1I, div._30jeq3, span.Nx9bqj").first();
            }
            if (priceEl == null) {
                priceEl = doc.select("div.Nx9bqj, div.C22v1I, div._30jeq3, div._16bRSp, span.Nx9bqj").first();
            }

            if (priceEl != null) {
                String pStr = priceEl.text().replaceAll("[^0-9.]", "");
                if (!pStr.isBlank()) {
                    try {
                        price = new BigDecimal(pStr);
                    } catch (Exception ignored) {}
                }
            }
        }

        // 3. Fallback: Parse meta description
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            String desc = doc.select("meta[name=Description], meta[name=description], meta[property=og:description]").attr("content");
            if (!desc.isBlank()) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("only\\s+for\\s+Rs\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher m = p.matcher(desc);
                if (m.find()) {
                    String pStr = m.group(1).replaceAll(",", "");
                    try {
                        price = new BigDecimal(pStr);
                    } catch (Exception ignored) {}
                } else {
                    java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("Rs\\.?\\s*([0-9,]+(?:\\.[0-9]+)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
                    java.util.regex.Matcher m2 = p2.matcher(desc);
                    if (m2.find()) {
                        String pStr = m2.group(1).replaceAll(",", "");
                        try {
                            price = new BigDecimal(pStr);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        // Extract MRP
        Element mrpEl = doc.select("div.yRaYxF, div.yRaYxFA, div._3I9_wc, div._2p63I6, div[class*='yRaYx'], div[class*='_3I9_wc']").first();
        if (mrpEl != null) {
            String mStr = mrpEl.text().replaceAll("[^0-9.]", "");
            if (!mStr.isBlank()) {
                try {
                    mrp = new BigDecimal(mStr);
                } catch (Exception ignored) {}
            }
        }

        if (mrp.compareTo(BigDecimal.ZERO) <= 0 || mrp.compareTo(price) <= 0) {
            java.util.regex.Pattern mrpRegex = java.util.regex.Pattern.compile("\"mrp\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
            java.util.regex.Matcher mrpMatcher = mrpRegex.matcher(doc.html());
            if (mrpMatcher.find()) {
                try {
                    BigDecimal parsedMrp = new BigDecimal(mrpMatcher.group(1));
                    if (parsedMrp.compareTo(price) >= 0) {
                        mrp = parsedMrp;
                    }
                } catch (Exception ignored) {}
            }
        }
        if (mrp.compareTo(BigDecimal.ZERO) <= 0 || mrp.compareTo(price) < 0) mrp = price;

        return new BigDecimal[]{mrp, price};
    }
}
