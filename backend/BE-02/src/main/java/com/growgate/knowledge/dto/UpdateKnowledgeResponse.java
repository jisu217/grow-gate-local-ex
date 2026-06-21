package com.growgate.knowledge.dto;

// 제품 지식 수정 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateKnowledgeResponse {

    private String productId;
    private String updatedAt;
}
