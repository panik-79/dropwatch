package com.dropwatch.core.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Document(collection = "products")
@CompoundIndex(name = "site_product_idx", def = "{'site': 1, 'siteProductId': 1}", unique = true)
public class Product {

    @Id
    private String id;
    private Site site;
    private String canonicalUrl;
    private String siteProductId;
    private String title;
    private String brand;
    private String imageUrl;
    private String category;
    private Instant lastFetchedAt;
    private Map<String, Object> attributes;

    @CreatedDate
    private Instant createdAt;
    private int schemaVersion = 1;

    @Version
    private Long version;

    public Product() {}

    public Product(String id, Site site, String canonicalUrl, String siteProductId, String title, String brand, String imageUrl, String category, Instant lastFetchedAt, Map<String, Object> attributes, Instant createdAt, int schemaVersion, Long version) {
        this.id = id;
        this.site = site;
        this.canonicalUrl = canonicalUrl;
        this.siteProductId = siteProductId;
        this.title = title;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.category = category;
        this.lastFetchedAt = lastFetchedAt;
        this.attributes = attributes;
        this.createdAt = createdAt;
        this.schemaVersion = schemaVersion;
        this.version = version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private Site site;
        private String canonicalUrl;
        private String siteProductId;
        private String title;
        private String brand;
        private String imageUrl;
        private String category;
        private Instant lastFetchedAt;
        private Map<String, Object> attributes;
        private Instant createdAt;
        private int schemaVersion = 1;
        private Long version;

        public Builder id(String id) { this.id = id; return this; }
        public Builder site(Site site) { this.site = site; return this; }
        public Builder canonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; return this; }
        public Builder siteProductId(String siteProductId) { this.siteProductId = siteProductId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder lastFetchedAt(Instant lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder schemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public Product build() {
            return new Product(id, site, canonicalUrl, siteProductId, title, brand, imageUrl, category, lastFetchedAt, attributes, createdAt, schemaVersion, version);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public String getSiteProductId() { return siteProductId; }
    public void setSiteProductId(String siteProductId) { this.siteProductId = siteProductId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Instant getLastFetchedAt() { return lastFetchedAt; }
    public void setLastFetchedAt(Instant lastFetchedAt) { this.lastFetchedAt = lastFetchedAt; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
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
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
