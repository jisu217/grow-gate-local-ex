package com.growgate.publicdata.service;

import lombok.Getter;

@Getter
public class PublicDataRequest {

    private String diagnosisId;
    private String userId;
    private String hsCode;
    private String targetCountry;
    private String productName;
    private String lawSearchQuery;

    public PublicDataRequest(String diagnosisId, String userId, String hsCode, String targetCountry, String productName) {
        this(diagnosisId, userId, hsCode, targetCountry, productName, null);
    }

    public PublicDataRequest(String diagnosisId, String userId, String hsCode, String targetCountry,
                             String productName, String lawSearchQuery) {
        this.diagnosisId = diagnosisId;
        this.userId = userId;
        this.hsCode = hsCode;
        this.targetCountry = targetCountry;
        this.productName = productName;
        this.lawSearchQuery = lawSearchQuery;
    }
}
