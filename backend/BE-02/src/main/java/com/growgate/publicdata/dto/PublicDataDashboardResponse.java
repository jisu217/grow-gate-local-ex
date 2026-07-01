package com.growgate.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PublicDataDashboardResponse {

    private int runCount;
    private int recordCount;
    private int successCount;
    private int fallbackCount;
    private int cacheHitCount;
    private double fallbackRatio;
    private double liveRatio;
    private double cacheRatio;
    private double averageConfidence;
    private int motieRelatedRecordCount;
    private int otherGovRecordCount;
    private String newestFetchedAt;
    private String oldestFetchedAt;
    private List<ProviderStatus> providers;

    @Getter
    @Builder
    public static class ProviderStatus {
        private String provider;
        private String agency;
        private String ministry;
        private String dataFamily;
        private boolean motieRelated;
        private String usedFor;
        private int refreshCycleHours;
        private int recordCount;
        private int liveCount;
        private int cacheCount;
        private int fallbackCount;
        private double fallbackRatio;
        private double averageConfidence;
        private String newestFetchedAt;
        private String newestExpiresAt;
        private boolean operationEndpointSeen;
        private boolean apiKeyConfiguredSeen;
    }
}
