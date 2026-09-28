package com.dropwatch.api.controller;

import com.dropwatch.api.service.SseStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/stream")
@Tag(name = "Live Stream", description = "Server-Sent Events (SSE) real-time feed")
public class SseStreamController {

    private final SseStreamService sseStreamService;

    public SseStreamController(SseStreamService sseStreamService) {
        this.sseStreamService = sseStreamService;
    }

    @GetMapping(path = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live price update & alert SSE stream")
    public SseEmitter streamLiveUpdates() {
        return sseStreamService.subscribe();
    }
}
