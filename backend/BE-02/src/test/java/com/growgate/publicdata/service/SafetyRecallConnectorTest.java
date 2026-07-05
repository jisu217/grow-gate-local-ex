package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
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

class SafetyRecallConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void keyMissingWithCsvCacheReturnsCacheNotFallback() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SafetyRecallConnector connector = connector(restTemplate, "");
        Path csv = writeCsv();
        configure(connector, csv, tempDir.resolve("cache"));

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.SAFETY_RECALL);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(evidence).containsEntry("cacheLineage", "file-snapshot");
        assertThat(evidence).containsEntry("csvCacheAvailable", true);
        assertThat(String.valueOf(evidence.get("reason"))).contains("API_KEY_NOT_CONFIGURED");
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void keyAndOperationEndpointConfiguredReturnsLiveAndWritesApiDerivedCache() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"data":[
                          {"인증번호":"R-1","제품명":"전기요","법정제품분류":"전기용품","모델명":"E-100",
                           "제조사명":"테스트제조","리콜종류":"명령에따른리콜","위해유형":"과열","등록일":"2026-07-01"},
                          {"인증번호":"R-2","제품명":"완구","법정제품분류":"어린이>완구","모델명":"T-1",
                           "제조사명":"테스트제조","리콜종류":"명령에따른리콜","위해유형":"유해물질","등록일":"2026-07-02"}
                        ]}"""));
        SafetyRecallConnector connector = connector(restTemplate, "test-key");
        configure(connector, null, tempDir.resolve("cache"));

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) result.getNormalizedPayload().get("collectionEvidence");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isLessThan(0);
        assertThat(result.getConfidence()).isGreaterThan(0.35);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(evidence).containsEntry("operationEndpoint", true);
        assertThat(evidence).containsEntry("apiKeyConfigured", true);
        assertThat(Files.exists(tempDir.resolve("cache").resolve(java.time.LocalDate.now().toString()).resolve("metadata.json"))).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void apiFailureWithCsvCacheReturnsCacheWithFailureReason() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("timed out"));
        SafetyRecallConnector connector = connector(restTemplate, "test-key");
        Path csv = writeCsv();
        configure(connector, csv, tempDir.resolve("cache"));

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        Map<String, Object> evidence = (Map<String, Object>) result.getNormalizedPayload().get("collectionEvidence");
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(String.valueOf(evidence.get("reason"))).contains("TIMEOUT");
        assertThat(String.valueOf(evidence.get("reason"))).contains("CSV cache available");
    }

    private SafetyRecallConnector connector(RestTemplate restTemplate, String apiKey) {
        SafetyRecallConnector connector = new SafetyRecallConnector(restTemplate, objectMapper);
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "datasetDate", "2023-08-09");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2025-09-05");
        ReflectionTestUtils.setField(connector, "portalTotalRows", 2);
        ReflectionTestUtils.setField(connector, "apiPerPage", 1000);
        return connector;
    }

    private void configure(SafetyRecallConnector connector, Path csv, Path cacheRoot) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "cacheRoot", cacheRoot.toString());
    }

    private Path writeCsv() throws Exception {
        Path csv = tempDir.resolve("kats-domestic-recall.csv");
        Files.writeString(csv, """
                인증번호,모델명,제품명,브랜드명,사업자구분,법정제품분류,리콜종류,리콜사업자명,제조국가명,제조사명,제조사국가명,리콜방법,위해유형,안전성조사명,등록일
                R-1,E-100,전기요,,수입,전기용품,명령에따른리콜,테스트사업자,중국,테스트제조,중국,교환수거,과열,4,2026-07-01
                R-2,T-1,완구,,수입,어린이>완구,명령에따른리콜,테스트사업자,중국,테스트제조,중국,교환수거,유해물질,4,2026-07-02
                """, Charset.forName("MS949"));
        return csv;
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "630110", "KR", product);
    }
}
