package com.dropwatch.scraper.engine.strategy;

import com.dropwatch.scraper.spi.FetchContext;
import com.dropwatch.scraper.spi.FetchResponse;
import com.dropwatch.scraper.spi.FetchStrategy;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DirectHttpStrategy implements FetchStrategy {

    public static final String NAME = "DIRECT_HTTP";
    private static final Logger log = LoggerFactory.getLogger(DirectHttpStrategy.class);

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
        boolean isInteractive = context != null && context.traceId() != null && context.traceId().startsWith("api-parse-");
        int timeoutMs = (isInteractive ? 8 : 12) * 1000;

        try {
            boolean isFlipkart = url != null && (url.contains("flipkart.com") || url.contains("fkrt.it"));
            String userAgent = isFlipkart
                    ? "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
                    : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

            Connection conn = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-IN,en;q=0.9")
                    .header("Accept-Encoding", "gzip, deflate")
                    .timeout(timeoutMs)
                    .followRedirects(true)
                    .ignoreHttpErrors(true);

            if (!isFlipkart) {
                conn.header("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Google Chrome\";v=\"128\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1");
            }

            if (context != null && context.customHeaders() != null) {
                conn.headers(context.customHeaders());
            }

            Connection.Response response = conn.execute();
            Map<String, String> responseHeaders = new HashMap<>(response.headers());

            return new FetchResponse(
                    response.statusCode(),
                    response.body(),
                    responseHeaders,
                    NAME
            );
        } catch (Exception e) {
            log.warn("DirectHttpStrategy fetch failed for URL {}: {}", url, e.getMessage());
            return new FetchResponse(500, "", Map.of(), NAME);
        }
    }
}
