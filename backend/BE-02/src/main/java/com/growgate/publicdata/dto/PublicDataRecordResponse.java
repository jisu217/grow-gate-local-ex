package com.growgate.publicdata.dto;

import com.growgate.publicdata.entity.PublicDataRecord;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicDataRecordResponse {

    private String recordId;
    private String runId;
    private String provider;
    private String endpoint;
    private String requestKey;
    private String hsCode;
    private String targetCountry;
    private String matchedKey;
    private String usedFor;
    private Integer scoreContribution;
    private boolean fallbackUsed;
    private boolean cacheHit;
    private String errorMessage;
    private String fetchedAt;
    private String expiresAt;
    private String normalizedPayload;

    public static PublicDataRecordResponse from(PublicDataRecord record) {
        return PublicDataRecordResponse.builder()
                .recordId(record.getId())
                .runId(record.getRun() != null ? record.getRun().getId() : null)
                .provider(record.getProvider().name())
                .endpoint(record.getEndpoint())
                .requestKey(record.getRequestKey())
                .hsCode(record.getHsCode())
                .targetCountry(record.getTargetCountry())
                .matchedKey(record.getMatchedKey())
                .usedFor(record.getUsedFor())
                .scoreContribution(record.getScoreContribution())
                .fallbackUsed(record.isFallbackUsed())
                .cacheHit(record.isCacheHit())
                .errorMessage(record.getErrorMessage())
                .fetchedAt(record.getFetchedAt() != null ? record.getFetchedAt().toString() : null)
                .expiresAt(record.getExpiresAt() != null ? record.getExpiresAt().toString() : null)
                .normalizedPayload(record.getNormalizedPayload())
                .build();
    }
}
