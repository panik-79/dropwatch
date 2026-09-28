package com.dropwatch.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "feature_flags")
public class FeatureFlagDoc {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private String description;

    public enum FlagType {
        BOOLEAN,
        PERCENTAGE,
        VARIANT,
        SCHEDULED
    }

    private FlagType type;
    private Object value;
    private List<String> allowList;
    private Double percentage;
    private boolean enabled = true;
    private String updatedBy;

    @LastModifiedDate
    private Instant updatedAt;

    public FeatureFlagDoc() {}

    public FeatureFlagDoc(String id, String key, String description, FlagType type, Object value, List<String> allowList, Double percentage, boolean enabled, String updatedBy, Instant updatedAt) {
        this.id = id;
        this.key = key;
        this.description = description;
        this.type = type;
        this.value = value;
        this.allowList = allowList;
        this.percentage = percentage;
        this.enabled = enabled;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String key;
        private String description;
        private FlagType type;
        private Object value;
        private List<String> allowList;
        private Double percentage;
        private boolean enabled = true;
        private String updatedBy;
        private Instant updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder type(FlagType type) { this.type = type; return this; }
        public Builder value(Object value) { this.value = value; return this; }
        public Builder allowList(List<String> allowList) { this.allowList = allowList; return this; }
        public Builder percentage(Double percentage) { this.percentage = percentage; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public FeatureFlagDoc build() {
            return new FeatureFlagDoc(id, key, description, type, value, allowList, percentage, enabled, updatedBy, updatedAt);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FlagType getType() { return type; }
    public void setType(FlagType type) { this.type = type; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public List<String> getAllowList() { return allowList; }
    public void setAllowList(List<String> allowList) { this.allowList = allowList; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureFlagDoc doc = (FeatureFlagDoc) o;
        return Objects.equals(id, doc.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
