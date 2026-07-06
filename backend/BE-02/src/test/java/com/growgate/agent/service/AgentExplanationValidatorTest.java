package com.growgate.agent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExplanationValidatorTest {

    @Test
    void replacesForbiddenFinalAndOverclaimPhrases() {
        AgentExplanationValidator validator = new AgentExplanationValidator();

        String sanitized = validator.sanitizeText("CE 인증 가능 확정이며 특허 침해 없음. 지원사업 선정 보장.");

        assertThat(sanitized)
                .doesNotContain("CE 인증 가능 확정")
                .doesNotContain("특허 침해 없음")
                .doesNotContain("지원사업 선정 보장")
                .contains("CE 적용 가능성과 필요 문서 확인이 필요합니다")
                .contains("최종 FTO 검토가 필요합니다")
                .contains("기관 심사에 따릅니다");
    }

    @Test
    void candidateWithoutSourceIdsIsDowngradedForReview() {
        AgentExplanationValidator validator = new AgentExplanationValidator();

        Map<String, Object> candidate = validator.validateCandidate(Map.of(
                "title", "모든 업무 자동 완료",
                "sourceIds", List.of(),
                "status", "PENDING_REVIEW"
        ));

        assertThat(candidate).containsEntry("status", "DATA_INSUFFICIENT");
        assertThat(candidate.get("title").toString()).doesNotContain("모든 업무 자동 완료");
        assertThat(candidate.get("reviewRequired")).isEqualTo(true);
    }
}
