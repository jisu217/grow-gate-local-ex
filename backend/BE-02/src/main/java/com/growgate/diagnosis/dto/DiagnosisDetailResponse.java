package com.growgate.diagnosis.dto;

// 진단 결과 상세 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DiagnosisDetailResponse {

    private String id;
    private String productId;
    private String companyId;
    private String knowledgeSnapshotId;
    private String productName;
    private String targetCountry;
    private String status;
    private ResultData result;
    private KnowledgeSnapshot knowledgeSnapshot;
    private String createdAt;

    @Getter
    @Builder
    public static class ResultData {
        private DiagnosisScore diagnosisScore;
        private String summary;
        private List<RiskItem> riskItems;
        private ActionRoadmap actionRoadmap;
        private List<String> evidenceSources;
        private Map<String, Object> agentTrace;
        private Map<String, Object> stageCards;
        private String reportMarkdown;
        private boolean reportFastApiUsed;
    }

    @Getter
    @Builder
    public static class DiagnosisScore {
        private int overall;
        private int market;
        private int regulation;
        private int recall;
        private int fto;
        private int support;
    }

    @Getter
    @Builder
    public static class RiskItem {
        private String level;
        private String label;
    }

    @Getter
    @Builder
    public static class ActionRoadmap {
        private int d30;
        private int d60;
        private int d90;
    }

    @Getter
    @Builder
    public static class KnowledgeSnapshot {
        private String trustGrade;
        private String sourceType;
        private List<CertSnapshot> certifications;
        private List<String> missingFields;
        private String snapshotAt;
    }

    @Getter
    @Builder
    public static class CertSnapshot {
        private String name;
        private String expiresAt;
        private long daysUntilExpiry;
    }
}
