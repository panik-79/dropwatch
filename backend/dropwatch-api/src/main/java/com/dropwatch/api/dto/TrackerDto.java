package com.dropwatch.api.dto;

import com.dropwatch.core.domain.TrackerRule;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class TrackerDto {

    public record CreateTrackerRequest(
            String targetUrl,
            String variantId,
            List<TrackerRule> rules,
            List<String> channelIds,
            int pollIntervalSeconds,
            long cooldownSeconds
    ) implements Serializable {}

    public record UpdateTrackerRequest(
            String variantId,
            List<TrackerRule> rules,
            List<String> channelIds,
            int pollIntervalSeconds,
            long cooldownSeconds,
            Boolean active
    ) implements Serializable {}

    public record TrackerResponse(
            String id,
            String userId,
            String productId,
            String variantId,
            List<TrackerRule> rules,
            boolean active,
            int pollIntervalSeconds,
            List<String> channelIds,
            long cooldownSeconds,
            Instant lastAlertAt,
            Instant createdAt,
            ProductDto.ProductResponse product
    ) implements Serializable {}
}
