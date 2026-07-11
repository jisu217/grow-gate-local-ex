package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KatsForeignRecallConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void collectUsesSafetyKoreaAuthKeyHeaderAndScoresProductMentionAsRiskSignal() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":1000,"data":[
                          {"recallId":"F-1","productName":"전기요","brand":"Brand A","modelName":"E-100","publishedAt":"2026-07-01"},
                          {"recallId":"F-2","productName":"완구","brand":"","modelName":"T-1","publishedAt":"2026-07-02"}
                        ]}"""));
        KatsForeignRecallConnector connector = new KatsForeignRecallConnector(restTemplate, objectMapper);
        configure(connector, "secret-safety-key");

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        Map<String, Object> evidence = (Map<String, Object>) result.getNormalizedPayload().get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KATS_FOREIGN_RECALL);
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.getScoreContribution()).isEqualTo(-3);
        assertThat(metrics.get("recordCount")).isEqualTo(2);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(metrics.get("brandRows")).isEqualTo(1L);
        assertThat(metrics.get("modelRows")).isEqualTo(2L);
        assertThat(evidence).containsEntry("collectionMode", "safetykorea_v2_authkey_header");
        assertThat(evidence).containsEntry("authHeader", "AuthKey");
        assertThat(uriCaptor.getValue().toString()).doesNotContain("secret-safety-key");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("AuthKey")).isEqualTo("secret-safety-key");
    }

    private void configure(KatsForeignRecallConnector connector, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://www.safetykorea.kr/openapi/api/recall/fRecallList.json");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "apiPerPage", 1000);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "630110", "KR", product);
    }
}
