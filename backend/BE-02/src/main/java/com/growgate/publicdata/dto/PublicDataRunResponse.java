package com.growgate.publicdata.dto;

import com.growgate.publicdata.entity.PublicDataCollectionRun;
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
                .errorSummary(run.getErrorSummary())
                .startedAt(run.getStartedAt() != null ? run.getStartedAt().toString() : null)
                .finishedAt(run.getFinishedAt() != null ? run.getFinishedAt().toString() : null)
                .build();
    }
}
