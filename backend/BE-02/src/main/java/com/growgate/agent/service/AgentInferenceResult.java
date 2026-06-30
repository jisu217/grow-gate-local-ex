package com.growgate.agent.service;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AgentInferenceResult {
    private boolean succeeded;
    private String provider;
    private String model;
    private Map<String, Object> content;
    private List<Map<String, Object>> knowledgeSuggestions;
    private List<String> fallbackReasons;
    private long latencyMs;
    private String errorMessage;

    public static AgentInferenceResult success(String provider, String model, Map<String, Object> content,
                                               List<Map<String, Object>> knowledgeSuggestions, long latencyMs) {
        return AgentInferenceResult.builder()
                .succeeded(true)
                .provider(provider)
                .model(model)
                .content(content == null ? Map.of() : content)
                .knowledgeSuggestions(knowledgeSuggestions == null ? List.of() : knowledgeSuggestions)
                .fallbackReasons(List.of())
                .latencyMs(latencyMs)
                .build();
    }

    public static AgentInferenceResult failure(String provider, String errorMessage) {
        return AgentInferenceResult.builder()
                .succeeded(false)
                .provider(provider)
                .content(Map.of())
                .knowledgeSuggestions(List.of())
                .fallbackReasons(List.of())
                .errorMessage(errorMessage == null || errorMessage.isBlank() ? "unknown" : errorMessage)
                .build();
    }

    public AgentInferenceResult withFallbackReasons(List<String> reasons) {
        List<String> safeReasons = reasons == null ? List.of() : new ArrayList<>(reasons);
        return AgentInferenceResult.builder()
                .succeeded(succeeded)
                .provider(provider)
                .model(model)
                .content(content == null ? Map.of() : content)
                .knowledgeSuggestions(knowledgeSuggestions == null ? List.of() : knowledgeSuggestions)
                .fallbackReasons(safeReasons)
                .latencyMs(latencyMs)
                .errorMessage(errorMessage)
                .build();
    }
}
