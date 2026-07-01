package com.growgate.publicdata.service;

import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PublicDataConnectorResult {

    private PublicDataProvider provider;
    private String endpoint;
    private String requestKey;
    private String hsCode;
    private String targetCountry;
    private String matchedKey;
    private String usedFor;
    private String rawPayload;
    private Map<String, Object> normalizedPayload;
    private Integer scoreContribution;
    private Double confidence;
    private String sourceType;
    private boolean fallbackUsed;
    private String errorMessage;
}
