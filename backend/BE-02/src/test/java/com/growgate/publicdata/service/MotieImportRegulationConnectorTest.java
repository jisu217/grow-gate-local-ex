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

class MotieImportRegulationConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieImportRegulationConnector connector = new MotieImportRegulationConnector(restTemplate, objectMapper);
        Path csv = writeCsv("관리번호,규제국,품목명,규제유형,조사개시일,규제상태,최근갱신일\n"
                + "2389,미국,폴리테트라메틸렌,반덤핑,2026-04-08,조사중,2026-04-23\n"
                + "7,미국,스탠다드 강관,반덤핑,1991-10-04,규제중,2025-09-29\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("US", "강관"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_IMPORT_REGULATION);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(((Map<String, Object>) payload.get("zeroResultDiagnostics")).get("sourceType")).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("countryMatchCount")).isEqualTo(2);
        assertThat(metrics.get("productMentionCount")).isEqualTo(1);
        assertThat(metrics.get("activeRegulationCount")).isEqualTo(2);
        assertThat(payload.get("sourcePeriodEnd")).isEqualTo("2026-04-23");
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부_수입규제현황_20260508");
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
                        {"page":1,"perPage":1000,"totalCount":2,"currentCount":2,
                        "data":[
                          {"관리번호":2389,"규제국":"미국","품목명":"폴리테트라메틸렌","규제유형":"반덤핑","조사개시일":"2026-04-08","규제상태":"조사중","최근갱신일":"2026-04-23"},
                          {"관리번호":7,"규제국":"미국","품목명":"스탠다드 강관","규제유형":"반덤핑","조사개시일":"1991-10-04","규제상태":"규제중","최근갱신일":"2025-09-29"}
                        ]}"""));
        MotieImportRegulationConnector connector = new MotieImportRegulationConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-05-14", "test-key");

        PublicDataConnectorResult result = connector.collect(request("US", "강관"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("countryMatchCount")).isEqualTo(2);
        assertThat(metrics.get("productMentionCount")).isEqualTo(1);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/3039436");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("motie-import-regulations.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(MotieImportRegulationConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/3039436/v1/uddi:e460a8d2-9074-45b1-b5eb-c3cd169cbe2a_201703291030");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-05-08");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2027-05-14");
    }

    private PublicDataRequest request(String country, String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "730630", country, product);
    }
}
