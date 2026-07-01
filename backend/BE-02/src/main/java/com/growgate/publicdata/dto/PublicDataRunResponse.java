package com.growgate.publicdata.dto;

import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.common.DateTimeUtils;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicDataRunResponse {

    private String runId;
    private String diagnosisId;
    private String hsCode;
    private String targetCountry;
    private String productName;
    private String status;
    private int providerCount;
    private int successCount;
    private int fallbackCount;
    private int cacheHitCount;
    private double fallbackRatio;
    private double liveRatio;
    private String errorSummary;
    private String startedAt;
    private String finishedAt;

    public static PublicDataRunResponse from(PublicDataCollectionRun run) {
        return PublicDataRunResponse.builder()
                .runId(run.getId())
                .diagnosisId(run.getDiagnosisId())
                .hsCode(run.getHsCode())
                .targetCountry(run.getTargetCountry())
                .productName(run.getProductName())
                .status(run.getStatus().name())
                .providerCount(run.getProviderCount())
                .successCount(run.getSuccessCount())
                .fallbackCount(run.getFallbackCount())
                .cacheHitCount(run.getCacheHitCount())
                .fallbackRatio(ratio(run.getFallbackCount(), run.getProviderCount()))
                .liveRatio(ratio(run.getSuccessCount(), run.getProviderCount()))
                .errorSummary(run.getErrorSummary())
                .startedAt(DateTimeUtils.formatUtc(run.getStartedAt()))
                .finishedAt(DateTimeUtils.formatUtc(run.getFinishedAt()))
                .build();
    }

    private static double ratio(int value, int total) {
        if (total <= 0) return 0.0;
        return Math.round((value * 10000.0 / total)) / 10000.0;
    }
}
