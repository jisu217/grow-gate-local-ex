package com.growgate.publicdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicDataDailyRefreshResponse {

    private String trigger;
    private int plannedCount;
    private int collectedCount;
    private int skippedFreshCount;
    private int failedCount;
    private int successCount;
    private int fallbackCount;
    private int cacheHitCount;
    private int providerCount;
    private int externalDataCount;
    private boolean diagnosisUsable;
    private String availabilityLabel;
    private List<String> providers;
    private List<String> failureReasons;
    private String startedAt;
    private String finishedAt;
    private List<Item> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private String productName;
        private String ownerId;
        private String hsCode;
        private String targetCountry;
        private String status;
        private String runId;
        private int successCount;
        private int fallbackCount;
        private int cacheHitCount;
        private int providerCount;
        private int externalDataCount;
        private boolean diagnosisUsable;
        private String availabilityLabel;
        private String reason;
    }
}
