package com.dropwatch.api.controller;

import com.dropwatch.api.dto.TrackerDto;
import com.dropwatch.api.service.TrackerApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trackers")
@Tag(name = "Trackers", description = "Price & stock tracker management")
public class TrackerController {

    private final TrackerApiService trackerApiService;

    public TrackerController(TrackerApiService trackerApiService) {
        this.trackerApiService = trackerApiService;
    }

    private String resolveUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            if (!principal.isBlank() && !"anonymousUser".equalsIgnoreCase(principal)) {
                return principal;
            }
        }
        return "usr-demo-123";
    }

    @PostMapping
    @Operation(summary = "Create new tracker for product URL")
    public ResponseEntity<TrackerDto.TrackerResponse> createTracker(
            Authentication auth,
            @RequestBody TrackerDto.CreateTrackerRequest req
    ) {
        String userId = resolveUserId(auth);
        return ResponseEntity.ok(trackerApiService.createTracker(userId, req));
    }

    @GetMapping
    @Operation(summary = "Get list of active trackers for authenticated user")
    public ResponseEntity<List<TrackerDto.TrackerResponse>> getUserTrackers(Authentication auth) {
        String userId = resolveUserId(auth);
        return ResponseEntity.ok(trackerApiService.getUserTrackers(userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tracker configuration")
    public ResponseEntity<TrackerDto.TrackerResponse> updateTracker(
            Authentication auth,
            @PathVariable String id,
            @RequestBody TrackerDto.UpdateTrackerRequest req
    ) {
        String userId = resolveUserId(auth);
        return ResponseEntity.ok(trackerApiService.updateTracker(userId, id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tracker by ID")
    public ResponseEntity<Void> deleteTracker(Authentication auth, @PathVariable String id) {
        String userId = resolveUserId(auth);
        trackerApiService.deleteTracker(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause tracker updates")
    public ResponseEntity<TrackerDto.TrackerResponse> pauseTracker(Authentication auth, @PathVariable String id) {
        String userId = resolveUserId(auth);
        return ResponseEntity.ok(trackerApiService.pauseTracker(userId, id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume tracker updates")
    public ResponseEntity<TrackerDto.TrackerResponse> resumeTracker(Authentication auth, @PathVariable String id) {
        String userId = resolveUserId(auth);
        return ResponseEntity.ok(trackerApiService.resumeTracker(userId, id));
    }
}
