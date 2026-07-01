package com.growgate.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenRouterAgentProviderClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void inferSendsBearerKeyOnlyInHeaderAndAddsAttributionHeaders() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenRouterAgentProviderClient client = configuredClient(restTemplate);
        when(restTemplate.postForObject(eq("https://openrouter.ai/api/v1/chat/completions"),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of(
                        "message", Map.of("content", "{\"summary\":\"backup ok\"}")))));

        AgentInferenceResult result = client.infer(request()).orElseThrow();

        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("https://openrouter.ai/api/v1/chat/completions"),
                entityCaptor.capture(), eq(Map.class));
        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();

        assertThat(result.isSucceeded()).isTrue();
        assertThat(entity.getHeaders().getFirst("Authorization")).startsWith("Bearer sk-or-v1-test");
        assertThat(entity.getHeaders().getFirst("HTTP-Referer")).isEqualTo("https://growgate.local");
        assertThat(entity.getHeaders().getFirst("X-Title")).isEqualTo("GrowGate Local EX");
        assertThat(entity.getBody()).containsEntry("model", "google/gemini-2.5-flash");
        assertThat(entity.getBody().toString()).doesNotContain("sk-or-v1-test");
    }

    @Test
    void inferRedactsOpenRouterKeyFromFailureReason() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenRouterAgentProviderClient client = configuredClient(restTemplate);
        when(restTemplate.postForObject(eq("https://openrouter.ai/api/v1/chat/completions"),
                any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException(
                        "401 Authorization: Bearer sk-or-v1-test-secret-token and key=sk-or-v1-test-secret-token"));

        AgentInferenceResult result = client.infer(request()).orElseThrow();

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.getErrorMessage()).contains("[REDACTED]");
        assertThat(result.getErrorMessage()).doesNotContain("sk-or-v1-test-secret-token");
    }

    @Test
    void isDisabledForNonOpenRouterEndpoint() {
        OpenRouterAgentProviderClient client = configuredClient(mock(RestTemplate.class));
        ReflectionTestUtils.setField(client, "url", "https://example.test/api");

        assertThat(client.isEnabled()).isFalse();
    }

    private OpenRouterAgentProviderClient configuredClient(RestTemplate restTemplate) {
        OpenRouterAgentProviderClient client = new OpenRouterAgentProviderClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "apiKey", "sk-or-v1-test-secret-token");
        ReflectionTestUtils.setField(client, "model", "google/gemini-2.5-flash");
        ReflectionTestUtils.setField(client, "url", "https://openrouter.ai/api/v1/chat/completions");
        ReflectionTestUtils.setField(client, "siteUrl", "https://growgate.local");
        ReflectionTestUtils.setField(client, "appTitle", "GrowGate Local EX");
        return client;
    }

    private AgentInferenceRequest request() {
        return AgentInferenceRequest.builder()
                .diagnosisId("diagnosis-1")
                .productName("Battery Pack")
                .hsCode("850760")
                .targetCountry("DE")
                .ragContext(Map.of("knowledge", "FTO review needed"))
                .scoringContext(Map.of("overall", 72))
                .build();
    }
}
