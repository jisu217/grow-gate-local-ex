package com.growgate.diagnosis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateDiagnosisRequest {

    private String productId;

    @NotBlank(message = "대상 국가는 필수입니다.")
    private String targetCountry;

    private InputData inputData;

    @Getter
    @NoArgsConstructor
    public static class InputData {
        private String productName;
        private String hsCode;
    }
}
