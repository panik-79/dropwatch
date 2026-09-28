package com.dropwatch.api.controller;

import com.dropwatch.api.dto.AlertDto;
import com.dropwatch.api.dto.ProductDto;
import com.dropwatch.api.service.ProductService;
import com.dropwatch.core.repository.AlertEventRepository;
import com.dropwatch.core.repository.TrackerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert history and notification log")
public class AlertController {

    private final AlertEventRepository alertEventRepository;
    private final TrackerRepository trackerRepository;
    private final ProductService productService;

    public AlertController(
            AlertEventRepository alertEventRepository,
            TrackerRepository trackerRepository,
            ProductService productService
    ) {
        this.alertEventRepository = alertEventRepository;
        this.trackerRepository = trackerRepository;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get user alert history")
    public ResponseEntity<List<AlertDto.AlertResponse>> getUserAlerts(Authentication auth) {
        String userId = auth.getPrincipal().toString();

        List<String> trackerIds = trackerRepository.findByUserId(userId).stream()
                .map(t -> t.getId())
                .collect(Collectors.toList());

        List<AlertDto.AlertResponse> alerts = trackerIds.stream()
                .flatMap(tid -> alertEventRepository.findByTrackerIdOrderByCreatedAtDesc(tid).stream())
                .map(a -> {
                    ProductDto.ProductResponse prod = null;
                    try {
                        var trackerOpt = trackerRepository.findById(a.getTrackerId());
                        if (trackerOpt.isPresent()) {
                            prod = productService.getProductById(trackerOpt.get().getProductId());
                        }
                    } catch (Exception ignored) {}

                    return new AlertDto.AlertResponse(
                            a.getId(),
                            a.getTrackerId(),
                            a.getRuleId(),
                            a.getRuleType(),
                            a.getPriceAtTrigger(),
                            a.getChannel(),
                            a.getDeliveryStatus(),
                            a.getCreatedAt(),
                            prod
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(alerts);
    }
}
