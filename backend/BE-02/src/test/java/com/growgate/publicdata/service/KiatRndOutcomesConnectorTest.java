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

class KiatRndOutcomesConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeNextScheduledUpdate() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KiatRndOutcomesConnector connector = new KiatRndOutcomesConnector(restTemplate, objectMapper);
        Path csv = writeCsv("성과발생년도,접수번호,시점구분,발생여부_논문,발생여부_특허,등록기탁번호_특허,국가과학기술지식정보서비스 순번,등록일자 및 수정일자,데이터 기준일자\n"
                + "2018,N030600016,성과입력,미발생,발생,20170113075,503,2018-12-13,2026-04-10\n"
                + "2019,P033700195,성과입력,발생,미발생,,504,2019-01-09,2026-04-10\n"
                + ",,,,,,,,\n");
        configure(connector, csv, "2026-07-05", "", "2027-05-14");

        PublicDataConnectorResult result = connector.collect(request("특허 기술 개발"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KIAT_RND_OUTCOMES);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("effectiveRecordCount")).isEqualTo(2);
        assertThat(metrics.get("supportKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("paperOutcomeCount")).isEqualTo(1);
        assertThat(metrics.get("patentOutcomeCount")).isEqualTo(1);
        assertThat(metrics.get("patentDepositNumberCount")).isEqualTo(1);
        assertThat(metrics.get("earliestOutcomeYear")).isEqualTo("2018");
        assertThat(metrics.get("latestOutcomeYear")).isEqualTo("2019");
        assertThat(metrics.get("latestRegistrationDate")).isEqualTo("2019-01-09");
        assertThat(metrics.get("latestBaseDate")).isEqualTo("2026-04-10");
        assertThat(payload.get("normalizedCount")).isEqualTo(2);
        assertThat(evidence.get("fileDataName")).isEqualTo("한국산업기술진흥원_연구개발 성과_20260410");
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
                        {"page":1,"perPage":10000,"totalCount":2,"currentCount":2,
                        "data":[
                          {"성과발생년도":2018,"접수번호":"N030600016","시점구분":"성과입력","발생여부_논문":"미발생","발생여부_특허":"발생","등록기탁번호_특허":"20170113075","국가과학기술지식정보서비스 순번":503,"등록일자 및 수정일자":"2018-12-13","데이터 기준일자":"2026-04-10"},
                          {"성과발생년도":2019,"접수번호":"P033700195","시점구분":"성과입력","발생여부_논문":"발생","발생여부_특허":"미발생","등록기탁번호_특허":"","국가과학기술지식정보서비스 순번":504,"등록일자 및 수정일자":"2019-01-09","데이터 기준일자":"2026-04-10"}
                        ]}"""));
        KiatRndOutcomesConnector connector = new KiatRndOutcomesConnector(restTemplate, objectMapper);
        configure(connector, null, "2027-05-14", "test-key", "2027-05-14");

        PublicDataConnectorResult result = connector.collect(request("R&D patent technology"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("effectiveRecordCount")).isEqualTo(2);
        assertThat(metrics.get("supportKeywordMatch")).isEqualTo(true);
        assertThat(metrics.get("paperOutcomeCount")).isEqualTo(1);
        assertThat(metrics.get("patentOutcomeCount")).isEqualTo(1);
        assertThat(result.getEndpoint()).contains("api.odcloud.kr/api/15144951");
        verify(restTemplate).getForEntity(any(URI.class), eq(String.class));
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("kiat-rnd-outcomes.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KiatRndOutcomesConnector connector, Path csv, String today, String apiKey,
                           String nextUpdateDate) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15144951/v1/uddi:552c6b5b-5d8b-4aa5-8859-d0441cf28de0");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", today);
        ReflectionTestUtils.setField(connector, "datasetDate", "2026-04-10");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "portalTotalRows", 3);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "KR", product);
    }
}
