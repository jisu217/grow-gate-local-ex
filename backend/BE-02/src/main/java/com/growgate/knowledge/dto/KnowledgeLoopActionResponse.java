package com.growgate.knowledge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KnowledgeLoopActionResponse {
    private String itemId;
    private String status;
    private String message;
    private String sourceId;
}
