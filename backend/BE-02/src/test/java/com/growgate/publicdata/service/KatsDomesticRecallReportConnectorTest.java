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

class KatsDomesticRecallReportConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvWhenSnapshotIsDueButApiKeyIsMissing() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KatsDomesticRecallReportConnector connector = new KatsDomesticRecallReportConnector(restTemplate, objectMapper);
        Path csv = writeCsv("안전기준위반내용,표시사항부적합내용,사고사례내용\n"
                + "전기요 전기용품안전기준 표면온도 50도 초과,표시사항미비-제조연월누락,화상 사고 유발 가능\n"
                + "프탈레이트계 가소제 기준치 초과,,프탈레이트계 가소제에 노출될 경우 간 손상 유발 가능\n"
                + "완구 작은 부품 기준 부적합,표시사항미비-사용연령누락,어린이 삼킴 사고 유발 가능\n");
        configure(connector, csv, "2026-07-05", "", "2022-08-02");

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KATS_DOMESTIC_RECALL_REPORT);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-2);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(metrics.get("safetyViolationRows")).isEqualTo(3L);
        assertThat(metrics.get("labelNonComplianceRows")).isEqualTo(2L);
        assertThat(metrics.get("accidentCaseRows")).isEqualTo(3L);
        assertThat(metrics.get("chemicalRiskRows")).isEqualTo(1L);
        assertThat(metrics.get("electricalRiskRows")).isEqualTo(1L);
        assertThat(metrics.get("childrenRiskRows")).isEqualTo(1L);
        assertThat(evidence.get("fileDataName")).isEqualTo("산업통상부_국가기술표준원_제품안전_국내리콜 보고서_20220801");
        assertThat(evidence.get("updateDue")).isEqualTo(true);
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
                        {"page":1,"perPage":1000,"totalCount":2,"currentCount":2,
                        "data":[
                          {"안전기준위반내용":"전기요 전기용품안전기준 표면온도 50도 초과","표시사항부적합내용":"표시사항미비","사고사례내용":"화상 사고 유발 가능"},
                          {"안전기준위반내용":"프탈레이트계 가소제 기준치 초과","표시사항부적합내용":"","사고사례내용":"간 손상 유발 가능"}
                        ]}"""));
        KatsDomesticRecallReportConnector connector = new KatsDomesticRecallReportConnector(restTemplate, objectMapper);
        configure(connector, null, "2026-07-05", "test-key", "2022-08-02");

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-2);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(metrics.get("chemicalRiskRows")).isEqualTo(1L);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15040695");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("kats-domestic-recall-report.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KatsDomesticRecallReportConnector connector, Path csv, String today, String apiKey,
                           String nextUpdateDate) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15040695/v1/uddi:d4469be3-c128-4fbc-8ef2-b6e15689625f");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2022-08-01");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "630110", "KR", product);
    }
}
