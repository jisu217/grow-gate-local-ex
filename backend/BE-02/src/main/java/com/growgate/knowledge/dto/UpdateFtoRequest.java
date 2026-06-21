package com.growgate.knowledge.dto;

// FTO 수정 요청 DTO
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateFtoRequest {

    private String targetCountry;
    private String riskLevel;
    private String summary;
    private String expiresAt;
    private String reason;
}
