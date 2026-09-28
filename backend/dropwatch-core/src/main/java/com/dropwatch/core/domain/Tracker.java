package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "trackers")
public class Tracker {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String productId;

    @Indexed
    private String variantId;

    private List<TrackerRule> rules = new ArrayList<>();
    private boolean active = true;
    private int pollIntervalSeconds = 3600;
    private TrackerPriority priority = TrackerPriority.NORMAL;
    private List<String> channelIds = new ArrayList<>();
    private long cooldownSeconds = 3600;
    private Instant lastAlertAt;
    private Instant pausedUntil;

    @CreatedDate
    private Instant createdAt;
    private int schemaVersion = 1;

    @Version
    private Long version;

    public Tracker() {}

    public Tracker(String id, String userId, String productId, String variantId, List<TrackerRule> rules, boolean active, int pollIntervalSeconds, TrackerPriority priority, List<String> channelIds, long cooldownSeconds, Instant lastAlertAt, Instant pausedUntil, Instant createdAt, int schemaVersion, Long version) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.variantId = variantId;
        this.rules = rules != null ? rules : new ArrayList<>();
        this.active = active;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.priority = priority != null ? priority : TrackerPriority.NORMAL;
        this.channelIds = channelIds != null ? channelIds : new ArrayList<>();
        this.cooldownSeconds = cooldownSeconds;
        this.lastAlertAt = lastAlertAt;
        this.pausedUntil = pausedUntil;
        this.createdAt = createdAt;
        this.schemaVersion = schemaVersion;
        this.version = version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String userId;
        private String productId;
        private String variantId;
        private List<TrackerRule> rules = new ArrayList<>();
        private boolean active = true;
        private int pollIntervalSeconds = 3600;
        private TrackerPriority priority = TrackerPriority.NORMAL;
        private List<String> channelIds = new ArrayList<>();
        private long cooldownSeconds = 3600;
        private Instant lastAlertAt;
        private Instant pausedUntil;
        private Instant createdAt;
        private int schemaVersion = 1;
        private Long version;

        public Builder id(String id) { this.id = id; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder variantId(String variantId) { this.variantId = variantId; return this; }
        public Builder rules(List<TrackerRule> rules) { this.rules = rules; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder pollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; return this; }
        public Builder priority(TrackerPriority priority) { this.priority = priority; return this; }
        public Builder channelIds(List<String> channelIds) { this.channelIds = channelIds; return this; }
        public Builder cooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; return this; }
        public Builder lastAlertAt(Instant lastAlertAt) { this.lastAlertAt = lastAlertAt; return this; }
        public Builder pausedUntil(Instant pausedUntil) { this.pausedUntil = pausedUntil; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder schemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public Tracker build() {
            return new Tracker(id, userId, productId, variantId, rules, active, pollIntervalSeconds, priority, channelIds, cooldownSeconds, lastAlertAt, pausedUntil, createdAt, schemaVersion, version);
        }
    }

    public boolean isPaused() {
        if (!active) return true;
        if (pausedUntil == null) return false;
        return Instant.now().isBefore(pausedUntil);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getVariantId() { return variantId; }
    public void setVariantId(String variantId) { this.variantId = variantId; }
    public List<TrackerRule> getRules() { return rules; }
    public void setRules(List<TrackerRule> rules) { this.rules = rules; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
    public TrackerPriority getPriority() { return priority; }
    public void setPriority(TrackerPriority priority) { this.priority = priority; }
    public List<String> getChannelIds() { return channelIds; }
    public void setChannelIds(List<String> channelIds) { this.channelIds = channelIds; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public Instant getLastAlertAt() { return lastAlertAt; }
    public void setLastAlertAt(Instant lastAlertAt) { this.lastAlertAt = lastAlertAt; }
    public Instant getPausedUntil() { return pausedUntil; }
    public void setPausedUntil(Instant pausedUntil) { this.pausedUntil = pausedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tracker tracker = (Tracker) o;
        return Objects.equals(id, tracker.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
