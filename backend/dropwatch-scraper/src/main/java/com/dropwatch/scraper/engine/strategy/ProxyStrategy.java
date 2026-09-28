package com.dropwatch.scraper.engine.strategy;

import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.FetchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetch strategy that routes requests through ScraperAPI, a residential proxy
 * service that bypasses IP-based bot detection on e-commerce sites like Myntra.
 *
 * <p>When {@code SCRAPER_API_KEY} env var is set, this strategy is active and
 * provides anti-bot bypass. When unset, {@link #isAvailable()} returns false
 * and the engine falls through to other strategies.
 *
 * <p>Free tier: 1,000 requests/month — sufficient for personal use.
 * Sign up at: https://www.scraperapi.com (no credit card required)
 */
@Component
public class ProxyStrategy implements FetchStrategy {

    public static final String NAME = "PROXY_ROTATION";
    private static final Logger log = LoggerFactory.getLogger(ProxyStrategy.class);

    private static final String SCRAPER_API_ENDPOINT = "https://api.scraperapi.com/";
    private static final int TIMEOUT_SECONDS = 60; // ScraperAPI needs up to 60s for heavy sites

    private final String scraperApiKey;
    private final HttpClient httpClient;

    public ProxyStrategy(@Value("${scraper.proxy.api-key:}") String scraperApiKey) {
        this.scraperApiKey = scraperApiKey != null ? scraperApiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }

    /**
     * Only available when SCRAPER_API_KEY is configured.
     * Without a key this strategy is a no-op, so the engine skips it.
     */
    @Override
    public boolean isAvailable() {
        return !scraperApiKey.isEmpty();
    }

    @Override
    public FetchResponse fetch(String url, FetchContext context) throws Exception {
        // Build ScraperAPI request URL with residential proxy and India geo-targeting
        String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
        String apiUrl = SCRAPER_API_ENDPOINT
                + "?api_key=" + scraperApiKey
                + "&url=" + encodedUrl
                + "&country_code=in"        // India residential IP
                + "&render=false"           // HTML only, no JS rendering (faster + cheaper)
                + "&keep_headers=true";     // Forward our browser headers

        log.debug("[ProxyStrategy] Routing through ScraperAPI for: {}", url);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-IN,en;q=0.9")
                .GET();

        if (context != null && context.customHeaders() != null) {
            context.customHeaders().forEach(builder::header);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        // ScraperAPI returns the original site's status code in the X-Scraperapi-Status header
        // The HTTP response itself is always 200 if ScraperAPI succeeded
        Map<String, String> responseHeaders = new HashMap<>();
        response.headers().map().forEach((k, v) -> responseHeaders.put(k, String.join(",", v)));

        int effectiveStatus = response.statusCode();
        // Check if ScraperAPI reported the original status
        String scraperStatus = responseHeaders.get("x-scraperapi-response-statuscode");
        if (scraperStatus != null && !scraperStatus.isBlank()) {
            try {
                effectiveStatus = Integer.parseInt(scraperStatus.trim());
            } catch (NumberFormatException ignored) {}
        }

        log.info("[ProxyStrategy] ScraperAPI fetch complete for {} — HTTP {} (body: {} chars)",
                url, effectiveStatus, response.body() != null ? response.body().length() : 0);

        return new FetchResponse(effectiveStatus, response.body(), responseHeaders, NAME);
    }
}
