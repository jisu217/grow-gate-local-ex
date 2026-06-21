package com.growgate.knowledge.dto;

// FTO 생성/수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FtoResponse {

    private String id;
    private String productId;
    private String targetCountry;
    private String riskLevel;
    private String summary;
    private String expiresAt;
}
