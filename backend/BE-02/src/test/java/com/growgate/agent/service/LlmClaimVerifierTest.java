package com.growgate.agent.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmClaimVerifierTest {

    private final LlmClaimVerifier verifier = new LlmClaimVerifier();

    @Test
    void removesClaimWithoutSourceId() {
        Map<String, Object> result = verifier.verify(List.of(
                Map.of("text", "독일 시장성이 높습니다.")
        ), List.of("SRC-1"));

        assertThat(result.get("acceptedCount")).isEqualTo(0);
        assertThat(result.get("rejectedClaims").toString()).contains("missing sourceId");
    }

    @Test
    void removesClaimWithUnknownSourceId() {
        Map<String, Object> result = verifier.verify(List.of(
                Map.of("text", "KCS metric 기반 시장 신호입니다.", "sourceId", "SRC-404")
        ), List.of("SRC-1"));

        assertThat(result.get("acceptedCount")).isEqualTo(0);
        assertThat(result.get("rejectedClaims").toString()).contains("unknown sourceId");
    }

    @Test
    void removesArbitraryNumericClaimWithoutMetricEvidence() {
        Map<String, Object> result = verifier.verify(List.of(
                Map.of("text", "성공률이 92%입니다.", "sourceId", "SRC-1")
        ), List.of("SRC-1"));

        assertThat(result.get("acceptedCount")).isEqualTo(0);
        assertThat(result.get("rejectedClaims").toString()).contains("numeric claim without metric evidence");
    }

    @Test
    void removesDefinitiveFtoSafetyClaim() {
        Map<String, Object> result = verifier.verify(List.of(
                Map.of("text", "FTO 문제 없음으로 판단됩니다.", "sourceId", "SRC-1")
        ), List.of("SRC-1"));

        assertThat(result.get("acceptedCount")).isEqualTo(0);
        assertThat(result.get("rejectedClaims").toString()).contains("unsafe definitive claim");
    }

    @Test
    void acceptsSourceBoundMetricClaim() {
        Map<String, Object> result = verifier.verify(List.of(
                Map.of("text", "KCS metric에서 YoY 12.4% 신호가 확인되었습니다.",
                        "sourceId", "SRC-1",
                        "numericEvidence", true)
        ), List.of("SRC-1"));

        assertThat(result.get("acceptedCount")).isEqualTo(1);
        assertThat(result.get("verdict")).isEqualTo("pass");
    }
}
