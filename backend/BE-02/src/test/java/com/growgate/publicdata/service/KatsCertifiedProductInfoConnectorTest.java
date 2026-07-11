package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

class KatsCertifiedProductInfoConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void collectUsesSafetyKoreaAuthKeyHeaderWhenScheduledUpdateIsDue() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"page":1,"perPage":1000,"totalCount":1,"data":[
                          {"인증번호":"SU-1","제품명":"전기요","모델명":"E-100","인증상태":"적합","제조사국가명":"대한민국"}
                        ]}"""));
        KatsCertifiedProductInfoConnector connector = new KatsCertifiedProductInfoConnector(restTemplate, objectMapper);
        configure(connector, "secret-safety-key");

        PublicDataConnectorResult result = connector.collect(request("전기요"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(), eq(String.class));
        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(metrics.get("recordCount")).isEqualTo(1);
        assertThat(metrics.get("productKeywordMentionCount")).isEqualTo(1);
        assertThat(uriCaptor.getValue().toString()).doesNotContain("secret-safety-key");
        assertThat(entityCaptor.getValue().getHeaders().getFirst("AuthKey")).isEqualTo("secret-safety-key");
        assertThat(result.getEndpoint()).contains("safetykorea.kr/openapi/api/cert/certificationList.json");
    }

    private void configure(KatsCertifiedProductInfoConnector connector, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint",
                "https://www.safetykorea.kr/openapi/api/cert/certificationList.json");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "todayOverride", "2026-08-01");
        ReflectionTestUtils.setField(connector, "nextUpdateDate", "2026-08-01");
        ReflectionTestUtils.setField(connector, "apiPerPage", 1000);
    }

    private PublicDataRequest request(String product) {
        return new PublicDataRequest("diagnosis-1", "user-1", "630110", "KR", product);
    }
}
