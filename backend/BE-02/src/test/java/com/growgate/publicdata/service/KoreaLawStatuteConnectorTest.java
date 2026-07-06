package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KoreaLawStatuteConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesManualLawSearchQueryAndCountsXmlLawRows() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        <LawSearch>
                          <law><법령명한글>대외무역법</법령명한글></law>
                          <law><법령명한글>무역거래기반 조성에 관한 법률</법령명한글></law>
                        </LawSearch>"""));
        KoreaLawStatuteConnector connector = new KoreaLawStatuteConnector(restTemplate, objectMapper);
        ReflectionTestUtils.setField(connector, "endpoint", "https://www.law.go.kr/DRF/lawSearch.do");
        ReflectionTestUtils.setField(connector, "apiKey", "law-oc");
        ReflectionTestUtils.setField(connector, "display", 10);
        ReflectionTestUtils.setField(connector, "responseType", "XML");
        ReflectionTestUtils.setField(connector, "defaultQuery", "무역");

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "KR", "배터리", "무역"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String decodedUri = URLDecoder.decode(uriCaptor.getValue().toString(), StandardCharsets.UTF_8);
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");

        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KOREA_LAW_STATUTES);
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(decodedUri).contains("target=law");
        assertThat(decodedUri).contains("type=XML");
        assertThat(decodedUri).contains("query=무역");
        assertThat(decodedUri).contains("OC=law-oc");
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("query")).isEqualTo("무역");
    }
}
