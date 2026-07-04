package com.growgate.diagnosis.report;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportRendererTest {

    @Test
    void officialMarkdownReportContainsFrontMatterAppendixAndLimitations() {
        MarkdownReportRenderer renderer = new MarkdownReportRenderer(
                new ReportFrontMatterBuilder(),
                new ReportClaimVerifier()
        );

        String markdown = renderer.render(MarkdownReportRenderer.ReportContext.builder()
                .diagnosisId("diag-1")
                .productName("배터리팩")
                .hsCode("850760")
                .targetCountry("DE")
                .overallScore(82)
                .publicDataProofRunId("run-1")
                .risks(List.of(Map.of("label", "FTO 검토", "nextAction", "KIPRIS 1차 검색")))
                .roadmap(Map.of("thisWeek", List.of(Map.of("title", "UN38.3 확인"))))
                .sourceTrustSummary(Map.of("sourceIds", List.of("KCS_TRADE:850760:DE")))
                .synthetic(false)
                .build());

        assertThat(markdown).startsWith("---\n");
        assertThat(markdown).contains(
                "publicDataProofRunId: run-1",
                "modelVersion: growgate-gate-risk-v0.1.0",
                "reportFormat: markdown",
                "limitation:",
                "## Source Appendix",
                "KCS_TRADE:850760:DE",
                "## 근거 데이터와 AI 보조 해석 구분",
                "sourceType: live/cache/fallback/seed/empty",
                "## FTO 사전 탐색 고지",
                "법률 판단이 아니며",
                "변리사 또는 법률 전문가 검토가 필요합니다",
                "## 개인정보·영업비밀 입력 제한",
                "## Synthetic Validation Disclaimer",
                "synthetic persona validation은 실제 고객 인터뷰가 아닙니다."
        );
        assertThat(markdown).doesNotContain("자동 PDF 생성");
    }
}
