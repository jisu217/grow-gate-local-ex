package com.growgate.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PublicDataSignal {

    private String provider;
    private String usedFor;
    private String hsCode;
    private String targetCountry;
    private Map<String, Object> metrics;
    private int scoreContribution;
    private double confidence;
}
