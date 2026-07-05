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

class TradeSecurityHskLinkageConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        TradeSecurityHskLinkageConnector connector = new TradeSecurityHskLinkageConnector(restTemplate, objectMapper);
        Path csv = writeCsv("품목번호,품명(국문),품명(영문),통제번호\n"
                + "2504900000,기타,Other,\"0C004.,1A003.b.\"\n"
                + "8507600000,리튬이온 축전지,Lithium-ion accumulators,\"3A001.,3A201.\"\n");
        configure(connector, csv, "2026-07-05", "");

        PublicDataConnectorResult result = connector.collect(request("850760", "리튬이온 배터리"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.TRADE_SECURITY_HSK_LINKAGE);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(((Map<String, Object>) payload.get("zeroResultDiagnostics")).get("sourceType")).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("hsCodeMatchCount")).isEqualTo(1);
        assertThat(metrics.get("productMentionCount")).isEqualTo(1);
        assertThat(metrics.get("matchedControlReferenceCount")).isEqualTo(2);
        assertThat(evidence.get("fileDataName")).isEqualTo("무역안보관리원_HSK 연계표 정보_20260522");
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
                          {"품목번호":2504900000,"품명(국문)":"기타","품명(영문)":"Other","통제번호":"0C004.,1A003.b."},
                          {"품목번호":8507600000,"품명(국문)":"리튬이온 축전지","품명(영문)":"Lithium-ion accumulators","통제번호":"3A001.,3A201."}
                        ]}"""));
        TradeSecurityHskLinkageConnector connector = new TradeSecurityHskLinkageConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-05-22", "test-key");

        PublicDataConnectorResult result = connector.collect(request("850760", "리튬이온 배터리"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("hsCodeMatchCount")).isEqualTo(1);
        assertThat(metrics.get("matchedControlReferenceCount")).isEqualTo(2);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15034135");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("hsk-linkage.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }

    private void configure(TradeSecurityHskLinkageConnector connector, Path csv, String today, String apiKey) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15034135/v1/uddi:80b756b5-9d5f-4369-8a38-ad5ac43534e5");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-05-22");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2027-05-22");
    }

    private PublicDataRequest request(String hsCode, String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", hsCode, "US", product);
    }
}
