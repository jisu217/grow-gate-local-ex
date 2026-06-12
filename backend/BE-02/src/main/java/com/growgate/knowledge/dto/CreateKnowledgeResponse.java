package com.growgate.knowledge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateKnowledgeResponse {

    private String productId;
    private String name;
    private String createdAt;
}
