package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KcsTradeConnectorTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesTradeMetricsIntoNormalizedPayloadAndScoreContribution() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {"items":[
                          {"hsCode":"850760","country":"DE","importAmountUsd":123456,"exportAmountUsd":45678,"yoyGrowthPct":12.4},
                          {"hsCode":"850760","country":"DE","importAmountUsd":1000,"exportAmountUsd":2000,"yoyGrowthPct":8.0}
                        ]}
                        """));

        KcsTradeConnector connector = new KcsTradeConnector(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(connector, "endpoint", "https://api.example.test/kcs");
        ReflectionTestUtils.setField(connector, "apiKey", "test-service-key");

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "Battery pack"));

        Map<String, Object> metrics = (Map<String, Object>) result.getNormalizedPayload().get("metrics");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getUsedFor()).isEqualTo("market");
        assertThat(((Number) metrics.get("importAmountUsd")).longValue()).isEqualTo(124456L);
        assertThat(((Number) metrics.get("exportAmountUsd")).longValue()).isEqualTo(47678L);
        assertThat(((Number) metrics.get("yoyGrowthPct")).doubleValue()).isEqualTo(12.4);
        assertThat(((Number) metrics.get("recordCount")).intValue()).isGreaterThanOrEqualTo(2);
        assertThat(result.getScoreContribution()).isGreaterThanOrEqualTo(8);
        assertThat(result.getConfidence()).isGreaterThanOrEqualTo(0.7);
    }
}
