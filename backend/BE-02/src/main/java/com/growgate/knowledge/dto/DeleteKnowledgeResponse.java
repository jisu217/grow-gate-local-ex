package com.growgate.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeleteKnowledgeResponse {

    private String message;
    private String productId;
}
