package com.dropwatch.api.dto;

import java.io.Serializable;
import java.util.List;

public class FlagDto {

    public record EvaluateFlagRequest(
            String flagKey
    ) implements Serializable {}

    public record FlagResponse(
            String id,
            String key,
            String description,
            boolean enabled,
            double rolloutPercentage,
            List<String> targetUsers
    ) implements Serializable {}

    public record UpdateFlagRequest(
            String description,
            boolean enabled,
            double rolloutPercentage,
            List<String> targetUsers
    ) implements Serializable {}
}
