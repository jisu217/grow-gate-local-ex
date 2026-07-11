package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderSchemaNormalizationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void kotraMarketMapsProviderFieldsIntoMetricsAndPeriod() {
        RestTemplate restTemplate = restTemplateReturning("publicdata/kotra-market-sample-live-response.json");
        KotraMarketConnector connector = new KotraMarketConnector(restTemplate, objectMapper);
        configure(connector, "endpoint", "https://api.example.test/kotra");
        configure(connector, "apiKey", "test-key");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> fieldMapping = (Map<String, Object>) payload.get("fieldMapping");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getUsedFor()).isEqualTo("market");
        assertThat(metrics.get("marketArticleCount")).isEqualTo(2);
        assertThat(((Number) metrics.get("countryMentions")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) metrics.get("productMentions")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) metrics.get("yoyGrowthPct")).doubleValue()).isEqualTo(12.4);
        assertThat(fieldMapping).containsKey("marketArticleCount");
        assertThat(payload.get("sourcePeriodStart")).isEqualTo("2026-06-15");
        assertThat(payload.get("sourcePeriodEnd")).isEqualTo("2026-06-30");
        assertThat(result.getScoreContribution()).isGreaterThan(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void safetyRecallMapsRecallAndHighRiskFieldsIntoNegativeContribution() {
        RestTemplate restTemplate = restTemplateReturning("publicdata/safety-recall-sample-live-response.json");
        SafetyRecallConnector connector = new SafetyRecallConnector(restTemplate, objectMapper);
        configure(connector, "endpoint", "https://api.example.test/safety");
        configure(connector, "apiKey", "test-key");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(metrics.get("recallCount")).isEqualTo(2);
        assertThat(((Number) metrics.get("highRiskCount")).intValue()).isGreaterThanOrEqualTo(1);
        assertThat(result.getScoreContribution()).isLessThan(0);
        assertThat(result.getConfidence()).isGreaterThan(0.35);
    }

    @Test
    @SuppressWarnings("unchecked")
    void kiprisFtoMapsPatentAndClaimCountsIntoFtoRisk() {
        RestTemplate restTemplate = restTemplateReturning("publicdata/kipris-fto-sample-live-response.json");
        KiprisFtoConnector connector = new KiprisFtoConnector(restTemplate, objectMapper);
        configure(connector, "endpoint", "https://api.example.test/kipris");
        configure(connector, "apiKey", "test-key");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(metrics.get("patentCount")).isEqualTo(2);
        assertThat(metrics.get("claimsCount")).isEqualTo(20);
        assertThat(((Number) metrics.get("productMentions")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(result.getUsedFor()).isEqualTo("fto");
        assertThat(result.getScoreContribution()).isLessThan(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void bizinfoSupportMapsOpenExportProgramsIntoSupportContribution() {
        RestTemplate restTemplate = restTemplateReturning("publicdata/bizinfo-support-sample-live-response.json");
        BizinfoSupportConnector connector = new BizinfoSupportConnector(restTemplate, objectMapper);
        configure(connector, "endpoint", "https://api.example.test/bizinfo");
        configure(connector, "apiKey", "test-key");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(metrics.get("supportProgramCount")).isEqualTo(2);
        assertThat(((Number) metrics.get("openProgramSignalCount")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) metrics.get("exportKeywordCount")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(result.getUsedFor()).isEqualTo("support");
        assertThat(result.getScoreContribution()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportEvidenceFeedCollectsAllowlistedRssIntoExternalEvidenceRecord() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(resource("publicdata/export-evidence-feed-sample.xml")));
        ExportEvidenceFeedConnector connector = new ExportEvidenceFeedConnector(restTemplate, objectMapper);
        configure(connector, "urls", "https://dream.kotra.or.kr/rss/export.xml");
        configure(connector, "allowedHosts", "dream.kotra.or.kr,www.kotra.or.kr");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("external_feed");
        assertThat(result.getUsedFor()).isEqualTo("market");
        assertThat(metrics.get("documentCount")).isEqualTo(2);
        assertThat(((Number) metrics.get("exportKeywordHits")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(result.getNormalizedPayload().get("sourcePeriodEnd")).isEqualTo("2026-06-30");
        assertThat(result.getScoreContribution()).isBetween(1, 3);
    }

    private PublicDataRequest request() {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack");
    }

    private RestTemplate restTemplateReturning(String path) {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String response = resource(path);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class))).thenReturn(ResponseEntity.ok(response));
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(response));
        return restTemplate;
    }

    private String resource(String path) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalArgumentException("missing test resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void configure(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }
}
