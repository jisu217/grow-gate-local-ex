package com.growgate.knowledge.dto;

// 인증서 생성/수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationResponse {

    private String id;
    private String productId;
    private String name;
    private String issuedAt;
    private String expiresAt;
    private Integer renewalCycle;
    private long daysUntilExpiry;
}
