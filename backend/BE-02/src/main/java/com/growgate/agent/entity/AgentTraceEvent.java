package com.growgate.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "agent_trace_events")
public class AgentTraceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_run_id", nullable = false, length = 64)
    private String agentRunId;

    @Column(name = "diagnosis_id", nullable = false, length = 36)
    private String diagnosisId;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Column(name = "agent_name", nullable = false, length = 80)
    private String agentName;

    @Column(name = "tool_name", nullable = false, length = 160)
    private String toolName;

    @Column(name = "input_hash", length = 96)
    private String inputHash;

    @Column(name = "output_schema", length = 120)
    private String outputSchema;

    @Column(name = "output_summary", length = 1000)
    private String outputSummary;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(length = 80)
    private String provider;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "fallback_reason", length = 1000)
    private String fallbackReason;

    @Column(name = "evidence_count", nullable = false)
    private Integer evidenceCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
