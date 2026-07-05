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

class MotieMpeTradeStatisticsConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieMpeTradeStatisticsConnector connector = new MotieMpeTradeStatisticsConnector(restTemplate, objectMapper);
        Path csv = writeCsv("지역,소재부품장비품목코드,소재부품장비품목명,2024수출(달러),2024수입(달러),2025수출(달러),2025수입(달러)\n"
                + "전체,000,전산업,1000,900,1200,1000\n"
                + "독일,21053,축전지,100,80,150,120\n"
                + "미국,21053,축전지,200,300,250,200\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 축전지", "DE"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_MPE_TRADE_STATISTICS);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(((Map<String, Object>) payload.get("zeroResultDiagnostics")).get("sourceType")).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("regionMatchRowCount")).isEqualTo(1);
        assertThat(metrics.get("productCategoryMatchRowCount")).isEqualTo(2);
        assertThat(metrics.get("matchedRowCount")).isEqualTo(1);
        assertThat(metrics.get("latestYear")).isEqualTo(2025);
        assertThat(metrics.get("latestExportUsd")).isEqualTo(150L);
        assertThat(metrics.get("latestImportUsd")).isEqualTo(120L);
        assertThat(metrics.get("latestTradeBalanceUsd")).isEqualTo(30L);
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부_소재부품장비 무역통계 수출입 총괄_20251231");
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
                          {"지역":"독일","소재부품장비품목코드":21053,"소재부품장비품목명":"축전지","2025수출(달러)":150,"2025수입(달러)":120},
                          {"지역":"미국","소재부품장비품목코드":21053,"소재부품장비품목명":"축전지","2025수출(달러)":250,"2025수입(달러)":200}
                        ]}"""));
        MotieMpeTradeStatisticsConnector connector = new MotieMpeTradeStatisticsConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-06-17", "test-key");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 축전지", "DE"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("matchedRowCount")).isEqualTo(1);
        assertThat(metrics.get("latestExportUsd")).isEqualTo(150L);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15099627");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("mpe-trade.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    private void configure(MotieMpeTradeStatisticsConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15099627/v1/uddi:64f277d4-70bb-49a7-bb50-a4d56466ea3e");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-12-31");
        ReflectionTestUtils.setField(connector, "registeredDate", "2026-06-17");
        ReflectionTestUtils.setField(connector, "modifiedDate", "2026-06-18");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2027-06-17");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product, String country) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", country, product);
    }
}
