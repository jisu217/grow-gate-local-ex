package com.growgate.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiAgentProviderClientTest {

    private static final String MASKED_GOOGLE_KEY = "GOOGLE_API_KEY_FOR_TEST_ONLY";
    private static final String MASKED_OAUTH_TOKEN = "GOOGLE_OAUTH_TOKEN_FOR_TEST_ONLY";

    @Test
    @SuppressWarnings("unchecked")
    void inferSendsGoogleApiKeyOnlyInHeader() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiAgentProviderClient client = configuredClient(restTemplate);
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", "{\"summary\":\"gemini ok\"}")))))));

        AgentInferenceResult result = client.infer(request()).orElseThrow();

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(uriCaptor.capture(), entityCaptor.capture(), eq(Map.class));

        URI uri = uriCaptor.getValue();
        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();

        assertThat(result.isSucceeded()).isTrue();
        assertThat(uri.toString()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
        assertThat(uri.toString()).doesNotContain("AIza");
        assertThat(entity.getHeaders().getFirst("x-goog-api-key")).isEqualTo(MASKED_GOOGLE_KEY);
        assertThat(entity.getBody().toString()).doesNotContain("AIza");
        assertThat(entity.getBody()).containsKey("generationConfig");
        assertThat(entity.getBody().get("generationConfig").toString()).contains("maxOutputTokens");
    }

    @Test
    void inferRedactsGoogleKeyAndTokenFromFailureReason() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiAgentProviderClient client = configuredClient(restTemplate);
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException(
                        "403 key=" + MASKED_GOOGLE_KEY + " token=" + MASKED_OAUTH_TOKEN));

        AgentInferenceResult result = client.infer(request()).orElseThrow();

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.getErrorMessage()).contains("[REDACTED]");
        assertThat(result.getErrorMessage()).doesNotContain(MASKED_GOOGLE_KEY);
        assertThat(result.getErrorMessage()).doesNotContain(MASKED_OAUTH_TOKEN);
    }

    @Test
    void isDisabledForNonGoogleEndpoint() {
        GeminiAgentProviderClient client = configuredClient(mock(RestTemplate.class));
        ReflectionTestUtils.setField(client, "baseUrl", "https://example.test/v1beta");

        assertThat(client.isEnabled()).isFalse();
    }

    private GeminiAgentProviderClient configuredClient(RestTemplate restTemplate) {
        GeminiAgentProviderClient client = new GeminiAgentProviderClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "enabled", true);
        ReflectionTestUtils.setField(client, "apiKey", MASKED_GOOGLE_KEY);
        ReflectionTestUtils.setField(client, "model", "gemini-2.5-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");
        ReflectionTestUtils.setField(client, "maxOutputTokens", 1024);
        return client;
    }

    private AgentInferenceRequest request() {
        return AgentInferenceRequest.builder()
                .userId("user-1")
                .diagnosisId("diagnosis-1")
                .productName("Battery Pack")
                .hsCode("850760")
                .targetCountry("DE")
                .evidenceContext(Map.of("knowledge", "FTO review needed"))
                .scoringContext(Map.of("overall", 72))
                .build();
    }
}
