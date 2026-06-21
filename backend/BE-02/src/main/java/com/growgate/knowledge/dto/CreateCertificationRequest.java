package com.growgate.knowledge.dto;

// 인증서 등록 요청 DTO
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCertificationRequest {

    @NotBlank(message = "인증서명은 필수입니다.")
    private String name;

    private String issuedAt;
    private String expiresAt;
    private Integer renewalCycle;
    private String reason;
}
