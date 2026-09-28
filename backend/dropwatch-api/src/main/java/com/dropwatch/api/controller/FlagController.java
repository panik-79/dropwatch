package com.dropwatch.api.controller;

import com.dropwatch.api.dto.FlagDto;
import com.dropwatch.core.domain.FeatureFlagDoc;
import com.dropwatch.core.repository.FeatureFlagRepository;
import com.dropwatch.flags.FeatureFlagEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/flags")
@Tag(name = "Feature Flags", description = "First-party feature flag engine and rollout management")
public class FlagController {

    private final FeatureFlagEngine flagEngine;
    private final FeatureFlagRepository flagRepository;

    public FlagController(FeatureFlagEngine flagEngine, FeatureFlagRepository flagRepository) {
        this.flagEngine = flagEngine;
        this.flagRepository = flagRepository;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate feature flag for current user context")
    public ResponseEntity<Map<String, Object>> evaluateFlag(
            Authentication auth,
            @RequestBody FlagDto.EvaluateFlagRequest req
    ) {
        String userId = auth != null ? auth.getPrincipal().toString() : "anonymous";
        boolean enabled = flagEngine.isEnabled(req.flagKey(), userId);
        return ResponseEntity.ok(Map.of("key", (Object) req.flagKey(), "enabled", (Object) enabled));
    }

    @GetMapping
    @Operation(summary = "List all registered feature flags")
    public ResponseEntity<List<FlagDto.FlagResponse>> listFlags() {
        List<FlagDto.FlagResponse> flags = flagRepository.findAll().stream()
                .map(f -> new FlagDto.FlagResponse(
                        f.getId(),
                        f.getKey(),
                        f.getDescription(),
                        f.isEnabled(),
                        f.getPercentage() != null ? f.getPercentage() : 0.0,
                        f.getAllowList()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(flags);
    }

    @PostMapping
    @Operation(summary = "Create or update feature flag")
    public ResponseEntity<FlagDto.FlagResponse> createOrUpdateFlag(@RequestBody FlagDto.FlagResponse req) {
        FeatureFlagDoc doc = FeatureFlagDoc.builder()
                .id(req.id())
                .key(req.key())
                .description(req.description())
                .type(FeatureFlagDoc.FlagType.BOOLEAN)
                .enabled(req.enabled())
                .percentage(req.rolloutPercentage())
                .allowList(req.targetUsers())
                .updatedAt(Instant.now())
                .build();

        FeatureFlagDoc saved = flagEngine.saveOrUpdateFlag(doc);
        return ResponseEntity.ok(new FlagDto.FlagResponse(
                saved.getId(),
                saved.getKey(),
                saved.getDescription(),
                saved.isEnabled(),
                saved.getPercentage() != null ? saved.getPercentage() : 0.0,
                saved.getAllowList()
        ));
    }
}
