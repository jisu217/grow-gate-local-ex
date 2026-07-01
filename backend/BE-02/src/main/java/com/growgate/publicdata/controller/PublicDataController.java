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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataCollectionService collectionService;
    private final PublicDataDailyRefreshService dailyRefreshService;

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
        return ResponseEntity.ok(PublicDataRecordsResponse.builder()
                .records(collectionService.getRecentRecords(userDetails.getUsername(), hsCode, targetCountry).stream()
                        .map(PublicDataRecordResponse::from)
                        .toList())
                .build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<PublicDataDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(collectionService.getDashboard(userDetails.getUsername()));
    }

    @PostMapping("/daily-refresh")
    public ResponseEntity<PublicDataDailyRefreshResponse> refreshDaily(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(dailyRefreshService.refreshForUser(userDetails.getUsername()));
    }
}
