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

class TradeSecurityItemKeywordConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TradeSecurityItemKeywordConnector connector = new TradeSecurityItemKeywordConnector(restTemplate, objectMapper);
        Path csv = writeCsv("통제번호,키워드1,키워드2,키워드3\n"
                + "1A001.,\"실; 씰; sealant\",\"불소화; elastomer\",\"우주; 항공\"\n"
                + "3A001.,\"리튬이온 배터리; battery pack\",\"전지; accumulator\",\"전기차\"\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 배터리팩"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.TRADE_SECURITY_ITEM_KEYWORD);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(((Map<String, Object>) payload.get("zeroResultDiagnostics")).get("sourceType")).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("keywordMatchRowCount")).isEqualTo(1);
        assertThat(metrics.get("matchedControlNumberCount")).isEqualTo(1);
        assertThat(metrics.get("matchedKeywordTokenCount")).isEqualTo(2);
        assertThat(evidence.get("fileDataName")).isEqualTo("무역안보관리원_전략물자 품목키워드 및 개정정보_20260522");
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
                          {"통제번호":"1A001.","키워드1":"실; 씰; sealant","키워드2":"불소화; elastomer","키워드3":"우주; 항공"},
                          {"통제번호":"3A001.","키워드1":"리튬이온 배터리; battery pack","키워드2":"전지; accumulator","키워드3":"전기차"}
                        ]}"""));
        TradeSecurityItemKeywordConnector connector = new TradeSecurityItemKeywordConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-05-22", "test-key");

        PublicDataConnectorResult result = connector.collect(request("리튬이온 배터리팩"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("keywordMatchRowCount")).isEqualTo(1);
        assertThat(metrics.get("matchedControlNumberCount")).isEqualTo(1);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15042113");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("item-keywords.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    private void configure(TradeSecurityItemKeywordConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15042113/v1/uddi:40a03829-dfb8-4e40-b6c5-e10345ee462d");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-05-22");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2027-05-22");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 2);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "US", product);
    }
}
