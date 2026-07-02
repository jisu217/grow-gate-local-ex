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
        private GateDecision gateDecision;
        private ImpactEstimate impactEstimate;
        private ConfidencePanel confidencePanel;
        private String summary;
        private List<RiskItem> riskItems;
        private ActionRoadmap actionRoadmap;
        private List<String> evidenceSources;
        private Map<String, Object> agentTrace;
        private Map<String, Object> evidenceRetrieval;
        private List<Map<String, Object>> exportKnowledgeTimeline;
        private Map<String, Object> evidenceGuardrails;
        private Map<String, Object> stageCards;
        private String reportMarkdown;
        private boolean reportFastApiUsed;
        private String errorType;
        private String errorMessage;
        private String failedAt;
    }

    @Getter
    @Builder
    public static class GateDecision {
        private String status;
        private String label;
        private String oneLineReason;
        private String allowedNextStep;
        private String blockedStep;
        private String buyerReplyGuidance;
        private List<String> mustFixBeforeQuote;
    }

    @Getter
    @Builder
    public static class ImpactEstimate {
        private int timeSavedHours;
        private int delayRiskDays;
        private long avoidableCostKrw;
        private long supportFundingOpportunityKrw;
        private String explanation;
    }

    @Getter
    @Builder
    public static class ConfidencePanel {
        private String level;
        private int liveSourceCount;
        private int cacheSourceCount;
        private int fallbackSourceCount;
        private List<String> missingEvidence;
        private boolean humanReviewRequired;
        private String reviewReason;
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
        private String riskId;
        private String category;
        private String level;
        private String label;
        private String severity;
        private String probability;
        private String businessImpact;
        private String requiredEvidence;
        private String ownerRole;
        private String deadline;
        private String nextAction;
        private List<String> sourceIds;
    }

    @Getter
    @Builder
    public static class ActionRoadmap {
        private int d30;
        private int d60;
        private int d90;
        private List<ActionItem> thisWeek;
        private List<ActionItem> beforeQuote;
        private List<ActionItem> beforeShipment;
    }

    @Getter
    @Builder
    public static class ActionItem {
        private String title;
        private String owner;
        private String output;
        private String dueDate;
        private String reason;
        private String blockedIfNotDone;
        private List<String> sourceIds;
    }

    @Getter
    @Builder
    public static class KnowledgeSnapshot {
        private String productName;
        private String hsCode;
        private String targetCountry;
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
