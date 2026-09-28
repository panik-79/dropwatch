package com.dropwatch.scraper.engine.strategy;

import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.FetchStrategy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProxyStrategy implements FetchStrategy {

    public static final String NAME = "PROXY_ROTATION";

    private final HttpClient httpClient;

    public ProxyStrategy() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String getStrategyName() {
        return NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public FetchResponse fetch(String url, FetchContext context) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-IN,en-GB;q=0.9,en;q=0.8")
                .header("X-Forwarded-For", "103.21.125." + (int)(Math.random() * 250 + 1))
                .GET();

        if (context.customHeaders() != null) {
            context.customHeaders().forEach(builder::header);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        Map<String, String> responseHeaders = new HashMap<>();
        response.headers().map().forEach((k, v) -> responseHeaders.put(k, String.join(",", v)));

        return new FetchResponse(
                response.statusCode(),
                response.body(),
                responseHeaders,
                NAME
        );
    }
}
