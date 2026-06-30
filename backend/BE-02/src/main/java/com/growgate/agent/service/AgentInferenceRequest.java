package com.growgate.agent.service;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AgentInferenceRequest {
    private String diagnosisId;
    private String productName;
    private String hsCode;
    private String targetCountry;
    private Map<String, Object> ragContext;
    private Map<String, Object> scoringContext;
}
