package com.growgate.diagnosis.report;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarkdownReportRenderer {

    public static final String DEFAULT_MODEL_VERSION = "growgate-gate-risk-v0.1.0";
    public static final String FTO_LEGAL_BOUNDARY = "본 결과는 공개 특허·상표 데이터와 입력 정보를 기반으로 한 사전 탐색 결과입니다. "
            + "특허 침해 여부, 실시 가능성, 권리 유효성에 대한 법률 판단이 아니며, "
            + "최종 의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.";

    private final ReportFrontMatterBuilder frontMatterBuilder;
    private final ReportClaimVerifier claimVerifier;

    public MarkdownReportRenderer(ReportFrontMatterBuilder frontMatterBuilder,
                                  ReportClaimVerifier claimVerifier) {
        this.frontMatterBuilder = frontMatterBuilder;
        this.claimVerifier = claimVerifier;
    }

    public String render(ReportContext context) {
        ReportContext safeContext = context == null ? ReportContext.builder().build() : context;
        List<String> sourceIds = sourceIds(safeContext.getSourceTrustSummary());
        String body = body(safeContext, sourceIds);
        List<ReportClaimVerifier.ClaimVerification> verifications = claimVerifier.verify(body, sourceIds);

        return frontMatterBuilder.build(safeContext)
                + body
                + "\n## Claim Verification\n\n"
                + verificationList(verifications)
                + "\n## Source Appendix\n\n"
                + sourceAppendix(sourceIds)
                + "\n## Synthetic Validation Disclaimer\n\n"
                + "synthetic persona validation은 실제 고객 인터뷰가 아닙니다. "
                + "합성 데이터는 시연 경로와 UX 검증용으로만 사용하며 live 공공데이터 proof와 구분합니다.\n";
    }

    private String body(ReportContext context, List<String> sourceIds) {
        String topRisk = firstRiskValue(context.getRisks(), "label", "출처 최신성 점검");
        String firstAction = firstRiskValue(context.getRisks(), "nextAction", "공공데이터 source trace 최신성을 확인하십시오.");
        return "# GROW-GATE Local EX Markdown 리포트\n\n"
                + "- 제품: " + safe(context.getProductName(), "입력 제품") + "\n"
                + "- HS Code: " + safe(context.getHsCode(), "미등록") + "\n"
                + "- 대상국: " + safe(context.getTargetCountry(), "미입력") + "\n"
                + "- Gate 판단 점수: " + context.getOverallScore() + "/100\n"
                + "- 공식 산출물: Markdown 리포트 다운로드\n"
                + "- PDF 보관: 필요 시 브라우저 인쇄 또는 CI 변환으로 보관 가능\n\n"
                + "## 바이어에게 보낼 1차 회신 초안\n\n"
                + "현재 제품의 인증·리콜·FTO·공공데이터 근거를 확인 중입니다. "
                + "견적과 납기 확정 전 `" + topRisk + "` 항목을 우선 점검하고, "
                + "확인 결과와 보완 자료를 함께 회신드리겠습니다.\n\n"
                + "## 담당자 즉시 할 일\n\n"
                + "- 이번 주: " + actionTitles(context.getRoadmap(), "thisWeek") + "\n"
                + "- 견적 전: " + actionTitles(context.getRoadmap(), "beforeQuote") + "\n"
                + "- 출하 전: " + actionTitles(context.getRoadmap(), "beforeShipment") + "\n\n"
                + "## 우선 보완 근거\n\n"
                + topRisk + " 때문에 `" + firstAction + "` 조치가 필요합니다.\n\n"
                + "## Public Data Proof\n\n"
                + "- proofRunId: " + safe(context.getPublicDataProofRunId(), "not-available") + "\n"
                + "- source count: " + sourceIds.size() + "\n\n"
                + "## 근거 데이터와 AI 보조 해석 구분\n\n"
                + "### 근거 데이터\n\n"
                + "- Provider: Source Appendix의 provider/sourceId별 확인\n"
                + "- sourceType: live/cache/fallback/seed/empty를 구분해 확인\n"
                + "- 조회일: fetchedAt 또는 proofRun 생성 시각 기준\n"
                + "- 원문 링크: provider officialUrl 또는 record endpoint 기준\n"
                + "- rawCount/normalizedCount: public-data normalized payload 기준\n\n"
                + "### AI 보조 해석\n\n"
                + "- 요약: source trace와 규칙 점수 설명을 사람이 읽기 쉽게 정리\n"
                + "- 리스크 후보: 전문가 검토가 필요한 후보 항목 표시\n"
                + "- 확인 필요 사항: 담당자·인증기관·변리사 검토 전 확정 금지\n\n"
                + "주의: 본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다.\n\n"
                + "## FTO 사전 탐색 고지\n\n"
                + FTO_LEGAL_BOUNDARY + "\n\n"
                + "## 개인정보·영업비밀 입력 제한\n\n"
                + "데모 환경에는 주민등록번호, 연락처, 계약서, 도면, 원가, 거래처명, 미공개 인증문서 등 "
                + "개인정보 또는 영업비밀에 해당할 수 있는 정보를 입력하지 마십시오. "
                + "실제 개인정보가 포함될 경우 별도 동의와 처리방침이 필요하며, 로그에는 개인정보/API key/영업비밀을 남기지 않습니다.\n\n";
    }

    private String verificationList(List<ReportClaimVerifier.ClaimVerification> verifications) {
        if (verifications == null || verifications.isEmpty()) return "- WARN: claim verification result 없음\n";
        StringBuilder out = new StringBuilder();
        for (ReportClaimVerifier.ClaimVerification verification : verifications) {
            out.append("- ").append(verification.status())
                    .append(" [").append(verification.code()).append("] ")
                    .append(verification.message()).append("\n");
        }
        return out.toString();
    }

    private String sourceAppendix(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) return "- source trace 없음: 담당자 검토 필요\n";
        StringBuilder out = new StringBuilder();
        for (String sourceId : sourceIds) {
            out.append("- ").append(sourceId).append("\n");
        }
        return out.toString();
    }

    private List<String> sourceIds(Map<String, Object> sourceTrustSummary) {
        if (sourceTrustSummary == null) return List.of();
        Object value = sourceTrustSummary.get("sourceIds");
        if (!(value instanceof List<?> raw)) return List.of();
        List<String> ids = new ArrayList<>();
        for (Object item : raw) {
            if (item != null && !String.valueOf(item).isBlank()) ids.add(String.valueOf(item));
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private String actionTitles(Map<String, Object> roadmap, String key) {
        if (roadmap == null) return "담당자 확인 항목 없음";
        Object value = roadmap.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) return "담당자 확인 항목 없음";
        return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> String.valueOf(item.getOrDefault("title", "")))
                .filter(title -> !title.isBlank())
                .limit(3)
                .reduce((left, right) -> left + " / " + right)
                .orElse("담당자 확인 항목 없음");
    }

    private String firstRiskValue(List<Map<String, Object>> risks, String key, String fallback) {
        if (risks == null || risks.isEmpty()) return fallback;
        Object value = risks.get(0).get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.replace("\n", " ");
    }

    @Getter
    @Builder
    public static class ReportContext {
        private String diagnosisId;
        private String productName;
        private String hsCode;
        private String targetCountry;
        private int overallScore;
        @Builder.Default
        private String modelVersion = DEFAULT_MODEL_VERSION;
        private String publicDataProofRunId;
        private List<Map<String, Object>> risks;
        private Map<String, Object> roadmap;
        private Map<String, Object> sourceTrustSummary;
        private boolean synthetic;
    }
}
