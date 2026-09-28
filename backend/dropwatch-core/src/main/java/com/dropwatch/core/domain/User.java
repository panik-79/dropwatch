package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String displayName;

    private String passwordHash;

    private String telegramChatId;

    private UserPreferences preferences = UserPreferences.defaultPreferences();

    private Set<String> roles = Set.of("ROLE_USER");

    @CreatedDate
    private Instant createdAt;

    private int schemaVersion = 1;

    @Version
    private Long version;

    public User() {}

    public User(String id, String email, String displayName, String passwordHash, String telegramChatId, UserPreferences preferences, Set<String> roles, Instant createdAt, int schemaVersion, Long version) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.telegramChatId = telegramChatId;
        this.preferences = preferences != null ? preferences : UserPreferences.defaultPreferences();
        this.roles = roles != null ? roles : Set.of("ROLE_USER");
        this.createdAt = createdAt;
        this.schemaVersion = schemaVersion;
        this.version = version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String email;
        private String displayName;
        private String passwordHash;
        private String telegramChatId;
        private UserPreferences preferences = UserPreferences.defaultPreferences();
        private Set<String> roles = Set.of("ROLE_USER");
        private Instant createdAt;
        private int schemaVersion = 1;
        private Long version;

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder telegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; return this; }
        public Builder preferences(UserPreferences preferences) { this.preferences = preferences; return this; }
        public Builder roles(Set<String> roles) { this.roles = roles; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder schemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public User build() {
            return new User(id, email, displayName, passwordHash, telegramChatId, preferences, roles, createdAt, schemaVersion, version);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }
    public UserPreferences getPreferences() { return preferences; }
    public void setPreferences(UserPreferences preferences) { this.preferences = preferences; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
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
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
