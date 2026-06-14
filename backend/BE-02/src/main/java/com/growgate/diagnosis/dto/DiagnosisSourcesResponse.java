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
        private String documentName;
        private String url;
        private String retrievedAt;
    }
}
