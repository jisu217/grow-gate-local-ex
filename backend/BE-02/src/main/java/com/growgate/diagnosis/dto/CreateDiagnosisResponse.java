package com.growgate.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateDiagnosisResponse {

    private String diagnosisId;
    private String status;
    private String message;
}
