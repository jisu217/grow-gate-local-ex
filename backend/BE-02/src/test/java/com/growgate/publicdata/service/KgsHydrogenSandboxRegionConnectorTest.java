package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

class KgsHydrogenSandboxRegionConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KgsHydrogenSandboxRegionConnector connector = new KgsHydrogenSandboxRegionConnector(restTemplate, objectMapper);
        Path csv = writeCsv("행정구역,규제자유특구,규제샌드박스,기준일자\n"
                + "서울,0,4,2025-11-30\n"
                + "경남,2,16,2025-11-30\n"
                + "울산,2,11,2025-11-30\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("수소 저장 탱크"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KGS_HYDROGEN_SANDBOX_REGIONS);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("hydrogenKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("totalFreeZoneCount")).isEqualTo(4);
        assertThat(metrics.get("totalSandboxCount")).isEqualTo(31);
        assertThat(metrics.get("leadingRegion")).isEqualTo("경남");
        assertThat(metrics.get("leadingRegionSandboxCount")).isEqualTo(16);
        assertThat(metrics.get("latestBaseDate")).isEqualTo("2025-11-30");
        assertThat(payload.get("sourcePeriodEnd")).isEqualTo("2025-11-30");
        assertThat(evidence.get("fileDataName")).isEqualTo("한국가스안전공사_수소분야 규제샌드박스 실증지역별 통계_20251130");
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
                          {"행정구역":"경남","규제자유특구":2,"규제샌드박스":16,"기준일자":"2025-11-30"},
                          {"행정구역":"울산","규제자유특구":2,"규제샌드박스":11,"기준일자":"2025-11-30"}
                        ]}"""));
        KgsHydrogenSandboxRegionConnector connector = new KgsHydrogenSandboxRegionConnector(restTemplate, objectMapper);
        configure(connector, null, "2026-12-01", "test-key");

        PublicDataConnectorResult result = connector.collect(request("hydrogen fuel cell"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("hydrogenKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("totalSandboxCount")).isEqualTo(27);
        assertThat(metrics.get("leadingRegion")).isEqualTo("경남");
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15139606");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("kgs-hydrogen-sandbox-regions.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    private void configure(KgsHydrogenSandboxRegionConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15139606/v1/uddi:1757878e-98ac-41b6-bcc6-98fda8d01d03");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-11-30");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-12-01");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "841950", "KR", product);
    }
}
