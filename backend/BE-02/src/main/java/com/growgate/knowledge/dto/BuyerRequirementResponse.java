package com.growgate.knowledge.dto;

// 바이어 요구사항 생성/수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BuyerRequirementResponse {

    private String id;
    private String productId;
    private String buyerName;
    private String country;
    private String requirements;
}
