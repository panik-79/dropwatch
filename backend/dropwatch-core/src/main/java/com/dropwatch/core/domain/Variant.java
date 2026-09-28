package com.dropwatch.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Objects;

@Document(collection = "variants")
@CompoundIndex(name = "product_sku_idx", def = "{'productId': 1, 'siteSkuId': 1}", unique = true)
public class Variant {

    @Id
    private String id;
    private String productId;
    private String label;
    private String siteSkuId;
    private Map<String, Object> attributes;
    private int schemaVersion = 1;

    @Version
    private Long version;

    public Variant() {}

    public Variant(String id, String productId, String label, String siteSkuId, Map<String, Object> attributes, int schemaVersion, Long version) {
        this.id = id;
        this.productId = productId;
        this.label = label;
        this.siteSkuId = siteSkuId;
        this.attributes = attributes;
        this.schemaVersion = schemaVersion;
        this.version = version;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private String productId;
        private String label;
        private String siteSkuId;
        private Map<String, Object> attributes;
        private int schemaVersion = 1;
        private Long version;

        public Builder id(String id) { this.id = id; return this; }
        public Builder productId(String productId) { this.productId = productId; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder siteSkuId(String siteSkuId) { this.siteSkuId = siteSkuId; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public Builder schemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public Variant build() {
            return new Variant(id, productId, label, siteSkuId, attributes, schemaVersion, version);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getSiteSkuId() { return siteSkuId; }
    public void setSiteSkuId(String siteSkuId) { this.siteSkuId = siteSkuId; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variant variant = (Variant) o;
        return Objects.equals(id, variant.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
