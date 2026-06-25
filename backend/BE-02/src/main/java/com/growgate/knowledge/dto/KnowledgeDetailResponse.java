package com.growgate.knowledge.dto;

// 제품 지식 상세 조회 응답 DTO
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class KnowledgeDetailResponse {

    private String productId;
    private String name;
    private String category;
    private String hsCode;
    private String targetCountry;
    private String description;
    private KnowledgeData knowledge;
    private List<CertificationData> certifications;
    private List<FtoData> fto;
    private List<BuyerRequirementData> buyerRequirements;
    private List<KnowledgeSourceData> knowledgeSources;
    private String updatedAt;

    @Getter
    @Builder
    public static class KnowledgeData {
        private String trustGrade;
        private String sourceType;
        private String notes;
    }

    @Getter
    @Builder
    public static class KnowledgeSourceData {
        private String id;
        private String url;
        private String documentName;
        private String retrievedAt;
    }

    @Getter
    @Builder
    public static class CertificationData {
        private String id;
        private String name;
        private String issuedAt;
        private String expiresAt;
        private Integer renewalCycle;
        private long daysUntilExpiry;
    }

    @Getter
    @Builder
    public static class FtoData {
        private String id;
        private String targetCountry;
        private String riskLevel;
        private String summary;
        private String expiresAt;
    }

    @Getter
    @Builder
    public static class BuyerRequirementData {
        private String id;
        private String buyerName;
        private String country;
        private String requirements;
    }
}
