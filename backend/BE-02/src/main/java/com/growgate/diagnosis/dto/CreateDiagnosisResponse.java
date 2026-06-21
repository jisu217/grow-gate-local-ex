package com.growgate.diagnosis.dto;

// 진단 실행 응답 DTO
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateDiagnosisResponse {

    private String diagnosisId;
    private String status;
    private String message;
}
