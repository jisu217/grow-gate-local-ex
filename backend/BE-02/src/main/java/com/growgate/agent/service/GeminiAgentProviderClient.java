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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Order(1)
public class GeminiAgentProviderClient extends AbstractLlmAgentProviderClient {

    private final RestTemplate restTemplate;

    @Value("${app.agent.gemini.enabled:false}")
    private boolean enabled;

    @Value("${app.agent.gemini.api-key:}")
    private String apiKey;

    @Value("${app.agent.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${app.agent.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    public GeminiAgentProviderClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.restTemplate = restTemplate;
    }

    @Override
    public String providerName() {
        return "gemini";
    }

    @Override
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank() && isAllowedEndpoint();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<AgentInferenceResult> infer(AgentInferenceRequest request) {
        if (!isEnabled()) return Optional.empty();
        long started = System.currentTimeMillis();
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/models/" + model + ":generateContent")
                    .build(true)
                    .toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("x-goog-api-key", apiKey);
            Map<String, Object> body = Map.of("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", buildPrompt(request)))
            )));
            Map<String, Object> response = restTemplate.postForObject(uri, new HttpEntity<>(body, headers), Map.class);
            String text = extractGeminiText(response);
            return Optional.of(parseTextResult(providerName(), model, text, System.currentTimeMillis() - started));
        } catch (IllegalArgumentException | RestClientException e) {
            String safeMessage = LlmSecretSanitizer.sanitize(e.getMessage());
            log.warn("Gemini agent provider failed: {}", safeMessage);
            return Optional.of(AgentInferenceResult.failure(providerName(), safeMessage));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> response) {
        if (response == null) return "";
        Object candidates = response.get("candidates");
        if (!(candidates instanceof List<?>) || ((List<?>) candidates).isEmpty()) return "";
        Object first = ((List<?>) candidates).get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object content = firstMap.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) return "";
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?>) || ((List<?>) parts).isEmpty()) return "";
        Object firstPart = ((List<?>) parts).get(0);
        if (firstPart instanceof Map<?, ?> partMap && partMap.get("text") != null) {
            return String.valueOf(partMap.get("text"));
        }
        return "";
    }

    private boolean isAllowedEndpoint() {
        try {
            URI uri = new URI(baseUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "generativelanguage.googleapis.com".equalsIgnoreCase(uri.getHost());
        } catch (URISyntaxException | IllegalArgumentException e) {
            log.warn("Gemini agent provider disabled because endpoint URL is invalid");
            return false;
        }
    }
}
