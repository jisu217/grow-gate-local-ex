package com.growgate.diagnosis.dto;

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
