package com.growgate.diagnosis.dto;

// 진단 출처 데이터 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DiagnosisSourcesResponse {

    private String diagnosisId;
    private List<SourceEntry> sources;

    @Getter
    @Builder
    public static class SourceEntry {
        private String field;
        private String sourceId;
        private String provider;
        private String agency;
        private String ministry;
        private String dataFamily;
        private Boolean motieRelated;
        private String datasetName;
        private String documentName;
        private String url;
        private String retrievedAt;
        private String recordId;
        private String matchedKey;
        private String usedFor;
        private Integer scoreContribution;
        private Integer rawScoreContribution;
        private Integer confidenceAdjustedContribution;
        private Double confidence;
        private String scoreFormula;
        private Object scoreInputs;
        private String sourceType;
        private Boolean fallbackUsed;
        private Boolean cacheHit;
        private String collectionRunId;
        private String errorMessage;
        private String reason;
    }
}
