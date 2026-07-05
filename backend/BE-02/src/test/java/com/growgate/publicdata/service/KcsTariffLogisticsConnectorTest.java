package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KcsTariffLogisticsConnectorTest {

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReadsCsvReferenceForHsAndCountryRouteMatches() throws Exception {
        KcsTariffLogisticsConnector connector = new KcsTariffLogisticsConnector(mock(RestTemplate.class), objectMapper);
        Path csv = writeCsv("""
                scenario,prefix,hs_code,hs_name_ko,hs_name_en,hs_valid_from,hs_valid_to,tariff_type,tariff_rate_pct,tariff_valid_from,tariff_valid_to,mode,route,latest_period,latest_cost_index,avg_12m_cost_index,yoy_pct,period_count,source_files
                battery-pack-demo,850760,8507609000,기타,Other,2017-01-01,2026-12-31,A,8,20260101,20261231,air_import,유럽연합,2026-05,3000,2800,12.5,89,KCS
                battery-pack-demo,850760,8507609000,기타,Other,2017-01-01,2026-12-31,E1,5.6,20260101,20261231,sea_export,유럽연합,2026-05,1500,1800,-16.67,89,KCS
                marine-valve-demo,848180,8481801010,전기 작동식의 것,Electrically operated,2012-01-01,2026-12-31,A,8,20260101,20261231,sea_export,미국서부,2026-05,2500,2100,20,89,KCS
                """);
        ReflectionTestUtils.setField(connector, "csvResource", "file:" + csv.toAbsolutePath());

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "Germany / EU", "리튬이온 배터리팩"));

        Map<String, Object> payload = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) payload.get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) payload.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KCS_TARIFF_LOGISTICS);
        assertThat(result.getSourceType()).isEqualTo("cache");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(metrics.get("hsCodeMatchCount")).isEqualTo(2);
        assertThat(metrics.get("routeMatchCount")).isEqualTo(2);
        assertThat(metrics.get("mfnTariffRatePct")).isEqualTo(8.0);
        assertThat(metrics.get("preferentialTariffRatePct")).isEqualTo(5.6);
        assertThat((List<String>) metrics.get("matchedRoutes")).containsExactly("유럽연합");
        assertThat(result.getScoreContribution()).isGreaterThanOrEqualTo(2);
        assertThat(result.getConfidence()).isGreaterThan(0.6);
        assertThat(evidence.get("operationEndpoint")).isEqualTo(false);
        assertThat(payload.get("decisionImpact")).asString().contains("참고 신호");
    }

    private Path writeCsv(String content) throws Exception {
        Path csv = tempDir.resolve("kcs-tariff-logistics.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }
}
