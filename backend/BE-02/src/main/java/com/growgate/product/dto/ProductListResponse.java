package com.growgate.product.dto;

// 제품 목록 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductListResponse {

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
