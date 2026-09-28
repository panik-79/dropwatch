# ADR 0001: Monorepo Architecture, Java 21 Virtual Threads & Non-Blocking Polling Loop

## Context
DropWatch requires a distributed, highly concurrent e-commerce price and stock tracking system capable of scraping multiple Indian e-commerce platforms (Myntra, Flipkart) on configurable schedules. The backend must handle high I/O throughput (HTTP fetch requests), resilient message routing, time-series storage, and instant alerts via Telegram.

## Decisions

### 1. Monorepo Structure
We adopt a single monorepo layout containing both backend Maven modules (`dropwatch-core`, `dropwatch-scraper`, `dropwatch-messaging`, `dropwatch-notify`, `dropwatch-flags`, `dropwatch-api`, `dropwatch-app`) and frontend (`frontend/` Angular 18+ app). This ensures cohesive configuration management, single-command development, and synchronized API contracts.

### 2. Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`)
Scraper workers spend most of their execution time waiting on HTTP I/O response latencies. Virtual Threads allow Spring Boot web applications and message listeners to spawn lightweight threads per request/task without thread-pool starvation or reactive callback complexity.

### 3. Native RabbitMQ TTL + Dead Letter Exchange (DLX) Delayed Loop
Rather than relying on external plugin dependencies, we implement a delayed retry and polling loop using standard RabbitMQ exchanges and queues (`dw.scrape.delay.1m`, `dw.scrape.delay.5m`, `dw.scrape.delay.15m`) configured with `x-message-ttl` and `x-dead-letter-exchange` routing back to `dw.scrape.work`.

### 4. Native Time-Series Collection in MongoDB
Price snapshots are stored in MongoDB 7.0 native time-series collection `price_snapshots` (`timeField=ts`, `metaField=meta{variantId, site}`, granularity `minutes`), ensuring high insertion performance and compressed storage.

## Consequences
- High I/O throughput without netty/reactive overhead.
- Reproducible local infra via Docker Compose.
- Single-command developer setup (`make up`, `make dev`).
