package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractPublicDataConnectorTest {

    @Test
    void failedRequestRedactsApiKeyFromFallbackPayloadAndErrorMessage() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenThrow(new RestClientException(
                        "500 from https://api.example.test/data?hs=850760&serviceKey=secret-key-123&apiKey=secret-api-key"));

        TestConnector connector = new TestConnector(restTemplate, new ObjectMapper());

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        assertThat(result.isFallbackUsed()).isTrue();
        assertThat(result.getErrorMessage())
                .contains("serviceKey=[REDACTED]")
                .contains("apiKey=[REDACTED]")
                .doesNotContain("secret-key-123")
                .doesNotContain("secret-api-key");
        assertThat(result.getRawPayload())
                .contains("[REDACTED]")
                .doesNotContain("secret-key-123")
                .doesNotContain("secret-api-key");
        assertThat(result.getNormalizedPayload().get("reason").toString())
                .doesNotContain("secret-key-123")
                .doesNotContain("secret-api-key");
    }

    @Test
    @SuppressWarnings("unchecked")
    void successPayloadStoresDomainSignalsAndEvidenceBasedContribution() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                .thenReturn("{\"items\":[{\"hsCode\":\"850760\",\"country\":\"DE\",\"amount\":1200,\"qty\":3}],"
                        + "\"product\":\"Battery pack\"}");

        TestConnector connector = new TestConnector(restTemplate, new ObjectMapper());

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        Map<String, Object> signals = (Map<String, Object>) result.getNormalizedPayload().get("domainSignals");
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(signals.get("containsHsCode")).isEqualTo(true);
        assertThat(signals.get("containsTargetCountry")).isEqualTo(true);
        assertThat(signals.get("containsProductName")).isEqualTo(true);
        assertThat(((Number) signals.get("evidenceStrength")).intValue()).isGreaterThanOrEqualTo(4);
        assertThat(((Number) metrics.get("recordCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(result.getScoreContribution()).isEqualTo(8);
        assertThat(result.getConfidence()).isGreaterThan(0.0);
    }

    private static class TestConnector extends AbstractPublicDataConnector {

        private TestConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
            super(restTemplate, objectMapper);
        }

        @Override
        public PublicDataProvider provider() {
            return PublicDataProvider.KCS_TRADE;
        }

        @Override
        public String usedFor() {
            return "market";
        }

        @Override
        public String requestKey(PublicDataRequest request) {
            return "KCS_TRADE:" + request.getHsCode() + ":" + request.getTargetCountry();
        }

        @Override
        public PublicDataConnectorResult collect(PublicDataRequest request) {
            return getJson(request, "https://api.example.test/data", "secret-key-123", Map.of("hs", request.getHsCode()));
        }

        @Override
        protected String endpointLabel() {
            return "test";
        }
    }
}
