package com.dropwatch.api.dto;

import com.dropwatch.core.domain.AlertEvent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public class AlertDto {

    public record AlertResponse(
            String id,
            String trackerId,
            String ruleId,
            String ruleType,
            BigDecimal priceAtTrigger,
            String channel,
            AlertEvent.DeliveryStatus deliveryStatus,
            Instant createdAt,
            ProductDto.ProductResponse product
    ) implements Serializable {}
}
