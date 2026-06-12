package com.growgate.knowledge.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KnowledgeListResponse {

    private List<ProductSummary> products;

    @Getter
    @Builder
    public static class ProductSummary {
        private String productId;
        private String name;
        private String category;
        private String trustGrade;
        private String knowledgeStatus;  // valid | expiring_soon | expired | needs_review | incomplete
        private int expiringCertCount;
        private int missingFieldCount;
        private String updatedAt;
    }
}
