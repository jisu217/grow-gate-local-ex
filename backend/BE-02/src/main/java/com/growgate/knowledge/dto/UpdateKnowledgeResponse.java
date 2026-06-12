package com.growgate.knowledge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateKnowledgeResponse {

    private String productId;
    private String updatedAt;
}
