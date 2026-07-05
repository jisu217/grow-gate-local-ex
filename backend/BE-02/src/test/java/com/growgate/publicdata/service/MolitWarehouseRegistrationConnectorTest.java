package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MolitWarehouseRegistrationConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void parsesWarehouseRegistrationMetricsAndMasksServiceKey() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AtomicReference<URI> calledUri = new AtomicReference<>();
        when(restTemplate.getForEntity(any(URI.class), eq(String.class))).thenAnswer(invocation -> {
            calledUri.set(invocation.getArgument(0));
            return ResponseEntity.ok("""
                    {"response":{"header":{"resultMsg":"정상","ResultCode":"0","TotalCount":2},
                    "body":{"items":{"item":[
                      {"COMPANY_ADDRESS":"인천광역시 중구 공항동로296번길 97-49","COMPANY_NAME":"(주)쉥커코리아",
                       "COMPANY_TEL":"0032-0744-0300","FROZEN_AREA":250,"FROZEN_WING_COUNT":1,
                       "GENERAL_AREA":6027.39,"GENERAL_WING_COUNT":1,"STORAGE_AREA":0,
                       "STORAGE_ITEM":"일반 및 보세화물","WARE_NO":"2012S34901140001"},
                      {"COMPANY_ADDRESS":"인천광역시 중구 공항동로296번길 98-114","COMPANY_NAME":"삼성전자로지텍(주)",
                       "FROZEN_AREA":0,"FROZEN_WING_COUNT":0,"GENERAL_AREA":4997.13,
                       "GENERAL_WING_COUNT":1,"STORAGE_AREA":0,"STORAGE_ITEM":"삼성전자 및 전자 관계사 제품취급",
                       "WARE_NO":"2012S34901140002"}
                    ]}}}}""");
        });
        MolitWarehouseRegistrationConnector connector = new MolitWarehouseRegistrationConnector(restTemplate, objectMapper);
        configure(connector, "https://apis.data.go.kr/1611000/whsinfoview2", "test-service-key");
        ReflectionTestUtils.setField(connector, "region", "인천");
        ReflectionTestUtils.setField(connector, "configuredItem", "보세화물");
        ReflectionTestUtils.setField(connector, "startDate", "2026-01-01");
        ReflectionTestUtils.setField(connector, "endDate", "20260705");

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "KR", "Battery pack"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        Map<String, String> requestParams = (Map<String, String>) payload.get("requestParamsMasked");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOLIT_WAREHOUSE_REGISTRATION);
        assertThat(result.getUsedFor()).isEqualTo("support");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getEndpoint()).endsWith("/WhsInfoDetail");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("totalCount")).isEqualTo(2);
        assertThat(metrics.get("itemKeywordMentions")).isEqualTo(1);
        assertThat(metrics.get("generalAreaSquareMeters")).isEqualTo(11024.5);
        assertThat(metrics.get("frozenAreaSquareMeters")).isEqualTo(250.0);
        assertThat(metrics.get("generalWingCount")).isEqualTo(2);
        assertThat(metrics.get("sampleWarehouseNumbers").toString()).contains("2012S34901140001");
        assertThat(evidence.get("technicalDocumentName")).isEqualTo("국토교통부_물류창고업등록정보기술문서");
        assertThat(evidence.get("apiKeyStored")).isEqualTo(false);
        assertThat(requestParams.get("serviceKey")).isEqualTo("[REDACTED]");
        assertThat(requestParams).containsEntry("type", "json")
                .containsEntry("Region", "인천")
                .containsEntry("item", "보세화물")
                .containsEntry("sday", "20260101")
                .containsEntry("eday", "20260705");
        assertThat(calledUri.get().toString()).contains("/WhsInfoDetail");
        assertThat(result.getEndpoint()).doesNotContain("test-service-key");
        assertThat(result.getRawPayload()).doesNotContain("test-service-key");
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingApiKeyReturnsEmptySourceWithoutCallingApi() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MolitWarehouseRegistrationConnector connector = new MolitWarehouseRegistrationConnector(restTemplate, objectMapper);
        configure(connector, "https://apis.data.go.kr/1611000/whsinfoview2", "");

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "KR", "Battery pack"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> diagnostics = (Map<String, Object>) payload.get("zeroResultDiagnostics");
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(payload.get("zeroReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(diagnostics.get("sourceType")).isEqualTo("empty");
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    private void configure(MolitWarehouseRegistrationConnector connector, String endpoint, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint", endpoint);
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "operation", "WhsInfoDetail");
        ReflectionTestUtils.setField(connector, "numOfRows", 50);
        ReflectionTestUtils.setField(connector, "pageNo", 1);
        ReflectionTestUtils.setField(connector, "region", "");
        ReflectionTestUtils.setField(connector, "configuredItem", "");
        ReflectionTestUtils.setField(connector, "startDate", "");
        ReflectionTestUtils.setField(connector, "endDate", "");
    }
}
