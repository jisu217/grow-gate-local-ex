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
            + "의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.";

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
                + "견적과 납기 제시 전 `" + topRisk + "` 항목을 우선 점검하고, "
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
                + "- 확인 필요 사항: 담당자·인증기관·변리사 검토 전 단정 금지\n\n"
                + "주의: 본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부에 대한 담당자 의사결정을 대신하지 않습니다.\n\n"
                + "## AI Agent 판단 이유 및 지식화 내역\n\n"
                + agentKnowledgeSection(context.getAgentKnowledgeCandidates())
                + "\n"
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

    @SuppressWarnings("unchecked")
    private String agentKnowledgeSection(List<Map<String, Object>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "- 검토 대기 지식 후보 없음\n"
                    + "- 승인된 제품 지식만 다음 진단의 신뢰도 높은 지식으로 재사용합니다.\n";
        }
        StringBuilder out = new StringBuilder();
        for (Map<String, Object> candidate : candidates) {
            out.append("### Agent: ").append(safeText(candidate.get("agent"), "UNKNOWN")).append("\n\n")
                    .append("- 결론: ").append(safeText(candidate.get("plainSummary"), "담당자 검토 필요")).append("\n")
                    .append("- 이유: ").append(safeText(candidate.get("whyItMatters"), "근거와 한계 확인 필요")).append("\n")
                    .append("- 근거: ").append(join(candidate.get("evidenceSummary"))).append("\n")
                    .append("- 한계: ").append(join(candidate.get("limitations"))).append("\n")
                    .append("- 다음 행동: ").append(join(candidate.get("nextActions"))).append("\n")
                    .append("- 데이터 상태: ").append(dataStatus(candidate.get("dataStatus"))).append("\n")
                    .append("- LLM 보조 여부: ").append(llmStatus(candidate.get("llm"))).append("\n")
                    .append("- 지식화 상태: ").append(safeText(candidate.get("status"), "PENDING_REVIEW")).append("\n")
                    .append("- 담당자 검토 필요 여부: ").append(String.valueOf(candidate.getOrDefault("reviewRequired", true))).append("\n\n");
        }
        out.append("- 승인된 제품 지식만 다음 진단의 신뢰도 높은 지식으로 재사용합니다.\n");
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private String dataStatus(Object value) {
        if (!(value instanceof Map<?, ?> map)) return "unknown";
        Object mode = ((Map<String, Object>) map).getOrDefault("mode", "unknown");
        Object provider = ((Map<String, Object>) map).getOrDefault("provider", "");
        return (String.valueOf(provider).isBlank() ? "" : provider + " / ") + mode;
    }

    @SuppressWarnings("unchecked")
    private String llmStatus(Object value) {
        if (!(value instanceof Map<?, ?> map)) return "used=false";
        Map<String, Object> llm = (Map<String, Object>) map;
        return "used=" + llm.getOrDefault("used", false)
                + ", provider=" + llm.getOrDefault("provider", "spring-local")
                + ", fallbackUsed=" + llm.getOrDefault("fallbackUsed", false);
    }

    private String join(Object value) {
        if (value instanceof List<?> list) {
            String joined = list.stream()
                    .filter(item -> item != null && !String.valueOf(item).isBlank())
                    .map(String::valueOf)
                    .limit(6)
                    .reduce((left, right) -> left + " / " + right)
                    .orElse("담당자 확인 필요");
            return safeText(joined, "담당자 확인 필요");
        }
        return safeText(value, "담당자 확인 필요");
    }

    private String safeText(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        String text = String.valueOf(value).replace("\n", " ");
        return text
                .replace("모든 데이터가 live로 수집됨", "현재 확인 가능한 데이터 상태를 기준으로 판단했으며, 일부 항목은 cache 또는 fallback일 수 있습니다.")
                .replace("입력만으로 통관 가능 확정", "입력 정보를 바탕으로 통관 전 검토해야 할 항목을 정리했습니다.")
                .replace("시장성이 높으니 바로 수출 가능", "시장 신호는 긍정적일 수 있으나 인증·가격·물류·바이어 조건 검토가 함께 필요합니다.")
                .replace("CE 인증 가능 확정", "CE 적용 가능성과 필요 문서 확인이 필요합니다.")
                .replace("리콜 없음 = 안전함", "확인된 유사 리콜 사례가 제한적이더라도 제품안전 점검은 별도로 필요합니다.")
                .replace("특허 침해 없음", "공개 특허 기준의 위험 후보 탐색 결과이며 FTO 검토가 필요합니다.")
                .replace("지원사업 선정 보장", "조건이 유사한 지원사업 후보이며 기관 심사에 따릅니다.")
                .replace("모든 업무 자동 완료", "AI가 업무 초안을 정리했으며 담당자가 우선순위와 완료 여부를 확인해야 합니다.")
                .replace("법률·인증 최종 보고서", "담당자 검토를 돕기 위한 수출 준비도 검토 보고서 초안입니다.")
                .replace("AI가 자율 최종 판단", "AI는 담당자 검토를 돕는 근거를 정리합니다.");
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
        private List<Map<String, Object>> agentKnowledgeCandidates;
        private boolean synthetic;
    }
}
