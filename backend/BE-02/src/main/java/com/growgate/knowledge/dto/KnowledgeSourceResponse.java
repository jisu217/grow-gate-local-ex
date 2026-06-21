package com.growgate.knowledge.dto;

// 지식 출처 응답 DTO
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KnowledgeSourceResponse {

    private String id;
    private String productId;
    private String url;
    private String documentName;
    private String retrievedAt;
}
