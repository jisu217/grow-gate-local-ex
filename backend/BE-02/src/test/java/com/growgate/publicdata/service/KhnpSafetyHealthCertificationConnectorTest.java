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

class KhnpSafetyHealthCertificationConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KhnpSafetyHealthCertificationConnector connector = new KhnpSafetyHealthCertificationConnector(restTemplate, objectMapper);
        Path csv = writeCsv("사업소,인증규격,인증기준,유효기간 시작일,유효기간 종료일,비  고\n"
                + "전사통합,ISO,KS Q ISO 45001:2018,2025-12-19,2028-12-18,\n"
                + "고리본부,KOSHA,KOSHA-MS,2024-01-01,2027-12-31,\n"
                + "예천양수,KOSHA,KOSHA-MS,2023-11-16,2026-11-15,인증 반납('25.5)\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("원자력 발전 안전 설비"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KHNP_SAFETY_HEALTH_CERTIFICATION);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("supportKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("isoCertificationCount")).isEqualTo(1);
        assertThat(metrics.get("koshaCertificationCount")).isEqualTo(2);
        assertThat(metrics.get("activeCertificationCount")).isEqualTo(2);
        assertThat(metrics.get("surrenderedCertificationCount")).isEqualTo(1);
        assertThat(metrics.get("earliestExpiryDate")).isEqualTo("2026-11-15");
        assertThat(metrics.get("latestExpiryDate")).isEqualTo("2028-12-18");
        assertThat(evidence.get("fileDataName")).isEqualTo("한국수력원자력(주)_안전보건 인증현황 정보_20260623");
        assertThat(evidence.get("updateDue")).isEqualTo(false);
        assertThat(evidence.get("operationEndpoint")).isEqualTo(false);
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesApiWhenScheduledUpdateIsDueAndKeyIsConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":100,"totalCount":2,"currentCount":2,
                        "data":[
                          {"사업소":"전사통합","인증규격":"ISO","인증기준":"KS Q ISO 45001:2018","유효기간 시작일":"2025-12-19","유효기간 종료일":"2028-12-18","비  고":""},
                          {"사업소":"예천양수","인증규격":"KOSHA","인증기준":"KOSHA-MS","유효기간 시작일":"2023-11-16","유효기간 종료일":"2026-11-15","비  고":"인증 반납('25.5)"}
                        ]}"""));
        KhnpSafetyHealthCertificationConnector connector = new KhnpSafetyHealthCertificationConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-06-30", "test-key");

        PublicDataConnectorResult result = connector.collect(request("nuclear safety equipment"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("supportKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("surrenderedCertificationCount")).isEqualTo(1);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15101214");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("khnp-safety-health-certifications.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KhnpSafetyHealthCertificationConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15101214/v1/uddi:81fdbdb3-dea1-46e7-8286-c64e83ae0743");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-06-23");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2027-06-30");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "841950", "KR", product);
    }
}
