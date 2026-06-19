package com.growgate.knowledge.dto;

// 인증서 수정 요청 DTO
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateCertificationRequest {

    private String name;
    private String issuedAt;
    private String expiresAt;
    private Integer renewalCycle;
    private String reason;
}
