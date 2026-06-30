package com.growgate.diagnosis.dto;

// 진단 실행 요청 DTO
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateDiagnosisRequest {

    private String productId;

    @NotBlank(message = "대상 국가는 필수입니다.")
    private String targetCountry;

    @Valid
    private InputData inputData;

    @Getter
    @NoArgsConstructor
    public static class InputData {
        @NotBlank(message = "제품명은 필수입니다.")
        @Size(max = 200, message = "제품명은 200자 이하여야 합니다.")
        private String productName;

        @NotBlank(message = "HS Code는 필수입니다.")
        @Pattern(regexp = "^\\d{6,10}$", message = "HS Code는 숫자 6~10자리여야 합니다.")
        private String hsCode;

        @Size(max = 2000, message = "제품 설명은 2000자 이하여야 합니다.")
        private String description;
    }
}
