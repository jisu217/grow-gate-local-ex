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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KsureOverseasBranchesConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvWhenNoScheduledUpdateIsDue() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KsureOverseasBranchesConnector connector = new KsureOverseasBranchesConnector(restTemplate, objectMapper);
        Path csv = writeCsv("지사명,영문 지사명,주소\n"
                + "L.A지사,Los Angeles Representative Office,\"915 Wilshire Blvd. Suite 1640 Los Angeles, CA 90017 USA\"\n"
                + "북경지사,Beijing Representative Office,\"Beijing, China\"\n"
                + "파리지사,Paris Representative Office,\"Paris, France\"\n");
        configure(connector, csv, "2026-07-05", "", "2099-12-31");

        PublicDataConnectorResult result = connector.collect(request("US"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KSURE_OVERSEAS_BRANCHES);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("targetCountryBranchMatch")).isEqualTo(true);
        assertThat(metrics.get("targetCountryBranchCount")).isEqualTo(1);
        assertThat((List<String>) metrics.get("targetCountryBranchNames")).containsExactly("L.A지사");
        assertThat(metrics.get("englishBranchNameCount")).isEqualTo(3L);
        assertThat(metrics.get("addressCount")).isEqualTo(3L);
        assertThat(evidence.get("fileDataName")).isEqualTo("한국무역보험공사_해외지사 현황_20260625");
        assertThat(evidence.get("updateDue")).isEqualTo(false);
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
                        {"page":1,"perPage":100,"totalCount":2,"currentCount":2,
                        "data":[
                          {"지사명":"L.A지사","영문 지사명":"Los Angeles Representative Office","주소":"915 Wilshire Blvd. Suite 1640 Los Angeles, CA 90017 USA"},
                          {"지사명":"북경지사","영문 지사명":"Beijing Representative Office","주소":"Beijing, China"}
                        ]}"""));
        KsureOverseasBranchesConnector connector = new KsureOverseasBranchesConnector(restTemplate, objectMapper);
        configure(connector, null, "2026-07-05", "test-key", "2026-07-05");

        PublicDataConnectorResult result = connector.collect(request("CN"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("targetCountryBranchMatch")).isEqualTo(true);
        assertThat(metrics.get("targetCountryBranchCount")).isEqualTo(1);
        assertThat((List<String>) metrics.get("targetCountryBranchNames")).containsExactly("북경지사");
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15044855");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("ksure-overseas-branches.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KsureOverseasBranchesConnector connector, Path csv, String today, String apiKey,
                           String nextUpdateDate) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15044855/v1/uddi:99a3f676-337f-439c-8ccd-c70ac08efcd9");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-06-25");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String country) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", country, "리튬이온 배터리");
    }
}
