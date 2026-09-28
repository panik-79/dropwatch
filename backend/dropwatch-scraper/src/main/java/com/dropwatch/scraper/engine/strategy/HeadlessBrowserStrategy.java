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

/**
 * Headless Browser fallback strategy.
 * Executes full rendering requests when static HTTP gets blocked.
 */
@Component
public class HeadlessBrowserStrategy implements FetchStrategy {

    public static final String NAME = "HEADLESS_BROWSER";

    private final HttpClient httpClient;

    public HeadlessBrowserStrategy() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(20))
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
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Linux\"")
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
