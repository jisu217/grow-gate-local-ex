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

class KicoxFactoryRegistrationConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReadsCsvFirstWhenApiKeyIsMissingEvenAfterUpdateDueDate() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KicoxFactoryRegistrationConnector connector = new KicoxFactoryRegistrationConnector(restTemplate, objectMapper);
        configure(connector, "", "2026-07-05");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(26);
        assertThat(metrics.get("regionCount")).isEqualTo(17);
        assertThat(metrics.get("industryCount")).isEqualTo(26);
        assertThat(metrics.get("totalRegisteredFactories")).isEqualTo(219242L);
        assertThat(metrics.get("topIndustry")).isEqualTo("25 금속가공제품 제조업; 기계 및 가구 제외");
        assertThat(metrics.get("topIndustryFactoryCount")).isEqualTo(35228L);
        assertThat(metrics.get("topRegion")).isEqualTo("경기");
        assertThat(metrics.get("topRegionFactoryCount")).isEqualTo(78449L);
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
                          {"년월":"2026-03-31","등록현황":"10 식료품 제조업","서울":"229","부산":"625","대구":"342","인천":"691","광주":"278","대전":"205","울산":"101","세종":"111","경기":"4929","강원":"1043","충북":"1557","충남":"1928","전북":"1589","전남":"2270","경북":"1535","경남":"1608","제주":"224"},
                          {"년월":"2026-03-31","등록현황":"25 금속가공제품 제조업; 기계 및 가구 제외","서울":"700","부산":"2500","대구":"1600","인천":"3100","광주":"700","대전":"520","울산":"400","세종":"300","경기":"15000","강원":"1200","충북":"1300","충남":"1700","전북":"1200","전남":"1300","경북":"2500","경남":"1600","제주":"108"}
                        ]}"""));
        KicoxFactoryRegistrationConnector connector = new KicoxFactoryRegistrationConnector(restTemplate, objectMapper);
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
        assertThat(uri).contains("https://api.odcloud.kr/api/15085508/v1/uddi:b0d2581a-2a88-47a6-af2d-87b575db32d2");
        assertThat(uri).contains("page=1");
        assertThat(uri).contains("perPage=100");
        assertThat(uri).contains("returnType=JSON");
        assertThat(uri).contains("serviceKey=test-key");
        assertThat(maskedParams.get("serviceKey")).isEqualTo("[REDACTED]");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("regionCount")).isEqualTo(17);
        assertThat(metrics.get("industryCount")).isEqualTo(2);
        assertThat(metrics.get("topIndustry")).isEqualTo("25 금속가공제품 제조업; 기계 및 가구 제외");
        assertThat(metrics.get("topRegion")).isEqualTo("경기");
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(evidence.get("collectionMode")).isEqualTo("api_after_scheduled_update");
        assertThat(evidence.get("updateDue")).isEqualTo(true);
    }

    private void configure(KicoxFactoryRegistrationConnector connector, String apiKey, String todayOverride) {
        ReflectionTestUtils.setField(connector, "endpoint", "https://api.odcloud.kr/api/15085508/v1/uddi:b0d2581a-2a88-47a6-af2d-87b575db32d2");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "csvResource", "classpath:publicdata/kicox-factory-registration-by-region-industry-20260331.csv");
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-03-31");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-04-30");
        ReflectionTestUtils.setField(connector, "swaggerLatestDatasetDate", "2026-03-31");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 26);
        ReflectionTestUtils.setField(connector, "apiPerPage", 100);
        ReflectionTestUtils.setField(connector, "todayOverride", todayOverride);
    }

    private PublicDataRequest request() {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "US", "배터리팩");
    }
}
