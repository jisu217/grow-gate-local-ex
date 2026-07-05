package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KatsProductSafetyHazardInfoConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KatsProductSafetyHazardInfoConnector connector = new KatsProductSafetyHazardInfoConnector(restTemplate, objectMapper);
        Path csv = writeCsv("품명,제품분류,제조국가명,위해부위,치료비용,사고발생일,날짜\n"
                + "직류전원장치,정보통신 사무기기>직류전원장치>직류전원장치,중국,손,12000,2025-07-08,2025-07-16\n"
                + "전기요,전기기기>전기담요 및 매트 전기침대>전기요,대한민국,다리,0,2024-01-03,2024-01-04\n"
                + "완구,어린이제품>완구,대한민국,기타,5000,2023-05-01,2023-05-02\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("직류전원장치"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KATS_PRODUCT_SAFETY_HAZARD_INFO);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-2);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("productKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("nonZeroTreatmentCostCount")).isEqualTo(2L);
        assertThat(metrics.get("treatmentCostTotal")).isEqualTo(17000L);
        assertThat(metrics.get("domesticManufactureCount")).isEqualTo(2L);
        assertThat(metrics.get("foreignManufactureCount")).isEqualTo(1L);
        assertThat(metrics.get("topProductName")).isEqualTo("직류전원장치");
        assertThat(metrics.get("topHazardBodyPart")).isEqualTo("손");
        assertThat(metrics.get("latestAccidentDate")).isEqualTo("2025-07-08");
        assertThat(metrics.get("latestReportDate")).isEqualTo("2025-07-16");
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부 국가기술표준원_제품안전_위해정보_20250716");
        assertThat(evidence.get("updateDue")).isEqualTo(false);
        assertThat(evidence.get("operationEndpoint")).isEqualTo(false);
        assertThat(evidence.get("apiKeyStored")).isEqualTo(false);
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesApiWhenScheduledUpdateIsDueAndKeyIsConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":500,"totalCount":2,"currentCount":2,
                        "data":[
                          {"품명":"직류전원장치","제품분류":"정보통신 사무기기>직류전원장치>직류전원장치","제조국가명":"중국","위해부위":"손","치료비용":"12000","사고발생일":"2025-07-08","날짜":"2025-07-16"},
                          {"품명":"전기요","제품분류":"전기기기>전기담요 및 매트 전기침대>전기요","제조국가명":"대한민국","위해부위":"다리","치료비용":"0","사고발생일":"2024-01-03","날짜":"2024-01-04"}
                        ]}"""));
        KatsProductSafetyHazardInfoConnector connector = new KatsProductSafetyHazardInfoConnector(restTemplate, objectMapper);
        configure(connector, null, "2026-08-04", "test-key");

        PublicDataConnectorResult result = connector.collect(request("직류전원장치"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-2);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("foreignManufactureCount")).isEqualTo(1L);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15064255");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("kats-product-safety-hazard-info.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KatsProductSafetyHazardInfoConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15064255/v1/uddi:b9de61e1-872c-4e55-9747-f4df5724aedc");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-07-16");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-08-04");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850440", "KR", product);
    }
}
