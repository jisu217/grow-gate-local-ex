package com.growgate.diagnosis.dto;

// 진단 이력 목록 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DiagnosisListResponse {

    private List<DiagnosisSummary> diagnoses;

    @Getter
    @Builder
    public static class DiagnosisSummary {
        private String id;
        private String productId;
        private String productName;
        private String targetCountry;
        private String status;
        private String createdAt;
        private Integer overallScore;
    }
}
