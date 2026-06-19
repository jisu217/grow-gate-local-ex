package com.growgate.knowledge.dto;

// 바이어 요구사항 수정 요청 DTO
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateBuyerRequirementRequest {

    private String buyerName;
    private String country;
    private String requirements;
    private String reason;
}
