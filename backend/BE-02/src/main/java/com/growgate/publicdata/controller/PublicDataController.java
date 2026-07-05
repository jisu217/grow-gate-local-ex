package com.growgate.publicdata.controller;

import com.growgate.publicdata.dto.PublicDataCollectRequest;
import com.growgate.publicdata.dto.PublicDataCollectResponse;
import com.growgate.publicdata.dto.PublicDataDailyRefreshResponse;
import com.growgate.publicdata.dto.PublicDataDashboardResponse;
import com.growgate.publicdata.dto.PublicDataRecordsResponse;
import com.growgate.publicdata.dto.PublicDataRunsResponse;
import com.growgate.publicdata.dto.PublicDataRecordResponse;
import com.growgate.publicdata.dto.PublicDataRunResponse;
import com.growgate.publicdata.service.PublicDataCollectionService;
import com.growgate.publicdata.service.PublicDataDailyRefreshService;
import com.growgate.publicdata.service.PublicDataOperationalStatusService;
import com.growgate.publicdata.service.PublicDataProofRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataCollectionService collectionService;
    private final PublicDataDailyRefreshService dailyRefreshService;
    private final PublicDataOperationalStatusService operationalStatusService;
    private final PublicDataProofRunService proofRunService;

    @PostMapping("/collect")
    public ResponseEntity<PublicDataCollectResponse> collect(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PublicDataCollectRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(PublicDataCollectResponse.from(collectionService.collectManual(
                        userDetails.getUsername(),
                        request.getHsCode(),
                        request.getTargetCountry(),
                        request.getProductName()
                )));
    }

    @GetMapping("/runs")
    public ResponseEntity<PublicDataRunsResponse> getRuns(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(PublicDataRunsResponse.builder()
                    .runs(List.of())
                    .build());
        }
        return ResponseEntity.ok(PublicDataRunsResponse.builder()
                .runs(collectionService.getRecentRuns(userDetails.getUsername()).stream()
                        .map(PublicDataRunResponse::from)
                        .toList())
                .build());
    }

    @GetMapping("/records")
    public ResponseEntity<PublicDataRecordsResponse> getRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String hsCode,
            @RequestParam(required = false) String targetCountry) {
        if (userDetails == null) {
            return ResponseEntity.ok(PublicDataRecordsResponse.builder()
                    .records(List.of())
                    .build());
        }
        return ResponseEntity.ok(PublicDataRecordsResponse.builder()
                .records(collectionService.getRecentRecords(userDetails.getUsername(), hsCode, targetCountry).stream()
                        .map(PublicDataRecordResponse::from)
                        .toList())
                .build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<PublicDataDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(PublicDataDashboardResponse.builder()
                    .providers(List.of())
                    .build());
        }
        return ResponseEntity.ok(collectionService.getDashboard(userDetails.getUsername()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth(
            @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> operationalStatus = operationalStatusService.status();
        if (userDetails == null) {
            Map<String, Object> response = new java.util.LinkedHashMap<>(operationalStatus);
            response.put("scope", "public-proof-and-configuration");
            response.put("latestProofRun", proofRunService.latest(null));
            return ResponseEntity.ok(response);
        }
        String userId = usernameOrNull(userDetails);
        Map<String, Object> health = new java.util.LinkedHashMap<>(collectionService.getHealth(userId));
        health.put("configuration", operationalStatus);
        health.put("latestProofRun", proofRunService.latest(userId));
        return ResponseEntity.ok(health);
    }

    @PostMapping("/daily-refresh")
    public ResponseEntity<PublicDataDailyRefreshResponse> refreshDaily(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dailyRefreshService.refreshForUser(userDetails.getUsername()));
    }

    @GetMapping("/proof-runs/latest")
    public ResponseEntity<Map<String, Object>> getLatestProofRun(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proofRunService.latest(usernameOrNull(userDetails)));
    }

    @GetMapping("/proof-runs/{proofRunId}")
    public ResponseEntity<Map<String, Object>> getProofRun(
            @PathVariable String proofRunId) {
        return ResponseEntity.ok(proofRunService.detail(proofRunId));
    }

    @GetMapping("/proof-runs/{proofRunId}/raw-responses")
    public ResponseEntity<Map<String, Object>> getProofRunRawResponses(
            @PathVariable String proofRunId) {
        return ResponseEntity.ok(Map.of(
                "proofRunId", proofRunId,
                "providers", proofRunService.rawResponses(proofRunId)
        ));
    }

    @GetMapping("/proof-runs/{proofRunId}/score-contributions")
    public ResponseEntity<Map<String, Object>> getProofRunScoreContributions(
            @PathVariable String proofRunId) {
        return ResponseEntity.ok(Map.of(
                "proofRunId", proofRunId,
                "scoreWaterfall", proofRunService.scoreContributions(proofRunId)
        ));
    }

    private String usernameOrNull(UserDetails userDetails) {
        return userDetails == null ? null : userDetails.getUsername();
    }
}
