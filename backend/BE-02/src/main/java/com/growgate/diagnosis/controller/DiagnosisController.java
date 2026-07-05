package com.growgate.diagnosis.controller;

// 진단 API 엔드포인트
import com.growgate.diagnosis.dto.CreateDiagnosisRequest;
import com.growgate.diagnosis.dto.CreateDiagnosisResponse;
import com.growgate.diagnosis.dto.DiagnosisDetailResponse;
import com.growgate.diagnosis.dto.DiagnosisListResponse;
import com.growgate.diagnosis.dto.DiagnosisReportResponse;
import com.growgate.diagnosis.dto.DiagnosisSourcesResponse;
import com.growgate.diagnosis.service.DiagnosisService;
import com.growgate.agent.service.AgentTraceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final AgentTraceQueryService agentTraceQueryService;

    @GetMapping
    public ResponseEntity<DiagnosisListResponse> getList(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(diagnosisService.getList(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CreateDiagnosisResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDiagnosisRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(diagnosisService.create(userDetails.getUsername(), request));
    }

    @DeleteMapping("/{diagnosisId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String diagnosisId) {
        diagnosisService.delete(userDetails.getUsername(), diagnosisId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{diagnosisId}")
    public ResponseEntity<DiagnosisDetailResponse> getDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(diagnosisService.getDetail(userDetails.getUsername(), diagnosisId));
    }

    @GetMapping("/{diagnosisId}/sources")
    public ResponseEntity<DiagnosisSourcesResponse> getSources(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(diagnosisService.getSources(userDetails.getUsername(), diagnosisId));
    }

    @GetMapping("/{diagnosisId}/report")
    public ResponseEntity<DiagnosisReportResponse> getReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(diagnosisService.getReport(userDetails.getUsername(), diagnosisId));
    }

    @GetMapping("/{diagnosisId}/agent-trace")
    public ResponseEntity<java.util.Map<String, Object>> getAgentTrace(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String diagnosisId) {
        return ResponseEntity.ok(agentTraceQueryService.getTrace(userDetails.getUsername(), diagnosisId));
    }
}
