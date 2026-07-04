package com.growgate.publicdata.controller;

import com.growgate.publicdata.service.PublicDataProofRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PublicDataProofController {

    private final PublicDataProofRunService proofRunService;

    @PostMapping("/api/admin/public-data/proof-runs")
    public ResponseEntity<Map<String, Object>> createProofRun(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "manual") String triggerType) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(proofRunService.createProofRun(userDetails.getUsername(), triggerType));
    }

    @GetMapping("/api/diagnoses/{diagnosisId}/public-data-waterfall")
    public ResponseEntity<Map<String, Object>> getPluralDiagnosisWaterfall(
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(proofRunService.waterfallForDiagnosis(diagnosisId));
    }

    @GetMapping("/api/diagnosis/{diagnosisId}/public-data-waterfall")
    public ResponseEntity<Map<String, Object>> getDiagnosisWaterfall(
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(proofRunService.waterfallForDiagnosis(diagnosisId));
    }
}
