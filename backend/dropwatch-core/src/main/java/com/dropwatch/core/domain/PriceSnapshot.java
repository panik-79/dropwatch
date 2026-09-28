package com.dropwatch.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Document(collection = "price_snapshots")
@TimeSeries(timeField = "ts", metaField = "meta")
public class PriceSnapshot {

    @Id
    private String id;
    private Instant ts;
    private SnapshotMeta meta;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private double discountPercent;
    private boolean inStock;
    private Integer quantityHint;
    private String sourceStrategy;
    private String parserVersion;

    public PriceSnapshot() {}

    public PriceSnapshot(String id, Instant ts, SnapshotMeta meta, BigDecimal mrp, BigDecimal sellingPrice, double discountPercent, boolean inStock, Integer quantityHint, String sourceStrategy, String parserVersion) {
        this.id = id;
        this.ts = ts;
        this.meta = meta;
        this.mrp = mrp;
        this.sellingPrice = sellingPrice;
        this.discountPercent = discountPercent;
        this.inStock = inStock;
        this.quantityHint = quantityHint;
        this.sourceStrategy = sourceStrategy;
        this.parserVersion = parserVersion;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private Instant ts;
        private SnapshotMeta meta;
        private BigDecimal mrp;
        private BigDecimal sellingPrice;
        private double discountPercent;
        private boolean inStock;
        private Integer quantityHint;
        private String sourceStrategy;
        private String parserVersion;

        public Builder id(String id) { this.id = id; return this; }
        public Builder ts(Instant ts) { this.ts = ts; return this; }
        public Builder meta(SnapshotMeta meta) { this.meta = meta; return this; }
        public Builder mrp(BigDecimal mrp) { this.mrp = mrp; return this; }
        public Builder sellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; return this; }
        public Builder discountPercent(double discountPercent) { this.discountPercent = discountPercent; return this; }
        public Builder inStock(boolean inStock) { this.inStock = inStock; return this; }
        public Builder quantityHint(Integer quantityHint) { this.quantityHint = quantityHint; return this; }
        public Builder sourceStrategy(String sourceStrategy) { this.sourceStrategy = sourceStrategy; return this; }
        public Builder parserVersion(String parserVersion) { this.parserVersion = parserVersion; return this; }

        public PriceSnapshot build() {
            return new PriceSnapshot(id, ts, meta, mrp, sellingPrice, discountPercent, inStock, quantityHint, sourceStrategy, parserVersion);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public SnapshotMeta getMeta() { return meta; }
    public void setMeta(SnapshotMeta meta) { this.meta = meta; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public Integer getQuantityHint() { return quantityHint; }
    public void setQuantityHint(Integer quantityHint) { this.quantityHint = quantityHint; }
    public String getSourceStrategy() { return sourceStrategy; }
    public void setSourceStrategy(String sourceStrategy) { this.sourceStrategy = sourceStrategy; }
    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceSnapshot snapshot = (PriceSnapshot) o;
        return Objects.equals(id, snapshot.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
