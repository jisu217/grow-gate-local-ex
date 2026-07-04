package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractPublicDataConnectorTest {

    @BeforeEach
    @SuppressWarnings("unchecked")
    void resetCircuitState() throws Exception {
        Field circuits = AbstractPublicDataConnector.class.getDeclaredField("CIRCUITS");
        circuits.setAccessible(true);
        ((Map<PublicDataProvider, ?>) circuits.get(null)).clear();
    }

    @Test
    void failedRequestRedactsApiKeyFromFallbackPayloadAndErrorMessage() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
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
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"items\":[{\"hsCode\":\"850760\",\"country\":\"DE\",\"amount\":1200,\"qty\":3}],"
                        + "\"product\":\"Battery pack\"}"));

        TestConnector connector = new TestConnector(restTemplate, new ObjectMapper());

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        Map<String, Object> signals = (Map<String, Object>) result.getNormalizedPayload().get("domainSignals");
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        Map<String, Object> diagnostics = (Map<String, Object>) result.getNormalizedPayload().get("zeroResultDiagnostics");
        Map<String, String> maskedParams = (Map<String, String>) result.getNormalizedPayload().get("requestParamsMasked");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(signals.get("containsHsCode")).isEqualTo(true);
        assertThat(signals.get("containsTargetCountry")).isEqualTo(true);
        assertThat(signals.get("containsProductName")).isEqualTo(true);
        assertThat(((Number) signals.get("evidenceStrength")).intValue()).isGreaterThanOrEqualTo(4);
        assertThat(((Number) metrics.get("recordCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(result.getNormalizedPayload().get("zeroReason")).isEqualTo("");
        assertThat(((Number) result.getNormalizedPayload().get("rawCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) result.getNormalizedPayload().get("normalizedCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(diagnostics.get("sourceType")).isEqualTo("live");
        assertThat(maskedParams.get("serviceKey")).isEqualTo("[REDACTED]");
        assertThat(maskedParams.get("hs")).isEqualTo("850760");
        assertThat(result.getScoreContribution()).isEqualTo(8);
        assertThat(result.getConfidence()).isGreaterThan(0.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyLiveResponseStoresZeroReasonWithoutUsingFallback() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class))).thenReturn(ResponseEntity.ok("{\"items\":[]}"));

        TestConnector connector = new TestConnector(restTemplate, new ObjectMapper());

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        Map<String, Object> diagnostics = (Map<String, Object>) result.getNormalizedPayload().get("zeroResultDiagnostics");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.getNormalizedPayload().get("zeroReason")).isEqualTo("EMPTY_LIVE_RESPONSE");
        assertThat(result.getNormalizedPayload().get("rawCount")).isEqualTo(0);
        assertThat(result.getNormalizedPayload().get("normalizedCount")).isEqualTo(0);
        assertThat(diagnostics.get("sourceType")).isEqualTo("empty");
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingApiKeyStoresZeroReasonAndDoesNotCallHttp() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TestConnector connector = new TestConnector(restTemplate, new ObjectMapper(), "");

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        Map<String, Object> diagnostics = (Map<String, Object>) result.getNormalizedPayload().get("zeroResultDiagnostics");
        Map<String, String> maskedParams = (Map<String, String>) result.getNormalizedPayload().get("requestParamsMasked");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.getErrorMessage()).contains("API_KEY_NOT_CONFIGURED");
        assertThat(result.getNormalizedPayload().get("zeroReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(diagnostics.get("sourceType")).isEqualTo("empty");
        assertThat(maskedParams.get("serviceKey")).isEqualTo("");
    }

    private static class TestConnector extends AbstractPublicDataConnector {

        private final String apiKey;

        private TestConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
            this(restTemplate, objectMapper, "secret-key-123");
        }

        private TestConnector(RestTemplate restTemplate, ObjectMapper objectMapper, String apiKey) {
            super(restTemplate, objectMapper);
            this.apiKey = apiKey;
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
            return getJson(request, "https://api.example.test/data", apiKey, Map.of("hs", request.getHsCode()));
        }

        @Override
        protected String endpointLabel() {
            return "test";
        }
    }
}
