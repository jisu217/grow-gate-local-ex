package com.growgate.knowledge.dto;

// 제품 지식 생성 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateKnowledgeResponse {

    private String productId;
    private String name;
    private String createdAt;
}
