package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KiatTechBankPatentConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReturnsEmptyWhenApiKeyIsMissing() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KiatTechBankPatentConnector connector = new KiatTechBankPatentConnector(restTemplate, objectMapper);
        configure(connector, "");

        PublicDataConnectorResult result = connector.collect(request("배터리 보호 기술"));

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");
        Map<String, Object> diagnostics = (Map<String, Object>) normalized.get("zeroResultDiagnostics");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KIAT_TECH_BANK_PATENT);
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(normalized.get("zeroReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(evidence.get("operationEndpoint")).isEqualTo(true);
        assertThat(evidence.get("apiKeyConfigured")).isEqualTo(false);
        assertThat(evidence.get("apiKeyStored")).isEqualTo(false);
        assertThat(diagnostics.get("sourceType")).isEqualTo("empty");
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesConfiguredGatewayEndpointAndMasksServiceKey() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"response":{"body":{"items":{"item":[
                          {"techName":"배터리 보호 기술","applicationNumber":"10-2026-0000001","patentTitle":"배터리 보호 회로"},
                          {"techName":"전력 변환 기술","applicationNumber":"10-2026-0000002","patentTitle":"전력 변환 장치"}
                        ]},"totalCount":2}}}"""));
        KiatTechBankPatentConnector connector = new KiatTechBankPatentConnector(restTemplate, objectMapper);
        configure(connector, "test-key");

        PublicDataConnectorResult result = connector.collect(request("배터리"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String uri = uriCaptor.getValue().toString();

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, String> maskedParams = (Map<String, String>) normalized.get("requestParamsMasked");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(uri).contains("https://apis.data.go.kr/B552536/tech_3");
        assertThat(uri).contains("pageNo=1");
        assertThat(uri).contains("numOfRows=10");
        assertThat(uri).contains("type=json");
        assertThat(uri).contains("serviceKey=test-key");
        assertThat(maskedParams.get("serviceKey")).isEqualTo("[REDACTED]");
        assertThat(maskedParams).doesNotContainKey("ServiceKey");
        assertThat(metrics.get("patentRecordCount")).isEqualTo(2);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productMentions")).isEqualTo(2);
        assertThat(metrics.get("applicationNumberCount")).isEqualTo(2);
        assertThat(metrics.get("technologyTitleCount")).isEqualTo(4);
        assertThat(result.getScoreContribution()).isLessThan(0);
        assertThat(evidence.get("collectionMode")).isEqualTo("live_operation_api");
        assertThat(evidence.get("unauthenticatedSmokeResult")).isEqualTo("Unexpected errors");
    }

    private void configure(KiatTechBankPatentConnector connector, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint", "https://apis.data.go.kr/B552536/tech_3");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "keyParam", "serviceKey");
        ReflectionTestUtils.setField(connector, "pageSize", 10);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "KR", product);
    }
}
