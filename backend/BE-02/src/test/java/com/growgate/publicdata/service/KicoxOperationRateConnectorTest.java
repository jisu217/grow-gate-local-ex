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

class KicoxOperationRateConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReadsCsvFirstWhenApiKeyIsMissingEvenAfterUpdateDueDate() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KicoxOperationRateConnector connector = new KicoxOperationRateConnector(restTemplate, objectMapper);
        configure(connector, "", "2026-07-05");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KICOX_OPERATION_RATE);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(40);
        assertThat(metrics.get("validRateRowCount")).isEqualTo(34);
        assertThat(metrics.get("averageOperationRatePct")).isEqualTo(78.566);
        assertThat(metrics.get("below70RateComplexCount")).isEqualTo(4);
        assertThat(metrics.get("highestRateComplex")).isEqualTo("대구");
        assertThat(metrics.get("lowestRateComplex")).isEqualTo("대불(외)");
        assertThat(evidence.get("collectionMode")).isEqualTo("csv_first_until_scheduled_update");
        assertThat(evidence.get("updateDue")).isEqualTo(true);
        assertThat(evidence.get("apiRefreshEligible")).isEqualTo(false);
        assertThat(evidence.get("apiRefreshSkippedReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(evidence.get("sameAsDataGoKrMetadata")).isEqualTo(true);
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesApiWhenUpdateDueAndApiKeyConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":100,"totalCount":2,"currentCount":2,"data":[
                          {"산업단지":"서울","제조업가동업체수(개사)":"3992","생산(백만원)_최대생산능력":"5389366.581","생산(백만원)_당분기생산액":"4318948.017","가동률(퍼센트)_당분기":"80.13832334","가동률(퍼센트)_전분기":"80.63653193","가동률(퍼센트)_전분기대비(퍼센트포인트)":"-0.498208587"},
                          {"산업단지":"대불(외)","제조업가동업체수(개사)":"23","생산(백만원)_최대생산능력":"130987.4","생산(백만원)_당분기생산액":"61770.8","가동률(퍼센트)_당분기":"47.15781823","가동률(퍼센트)_전분기":"79.32503604","가동률(퍼센트)_전분기대비(퍼센트포인트)":"-32.16721781"}
                        ]}"""));
        KicoxOperationRateConnector connector = new KicoxOperationRateConnector(restTemplate, objectMapper);
        configure(connector, "test-key", "2026-07-05");

        PublicDataConnectorResult result = connector.collect(request());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String uri = uriCaptor.getValue().toString();

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, String> maskedParams = (Map<String, String>) normalized.get("requestParamsMasked");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(uri).contains("https://api.odcloud.kr/api/15085889/v1/uddi:e32e7d2e-82a3-4cde-9d24-f3e90e2b1eb6");
        assertThat(uri).contains("page=1");
        assertThat(uri).contains("perPage=100");
        assertThat(uri).contains("returnType=JSON");
        assertThat(uri).contains("serviceKey=test-key");
        assertThat(maskedParams.get("serviceKey")).isEqualTo("[REDACTED]");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("validRateRowCount")).isEqualTo(2);
        assertThat(metrics.get("averageOperationRatePct")).isEqualTo(63.648);
        assertThat(metrics.get("below70RateComplexCount")).isEqualTo(1);
        assertThat(result.getScoreContribution()).isEqualTo(0);
        assertThat(evidence.get("collectionMode")).isEqualTo("api_after_scheduled_update");
        assertThat(evidence.get("updateDue")).isEqualTo(true);
    }

    private void configure(KicoxOperationRateConnector connector, String apiKey, String todayOverride) {
        ReflectionTestUtils.setField(connector, "endpoint", "https://api.odcloud.kr/api/15085889/v1/uddi:e32e7d2e-82a3-4cde-9d24-f3e90e2b1eb6");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "csvResource", "classpath:publicdata/kicox-national-industrial-complex-operation-rate-20260331.csv");
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-03-31");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-06-30");
        ReflectionTestUtils.setField(connector, "swaggerLatestDatasetDate", "2026-03-31");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 40);
        ReflectionTestUtils.setField(connector, "apiPerPage", 100);
        ReflectionTestUtils.setField(connector, "todayOverride", todayOverride);
    }

    private PublicDataRequest request() {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "US", "배터리팩");
    }
}
