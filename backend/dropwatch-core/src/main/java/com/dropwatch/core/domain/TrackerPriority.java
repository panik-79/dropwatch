package com.dropwatch.core.domain;

public enum TrackerPriority {
    NORMAL,
    HIGH_FLASH_SALE;

    public String toDelayQueueRoutingKey() {
        return switch (this) {
            case HIGH_FLASH_SALE -> "delay.1m";
            case NORMAL -> "delay.5m";
        };
    }
}
