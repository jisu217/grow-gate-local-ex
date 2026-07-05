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

class MotieMpeInventoryIndexConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieMpeInventoryIndexConnector connector = new MotieMpeInventoryIndexConnector(restTemplate, objectMapper);
        Path csv = writeCsv("품목코드,품목명,재고지수2501,재고지수2502,재고지수2512\n"
                + "0000,소재부품장비,118.5,117.8,121.2\n"
                + "21053,축전지,100.0,105.0,130.0\n"
                + "36010,기타무선통신장비및방송장비,105.9,113.8,143.2\n");
        configure(connector, csv, "2026-06-26", "");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 축전지"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_MPE_INVENTORY_INDEX);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("productCategoryMatchRowCount")).isEqualTo(1);
        assertThat(metrics.get("latestMonth")).isEqualTo("2025-12");
        assertThat(metrics.get("latestInventoryIndex")).isEqualTo(130.0);
        assertThat(metrics.get("firstMonth")).isEqualTo("2025-01");
        assertThat(metrics.get("firstInventoryIndex")).isEqualTo(100.0);
        assertThat(metrics.get("periodChangePct")).isEqualTo(30.0);
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부_소재부품장비 생산지수통계(재고)_20251231");
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
                          {"품목코드":21053,"품목명":"축전지","재고지수2501":"100.0","재고지수2512":"130.0"},
                          {"품목코드":36010,"품목명":"기타무선통신장비및방송장비","재고지수2501":"105.9","재고지수2512":"143.2"}
                        ]}"""));
        MotieMpeInventoryIndexConnector connector = new MotieMpeInventoryIndexConnector(restTemplate, objectMapper);
        configure(connector, null, "2026-06-27", "test-key");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 축전지"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productCategoryMatchRowCount")).isEqualTo(1);
        assertThat(metrics.get("latestInventoryIndex")).isEqualTo(130.0);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15099629");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("mpe-inventory-index.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(MotieMpeInventoryIndexConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15099629/v1/uddi:f48b90d3-4b80-4492-a9ab-de45471d408e");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-12-31");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-06-27");
        ReflectionTestUtils.setField(connector, "portalDetailDatasetDate", "2025-12-31");
        ReflectionTestUtils.setField(connector, "portalDetailTotalRows", 128);
        ReflectionTestUtils.setField(connector, "localTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", product);
    }
}
