// DropWatch MongoDB Initialization Script
db = db.getSiblingDB('dropwatch');

// Create native time-series collection for price_snapshots if not exists
try {
  db.createCollection('price_snapshots', {
    timeseries: {
      timeField: 'ts',
      metaField: 'meta',
      granularity: 'minutes'
    },
    expireAfterSeconds: 31536000 // 365 days retention TTL
  });
  print('Created time-series collection: price_snapshots');
} catch (e) {
  print('price_snapshots collection already exists or time-series error: ' + e);
}

// Create explicit indexes for collections
db.products.createIndex({ site: 1, siteProductId: 1 }, { unique: true });
db.variants.createIndex({ productId: 1, siteSkuId: 1 }, { unique: true });
db.alert_events.createIndex({ dedupeKey: 1 }, { unique: true });
db.trackers.createIndex({ userId: 1, active: 1 });
db.trackers.createIndex({ productId: 1, variantId: 1 });
db.feature_flags.createIndex({ key: 1 }, { unique: true });

print('DropWatch MongoDB initialization completed successfully.');
