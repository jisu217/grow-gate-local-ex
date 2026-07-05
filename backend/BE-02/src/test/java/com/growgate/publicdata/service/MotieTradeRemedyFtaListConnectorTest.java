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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MotieTradeRemedyFtaListConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesCsvBeforeScheduledUpdateAndMatchesTargetCountry() throws Exception {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieTradeRemedyFtaListConnector connector = new MotieTradeRemedyFtaListConnector(restTemplate, objectMapper);
        Path csv = writeCsv("자유무역협정아이디,자유무역협정명,사용여부\n"
                + "1,한-칠레 FTA,Y\n"
                + "2,한-미국 FTA,Y\n"
                + "3,한-EU FTA,Y\n"
                + "4,테스트 비활성 FTA,N\n");
        configure(connector, csv, "", "2026-07-05", "2026-08-25", 4);

        PublicDataConnectorResult result = connector.collect(request("US"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_TRADE_REMEDY_FTA_LIST);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.getScoreContribution()).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(4);
        assertThat(metrics.get("activeAgreementCount")).isEqualTo(3L);
        assertThat(metrics.get("targetCountryAgreementMatch")).isEqualTo(true);
        assertThat((List<String>) metrics.get("targetCountryAgreementNames")).containsExactly("한-미국 FTA");
        assertThat(evidence.get("updateDue")).isEqualTo(false);
        assertThat(evidence.get("sameAsDataGoKrMetadata")).isEqualTo(true);
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
                          {"자유무역협정아이디":"1","자유무역협정명":"한-EU FTA","사용여부":"Y"},
                          {"자유무역협정아이디":"2","자유무역협정명":"한-미국 FTA","사용여부":"Y"}
                        ]}"""));
        MotieTradeRemedyFtaListConnector connector = new MotieTradeRemedyFtaListConnector(restTemplate, objectMapper);
        configure(connector, null, "test-key", "2026-08-25", "2026-08-25", 16);

        PublicDataConnectorResult result = connector.collect(request("DE"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(uriCaptor.getValue().toString()).contains("https://api.odcloud.kr/api/15117950/v1/");
        assertThat(uriCaptor.getValue().toString()).contains("serviceKey=test-key");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("targetCountryAgreementMatch")).isEqualTo(true);
        assertThat((List<String>) metrics.get("targetCountryAgreementNames")).containsExactly("한-EU FTA");
        assertThat(result.getScoreContribution()).isEqualTo(1);
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("motie-trade-remedy-fta-list.csv");
        Files.writeString(csv, content, Charset.forName("MS949"));
        return csv;
    }

    private void configure(MotieTradeRemedyFtaListConnector connector, Path csv, String apiKey,
                           String todayOverride, String nextUpdateDate, int portalTotalRows) {
        if (csv != null) {
            ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());
        }
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://api.odcloud.kr/api/15117950/v1/uddi:dd7abd3f-f7fc-4064-9377-ffd563de8d12");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "datasetDate", "2025-08-25");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", nextUpdateDate);
        ReflectionTestUtils.setField(connector, "swaggerLatestDatasetDate", "2025-08-25");
        ReflectionTestUtils.setField(connector, "portalTotalRows", portalTotalRows);
        ReflectionTestUtils.setField(connector, "apiPerPage", 100);
        ReflectionTestUtils.setField(connector, "todayOverride", todayOverride);
    }

    private PublicDataRequest request(String country) {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", country, "배터리팩");
    }
}
