package com.growgate.agent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimVerificationServiceTest {

    private final ClaimVerificationService service = new ClaimVerificationService();

    @Test
    void blocksSourceLessClaimAndExposesFallbackReason() {
        Map<String, Object> result = service.verifyClaims(List.of(
                Map.of("text", "독일 시장성은 확실히 높습니다.")
        ), List.of("SRC-1"));

        assertThat(result.get("safeClaims")).asList().isEmpty();
        assertThat(result.get("fallbackReasons").toString()).contains("missing sourceId");
        assertThat(result.get("claimVerifications").toString()).contains("BLOCK");
    }

    @Test
    void passesSourcedClaimWithKnownSourceId() {
        Map<String, Object> result = service.verifyClaims(List.of(
                Map.of("text", "KCS source에서 수출 신호가 확인되었습니다.", "sourceId", "SRC-1")
        ), List.of("SRC-1"));

        assertThat(result.get("safeClaims")).asList().hasSize(1);
        assertThat(result.get("claimVerifications").toString()).contains("PASS");
        assertThat(result.get("fallbackReasons")).asList().isEmpty();
    }

    @Test
    void blocksUnknownSourceAndUnsafeDefinitiveClaim() {
        Map<String, Object> result = service.verifyClaims(List.of(
                Map.of("text", "FTO 문제 없음으로 확정됩니다.", "sourceId", "SRC-404")
        ), List.of("SRC-1"));

        assertThat(result.get("safeClaims")).asList().isEmpty();
        assertThat(result.get("claimVerifications").toString()).contains("BLOCK");
        assertThat(result.get("claimVerifications").toString()).contains("unknown sourceId");
    }
}
