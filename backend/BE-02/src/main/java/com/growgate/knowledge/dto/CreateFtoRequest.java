package com.growgate.knowledge.dto;

// FTO 등록 요청 DTO
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateFtoRequest {

    @NotBlank(message = "대상 국가는 필수입니다.")
    private String targetCountry;

    private String riskLevel;
    private String summary;
    private String expiresAt;
    private String reason;
}
