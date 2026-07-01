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
        assertThat(entity.getHeaders().getFirst("x-goog-api-key")).startsWith("AIza");
        assertThat(entity.getBody().toString()).doesNotContain("AIza");
    }

    @Test
    void inferRedactsGoogleKeyAndTokenFromFailureReason() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiAgentProviderClient client = configuredClient(restTemplate);
        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException(
                        "403 key=AIzaSyTestSecretToken1234567890 token=AQ.Ab8TestSecretToken1234567890"));

        AgentInferenceResult result = client.infer(request()).orElseThrow();

        assertThat(result.isSucceeded()).isFalse();
        assertThat(result.getErrorMessage()).contains("[REDACTED]");
        assertThat(result.getErrorMessage()).doesNotContain("AIzaSyTestSecretToken1234567890");
        assertThat(result.getErrorMessage()).doesNotContain("AQ.Ab8TestSecretToken1234567890");
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
        ReflectionTestUtils.setField(client, "apiKey", "AIzaSyTestSecretToken1234567890");
        ReflectionTestUtils.setField(client, "model", "gemini-2.5-flash");
        ReflectionTestUtils.setField(client, "baseUrl", "https://generativelanguage.googleapis.com/v1beta");
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
