package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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

class MotieTradeRemedyOccurrenceConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieTradeRemedyOccurrenceConnector connector = new MotieTradeRemedyOccurrenceConnector(restTemplate, objectMapper);
        Path csv = writeCsv("연도,반덤핑_국내,반덤핑_전세계,상계관세_국내,상계관세_전세계,세이프가드_국내,세이프가드_전세계\n"
                + "2024,9,303,0,52,0,14\n"
                + "2025,13,323,0,54,0,14\n");
        configure(connector, csv, "", "2026-07-05", "2026-12-31", 2);

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_TRADE_REMEDY_OCCURRENCE);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-1);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("latestYear")).isEqualTo("2025");
        assertThat(metrics.get("latestDomesticRemedyCount")).isEqualTo(13);
        assertThat(metrics.get("latestWorldRemedyCount")).isEqualTo(391);
        assertThat(metrics.get("latestAntiDumpingDomestic")).isEqualTo(13);
        assertThat(metrics.get("latestCountervailingDomestic")).isEqualTo(0);
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부_무역구제제도 발생현황_20251231");
        assertThat(evidence.get("updateDue")).isEqualTo(false);
        assertThat(evidence.get("sameAsDataGoKrMetadata")).isEqualTo(true);
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesApiWhenScheduledUpdateIsDueAndKeyIsConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":100,"totalCount":1,"currentCount":1,
                        "data":[{"연도":"2026","반덤핑_국내":"15","반덤핑_전세계":"330","상계관세_국내":"1","상계관세_전세계":"55","세이프가드_국내":"0","세이프가드_전세계":"14"}]}"""));
        MotieTradeRemedyOccurrenceConnector connector = new MotieTradeRemedyOccurrenceConnector(restTemplate, objectMapper);
        configure(connector, null, "test-key", "2026-12-31", "2026-12-31", 22);

        PublicDataConnectorResult result = connector.collect(request());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(uriCaptor.getValue().toString()).contains("https://api.odcloud.kr/api/3039967/v1/");
        assertThat(uriCaptor.getValue().toString()).contains("serviceKey=test-key");
        assertThat(metrics.get("recordCount")).isEqualTo(1);
        assertThat(metrics.get("latestYear")).isEqualTo("2026");
        assertThat(metrics.get("latestDomesticRemedyCount")).isEqualTo(16);
        assertThat(result.getScoreContribution()).isEqualTo(-1);
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("motie-trade-remedy-occurrences.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(MotieTradeRemedyOccurrenceConnector connector, Path csv, String apiKey,
                           String todayOverride, String nextUpdateDate, int portalTotalRows) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/3039967/v1/uddi:aaf4da5b-3469-4e43-9e45-ef834a9a2de0_202002211020");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-12-31");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "swaggerLatestDatasetDate", "2025-12-31");
        ReflectionTestUtils.setField(connector, "portalTotalRows", portalTotalRows);
        ReflectionTestUtils.setField(connector, "apiPerPage", 100);
        ReflectionTestUtils.setField(connector, "todayOverride", todayOverride);
    }

    private PublicDataRequest request() {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "US", "배터리팩");
    }
}
