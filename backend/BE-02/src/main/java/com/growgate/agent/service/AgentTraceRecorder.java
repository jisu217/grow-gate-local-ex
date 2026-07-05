package com.growgate.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.agent.entity.AgentTraceEvent;
import com.growgate.agent.repository.AgentTraceEventRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentTraceRecorder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AgentTraceEventRepository repository;

    @Transactional
    public AgentTraceEvent record(TraceEvent event) {
        AgentTraceEvent entity = AgentTraceEvent.builder()
                .agentRunId(blankTo(event.agentRunId, "agent-run-unknown"))
                .diagnosisId(blankTo(event.diagnosisId, "diagnosis-unknown"))
                .stepNo(event.stepNo == null ? 0 : event.stepNo)
                .agentName(blankTo(event.agentName, "workflow"))
                .toolName(blankTo(event.toolName, "unknown-tool"))
                .inputHash("sha256:" + sha256(toJson(event.input)))
                .outputSchema(blankTo(event.outputSchema, "TraceEvent"))
                .outputSummary(prefix(event.outputSummary, 1000))
                .sourceType(blankTo(event.sourceType, "heuristic"))
                .provider(blankTo(event.provider, ""))
                .endpoint(blankTo(event.endpoint, ""))
                .latencyMs(event.latencyMs)
                .status(blankTo(event.status, "done"))
                .retryCount(event.retryCount == null ? 0 : event.retryCount)
                .fallbackReason(prefix(event.fallbackReason, 1000))
                .evidenceCount(event.evidenceCount == null ? 0 : event.evidenceCount)
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(entity);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "hash-error";
        }
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String prefix(String value, int limit) {
        if (value == null) return "";
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    @Getter
    @Builder
    public static class TraceEvent {
        private String agentRunId;
        private String diagnosisId;
        private Integer stepNo;
        private String agentName;
        private String toolName;
        private Map<String, Object> input;
        private String outputSchema;
        private String outputSummary;
        private String sourceType;
        private String provider;
        private String endpoint;
        private Long latencyMs;
        private String status;
        private Integer retryCount;
        private String fallbackReason;
        private Integer evidenceCount;
    }
}
