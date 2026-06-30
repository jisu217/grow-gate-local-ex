package com.growgate.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DiagnosisReportResponse {

    private String diagnosisId;
    private String fileName;
    private String contentType;
    private String markdown;
    private boolean fastApiUsed;
}
