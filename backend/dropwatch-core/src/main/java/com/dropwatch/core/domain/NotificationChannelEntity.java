package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Document(collection = "notification_channels")
public class NotificationChannelEntity {

    @Id
    private String id;

    @Indexed
    private String userId;

    public enum ChannelType {
        TELEGRAM,
        EMAIL,
        DISCORD,
        WEBHOOK
    }

    private ChannelType type;
    private Map<String, String> config;
    private boolean verified = false;
    private boolean enabled = true;

    @CreatedDate
    private Instant createdAt;

    @Version
    private Long version;

    public NotificationChannelEntity() {}

    public NotificationChannelEntity(String id, String userId, ChannelType type, Map<String, String> config, boolean verified, boolean enabled, Instant createdAt, Long version) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.config = config;
        this.verified = verified;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.version = version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String userId;
        private ChannelType type;
        private Map<String, String> config;
        private boolean verified = false;
        private boolean enabled = true;
        private Instant createdAt;
        private Long version;

        public Builder id(String id) { this.id = id; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder type(ChannelType type) { this.type = type; return this; }
        public Builder config(Map<String, String> config) { this.config = config; return this; }
        public Builder verified(boolean verified) { this.verified = verified; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public NotificationChannelEntity build() {
            return new NotificationChannelEntity(id, userId, type, config, verified, enabled, createdAt, version);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public ChannelType getType() { return type; }
    public void setType(ChannelType type) { this.type = type; }
    public Map<String, String> getConfig() { return config; }
    public void setConfig(Map<String, String> config) { this.config = config; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationChannelEntity entity = (NotificationChannelEntity) o;
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
