package com.growgate.agent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentKnowledgeCaptureServiceTest {

    private final AgentKnowledgeCaptureService service = new AgentKnowledgeCaptureService(
            new AgentInstructionRegistry(),
            new AgentExplanationValidator()
    );

    @Test
    void convertsAgentExplanationsIntoReviewableKnowledgeCandidates() {
        AgentInferenceResult llmResult = AgentInferenceResult.success(
                "gemini",
                "gemini-2.5-flash",
                Map.of("summary", "CE 인증 가능 확정"),
                List.of(),
                120
        );

        AgentKnowledgeCaptureService.CaptureResult result = service.capture(
                "diag-1",
                "product-1",
                "company-1",
                "배터리팩",
                "850760",
                "DE",
                Map.of("market", 72, "regulation", 64, "recall", 58, "fto", 61, "support", 69),
                List.of(Map.of(
                        "sourceId", "KTL_CERTINFO-cache-1",
                        "provider", "KTL Certinfo",
                        "sourceType", "cache",
                        "usedFor", "regulation"
                )),
                List.of(Map.of("label", "CE 문서 확인", "nextAction", "DoC와 라벨 준비")),
                Map.of("thisWeek", List.of(Map.of("title", "CE 적용 가능성 확인"))),
                llmResult
        );

        assertThat(result.explanations()).hasSize(10);
        assertThat(result.candidates()).hasSize(10);
        assertThat(result.summary()).containsEntry("candidateCount", 10);

        Map<String, Object> regulation = result.candidates().stream()
                .filter(item -> "REGULATION_CERTIFICATION".equals(item.get("agent")))
                .findFirst()
                .orElseThrow();
        assertThat(regulation).containsEntry("knowledgeType", "REGULATION_CHECKLIST");
        assertThat(regulation.get("status")).isEqualTo("PENDING_REVIEW");
        assertThat(regulation.toString()).doesNotContain("CE 인증 가능 확정");
        assertThat(regulation.get("llm").toString()).contains("gemini");
    }
}
