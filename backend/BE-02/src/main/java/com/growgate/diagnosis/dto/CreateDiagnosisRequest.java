package com.growgate.diagnosis.dto;

// 진단 실행 요청 DTO
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
        @Size(max = 200, message = "제품명은 200자 이하여야 합니다.")
        private String productName;

        @Pattern(regexp = "^\\d{6,10}$", message = "HS Code는 숫자 6~10자리여야 합니다.")
        private String hsCode;

        @Size(max = 2000, message = "제품 설명은 2000자 이하여야 합니다.")
        private String description;

        @Size(max = 1000, message = "바이어 문의 내용은 1000자 이하여야 합니다.")
        private String buyerRequest;

        private LocalDate buyerDeadline;

        private LocalDate expectedShipmentDate;

        private List<@Size(max = 80, message = "요구 인증명은 80자 이하여야 합니다.") String> requestedCertifications;

        private List<@Size(max = 80, message = "보유 문서명은 80자 이하여야 합니다.") String> currentDocuments;

        @Size(max = 40, message = "수출 경험 값은 40자 이하여야 합니다.")
        private String exportExperience;

        private Long budgetLimitKrw;
    }
}
