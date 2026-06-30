package com.growgate.diagnosis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiAgentClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.fastapi.enabled:false}")
    private boolean enabled;

    @Value("${app.fastapi.base-url:http://localhost:8000}")
    private String baseUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<Map<String, Object>> analyze(Map<String, Object> payload) {
        return postForMap("/analyze", payload);
    }

    public Optional<Map<String, Object>> report(Map<String, Object> payload) {
        return postForMap("/report", payload);
    }

    private Optional<Map<String, Object>> postForMap(String path, Map<String, Object> payload) {
        if (!enabled) return Optional.empty();

        try {
            Object response = restTemplate.postForObject(normalizedBaseUrl() + path, payload, Object.class);
            if (response == null) return Optional.empty();
            return Optional.of(objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {}));
        } catch (IllegalArgumentException | RestClientException e) {
            log.warn("FastAPI agent call failed: {}{}", normalizedBaseUrl(), path, e);
            return Optional.empty();
        }
    }

    public Map<String, Object> status(boolean called, boolean succeeded, String reason) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", enabled);
        status.put("called", called);
        status.put("succeeded", succeeded);
        status.put("baseUrl", normalizedBaseUrl());
        status.put("fallbackUsed", !succeeded);
        status.put("reason", reason);
        return status;
    }

    private String normalizedBaseUrl() {
        return baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }
}
