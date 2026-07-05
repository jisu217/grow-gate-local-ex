package com.growgate.agent.service;

import com.growgate.agent.entity.AgentTraceEvent;
import com.growgate.agent.repository.AgentTraceEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTraceRecorderTest {

    @Test
    void recordPersistsAgentTraceEventWithSourceBoundMetadata() {
        AgentTraceEventRepository repository = mock(AgentTraceEventRepository.class);
        when(repository.save(any(AgentTraceEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AgentTraceRecorder recorder = new AgentTraceRecorder(repository);

        recorder.record(AgentTraceRecorder.TraceEvent.builder()
                .agentRunId("agent-run-1")
                .diagnosisId("diagnosis-1")
                .stepNo(2)
                .agentName("Market")
                .toolName("PublicDataCollectionService.collectForDiagnosis")
                .input(Map.of("hsCode", "850760", "targetCountry", "DE"))
                .outputSchema("MarketAgentOutput")
                .outputSummary("KCS_TRADE source-bound market signal")
                .sourceType("cache")
                .provider("KCS_TRADE")
                .endpoint("csv:classpath:publicdata/kcs.csv")
                .latencyMs(42L)
                .status("done")
                .retryCount(0)
                .fallbackReason("")
                .evidenceCount(1)
                .build());

        verify(repository).save(any(AgentTraceEvent.class));
        AgentTraceEvent saved = recorder.record(AgentTraceRecorder.TraceEvent.builder()
                .agentRunId("agent-run-1")
                .diagnosisId("diagnosis-1")
                .stepNo(3)
                .agentName("Guardrail")
                .toolName("ClaimVerificationService.verifyClaims")
                .input(Map.of("claims", 0))
                .outputSchema("GuardrailAgentOutput")
                .outputSummary("source-less claims removed")
                .sourceType("heuristic")
                .provider("RULE_ENGINE")
                .endpoint("")
                .status("done")
                .retryCount(0)
                .evidenceCount(0)
                .build());

        assertThat(saved.getDiagnosisId()).isEqualTo("diagnosis-1");
        assertThat(saved.getOutputSchema()).isEqualTo("GuardrailAgentOutput");
        assertThat(saved.getInputHash()).startsWith("sha256:");
        assertThat(saved.getSourceType()).isEqualTo("heuristic");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
