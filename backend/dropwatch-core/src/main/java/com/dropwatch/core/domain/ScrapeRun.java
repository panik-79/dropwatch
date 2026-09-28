package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

@Document(collection = "scrape_runs")
public class ScrapeRun {

    @Id
    private String id;

    @Indexed
    private String host;

    @Indexed
    private String productId;

    private long latencyMs;
    private int statusCode;
    private String status;
    private String strategy;
    private String errorClass;
    private String errorMessage;

    @CreatedDate
    @Indexed
    private Instant timestamp;

    public ScrapeRun() {}

    public ScrapeRun(String id, String host, String productId, long latencyMs, int statusCode, String status, String strategy, String errorClass, String errorMessage, Instant timestamp) {
        this.id = id;
        this.host = host;
        this.productId = productId;
        this.latencyMs = latencyMs;
        this.statusCode = statusCode;
        this.status = status;
        this.strategy = strategy;
        this.errorClass = errorClass;
        this.errorMessage = errorMessage;
        this.timestamp = timestamp;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String host;
        private String productId;
        private long latencyMs;
        private int statusCode;
        private String status;
        private String strategy;
        private String errorClass;
        private String errorMessage;
        private Instant timestamp;

        public Builder id(String id) { this.id = id; return this; }
        public Builder host(String host) { this.host = host; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder latencyMs(long latencyMs) { this.latencyMs = latencyMs; return this; }
        public Builder statusCode(int statusCode) { this.statusCode = statusCode; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder strategy(String strategy) { this.strategy = strategy; return this; }
        public Builder errorClass(String errorClass) { this.errorClass = errorClass; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public ScrapeRun build() {
            return new ScrapeRun(id, host, productId, latencyMs, statusCode, status, strategy, errorClass, errorMessage, timestamp);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public String getErrorClass() { return errorClass; }
    public void setErrorClass(String errorClass) { this.errorClass = errorClass; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScrapeRun scrapeRun = (ScrapeRun) o;
        return Objects.equals(id, scrapeRun.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
