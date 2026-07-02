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

import java.net.URI;
import java.net.URISyntaxException;
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

    @Value("${app.agent.openrouter.site-url:}")
    private String siteUrl;

    @Value("${app.agent.openrouter.app-title:GrowGate Local EX}")
    private String appTitle;

    @Value("${app.agent.budget.max-output-tokens:1024}")
    private int maxOutputTokens;

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
        return enabled && apiKey != null && !apiKey.isBlank() && isAllowedEndpoint();
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
            if (siteUrl != null && !siteUrl.isBlank()) {
                headers.add("HTTP-Referer", siteUrl);
            }
            headers.add("X-Title", appTitle == null || appTitle.isBlank() ? "GrowGate Local EX" : appTitle);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", buildPrompt(request))),
                    "temperature", 0.2,
                    "max_tokens", Math.max(256, maxOutputTokens)
            );
            Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            String text = extractOpenRouterText(response);
            return Optional.of(parseTextResult(providerName(), model, text, System.currentTimeMillis() - started));
        } catch (IllegalArgumentException | RestClientException e) {
            String safeMessage = LlmSecretSanitizer.sanitize(e.getMessage());
            log.warn("OpenRouter agent provider failed: {}", safeMessage);
            return Optional.of(AgentInferenceResult.failure(providerName(), safeMessage));
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

    private boolean isAllowedEndpoint() {
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "openrouter.ai".equalsIgnoreCase(uri.getHost());
        } catch (URISyntaxException | IllegalArgumentException e) {
            log.warn("OpenRouter agent provider disabled because endpoint URL is invalid");
            return false;
        }
    }
}
