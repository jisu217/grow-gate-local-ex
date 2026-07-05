package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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

class KsureCountryIndustryOverdueRateConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvWhenUpdateIsDueButApiKeyIsMissing() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        KsureCountryIndustryOverdueRateConnector connector = new KsureCountryIndustryOverdueRateConnector(restTemplate, objectMapper);
        Path csv = writeCsv("국가,업종명,2017,2018,2019,2020,2021\n"
                + "중국,전구 램프 및 조명장치 도매업,0.00%,0.00%,9.90%,14.50%,78.30%\n"
                + "중국,기계장비 도매업,0.00%,0.00%,0.00%,0.00%,20.00%\n"
                + "미국,전자부품 도매업,0.00%,0.00%,0.00%,0.00%,5.00%\n");
        configure(connector, csv, "", "2026-07-05", "2022-12-31", 3);

        PublicDataConnectorResult result = connector.collect(request("CN"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getScoreContribution()).isEqualTo(-2);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(metrics.get("countryCount")).isEqualTo(2L);
        assertThat(metrics.get("industryCount")).isEqualTo(3L);
        assertThat(metrics.get("targetCountryRowCount")).isEqualTo(2);
        assertThat(metrics.get("targetCountryAverageLatestOverduePct")).isEqualTo(49.2);
        assertThat(metrics.get("targetCountryHighOverdueRowCount")).isEqualTo(2L);
        assertThat(evidence.get("updateDue")).isEqualTo(true);
        assertThat(evidence.get("apiRefreshEligible")).isEqualTo(false);
        assertThat(evidence.get("apiRefreshSkippedReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(evidence.get("sameAsDataGoKrMetadata")).isEqualTo(true);
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesApiWhenScheduledUpdateIsDueAndKeyIsConfigured() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":1500,"totalCount":2,"currentCount":2,
                        "data":[
                          {"국가":"중국","업종명":"전구 램프 및 조명장치 도매업","2017":"0.00%","2018":"0.00%","2019":"9.90%","2020":"14.50%","2021":"78.30%"},
                          {"국가":"미국","업종명":"전자부품 도매업","2017":"0.00%","2018":"0.00%","2019":"0.00%","2020":"0.00%","2021":"5.00%"}
                        ]}"""));
        KsureCountryIndustryOverdueRateConnector connector = new KsureCountryIndustryOverdueRateConnector(restTemplate, objectMapper);
        configure(connector, null, "test-key", "2026-07-05", "2022-12-31", 1217);

        PublicDataConnectorResult result = connector.collect(request("US"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(uriCaptor.getValue().toString()).contains("https://api.odcloud.kr/api/15104444/v1/");
        assertThat(uriCaptor.getValue().toString()).contains("serviceKey=test-key");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("targetCountryRowCount")).isEqualTo(1);
        assertThat(metrics.get("targetCountryAverageLatestOverduePct")).isEqualTo(5.0);
        assertThat(result.getScoreContribution()).isEqualTo(0);
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("ksure-country-industry-overdue-rates.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(KsureCountryIndustryOverdueRateConnector connector, Path csv, String apiKey,
                           String todayOverride, String nextUpdateDate, int portalTotalRows) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15104444/v1/uddi:8deb1668-3932-4323-93bf-fad271cfdb0c");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "datasetDate", "2021-12-31");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "swaggerLatestDatasetDate", "2021-12-31");
        ReflectionTestUtils.setField(connector, "portalTotalRows", portalTotalRows);
        ReflectionTestUtils.setField(connector, "apiPerPage", 1500);
        ReflectionTestUtils.setField(connector, "todayOverride", todayOverride);
    }

    private PublicDataRequest request(String country) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", country, "배터리팩");
    }
}
