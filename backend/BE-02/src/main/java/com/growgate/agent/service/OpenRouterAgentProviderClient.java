package com.growgate.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Order(2)
public class OpenRouterAgentProviderClient extends AbstractLlmAgentProviderClient {

    private final RestTemplate restTemplate;

    @Value("${app.agent.openrouter.enabled:false}")
    private boolean enabled;

    @Value("${app.agent.openrouter.api-key:}")
    private String apiKey;

    @Value("${app.agent.openrouter.model:google/gemini-2.5-flash}")
    private String model;

    @Value("${app.agent.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String url;

    public OpenRouterAgentProviderClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerName() {
        return "openrouter";
    }

    @Override
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<AgentInferenceResult> infer(AgentInferenceRequest request) {
        if (!isEnabled()) return Optional.empty();
        long started = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.add("X-Title", "GrowGate Local EX");
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(request)))
            );
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            String text = extractOpenRouterText(response);
            return Optional.of(parseTextResult(providerName(), model, text, System.currentTimeMillis() - started));
        } catch (IllegalArgumentException | RestClientException e) {
            log.warn("OpenRouter agent provider failed: {}", sanitize(e.getMessage()));
            return Optional.of(AgentInferenceResult.failure(providerName(), sanitize(e.getMessage())));
        }
    }

    private String extractOpenRouterText(Map<String, Object> response) {
        if (response == null) return "";
        Object choices = response.get("choices");
        if (!(choices instanceof List<?>) || ((List<?>) choices).isEmpty()) return "";
        Object first = ((List<?>) choices).get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object message = firstMap.get("message");
        if (message instanceof Map<?, ?> messageMap && messageMap.get("content") != null) {
            return String.valueOf(messageMap.get("content"));
        }
        return "";
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) return "unknown";
        return message.replaceAll("(?i)(Bearer\\s+)[A-Za-z0-9._\\-]+", "$1[REDACTED]");
    }
}
