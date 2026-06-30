package com.growgate.publicdata.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicDataRequest {

    private String diagnosisId;
    private String userId;
    private String hsCode;
    private String targetCountry;
    private String productName;
}
