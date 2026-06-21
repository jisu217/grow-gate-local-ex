package com.growgate.knowledge.dto;

// 지식 출처 등록 요청 DTO
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateKnowledgeSourceRequest {

    private String url;
    private String documentName;
    private String retrievedAt;
    private String reason;
}
