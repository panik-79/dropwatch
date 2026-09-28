package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Document(collection = "alert_events")
public class AlertEvent {

    @Id
    private String id;

    @Indexed
    private String trackerId;
    private String ruleId;
    private String ruleType;
    private BigDecimal priceAtTrigger;
    private String channel;

    public enum DeliveryStatus {
        PENDING,
        DELIVERED,
        FAILED
    }

    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;
    private Instant deliveredAt;

    @Indexed(unique = true)
    private String dedupeKey;

    @CreatedDate
    private Instant createdAt;

    public AlertEvent() {}

    public AlertEvent(String id, String trackerId, String ruleId, String ruleType, BigDecimal priceAtTrigger, String channel, DeliveryStatus deliveryStatus, Instant deliveredAt, String dedupeKey, Instant createdAt) {
        this.id = id;
        this.trackerId = trackerId;
        this.ruleId = ruleId;
        this.ruleType = ruleType;
        this.priceAtTrigger = priceAtTrigger;
        this.channel = channel;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : DeliveryStatus.PENDING;
        this.deliveredAt = deliveredAt;
        this.dedupeKey = dedupeKey;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String trackerId;
        private String ruleId;
        private String ruleType;
        private BigDecimal priceAtTrigger;
        private String channel;
        private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;
        private Instant deliveredAt;
        private String dedupeKey;
        private Instant createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder trackerId(String trackerId) { this.trackerId = trackerId; return this; }
        public Builder ruleId(String ruleId) { this.ruleId = ruleId; return this; }
        public Builder ruleType(String ruleType) { this.ruleType = ruleType; return this; }
        public Builder priceAtTrigger(BigDecimal priceAtTrigger) { this.priceAtTrigger = priceAtTrigger; return this; }
        public Builder channel(String channel) { this.channel = channel; return this; }
        public Builder deliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; return this; }
        public Builder deliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; return this; }
        public Builder dedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AlertEvent build() {
            return new AlertEvent(id, trackerId, ruleId, ruleType, priceAtTrigger, channel, deliveryStatus, deliveredAt, dedupeKey, createdAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTrackerId() { return trackerId; }
    public void setTrackerId(String trackerId) { this.trackerId = trackerId; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public BigDecimal getPriceAtTrigger() { return priceAtTrigger; }
    public void setPriceAtTrigger(BigDecimal priceAtTrigger) { this.priceAtTrigger = priceAtTrigger; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertEvent alertEvent = (AlertEvent) o;
        return Objects.equals(id, alertEvent.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
