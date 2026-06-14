package com.growgate.knowledge.dto;

// 제품 지식 삭제 응답 DTO
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteKnowledgeResponse {

    private String message;
    private String productId;
}
