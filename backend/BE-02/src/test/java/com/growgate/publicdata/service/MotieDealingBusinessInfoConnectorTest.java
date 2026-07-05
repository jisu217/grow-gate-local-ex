package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MotieDealingBusinessInfoConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReturnsEmptyWhenApiKeyIsMissing() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieDealingBusinessInfoConnector connector = new MotieDealingBusinessInfoConnector(restTemplate, objectMapper);
        configure(connector, "");

        PublicDataConnectorResult result = connector.collect(request("3208101000"));

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_DEALING_BUSINESS_INFO);
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(normalized.get("zeroReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(evidence.get("operationEndpoint")).isEqualTo(true);
        assertThat(evidence.get("apiKeyConfigured")).isEqualTo(false);
        assertThat(evidence.get("operation")).isEqualTo("getDelngSttusInqire");
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesHsSixOperationAndParsesXmlMetrics() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        <response>
                          <header>
                            <resultCode>00</resultCode>
                            <resultMsg>NORMAL SERVICE.</resultMsg>
                          </header>
                          <body>
                            <items>
                              <item><count>127</count><purchaserSidoName>경기도</purchaserSidoName><year>2019</year></item>
                              <item><count>139</count><purchaserSidoName>경상북도</purchaserSidoName><year>2019</year></item>
                              <item><count>1112</count><purchaserSidoName>기타</purchaserSidoName><year>2019</year></item>
                            </items>
                            <numOfRows>3</numOfRows>
                            <pageNo>1</pageNo>
                            <totalCount>10</totalCount>
                          </body>
                        </response>"""));
        MotieDealingBusinessInfoConnector connector = new MotieDealingBusinessInfoConnector(restTemplate, objectMapper);
        configure(connector, "test-key");

        PublicDataConnectorResult result = connector.collect(request("3208101000"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String uri = uriCaptor.getValue().toString();

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, String> maskedParams = (Map<String, String>) normalized.get("requestParamsMasked");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(uri).contains("https://apis.data.go.kr/1450000/delngBsshInfoService/getDelngSttusInqire");
        assertThat(uri).contains("hscdSix=320810");
        assertThat(uri).contains("ServiceKey=test-key");
        assertThat(maskedParams.get("ServiceKey")).isEqualTo("[REDACTED]");
        assertThat(metrics.get("dealingRecordCount")).isEqualTo(3);
        assertThat(metrics.get("totalCount")).isEqualTo(10);
        assertThat(metrics.get("tradeCount")).isEqualTo(1378L);
        assertThat(metrics.get("regionCount")).isEqualTo(3);
        assertThat(metrics.get("yearCount")).isEqualTo(1);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(result.getScoreContribution()).isEqualTo(5);
        assertThat(evidence.get("collectionMode")).isEqualTo("live_operation_api");
        assertThat(evidence.get("documentName")).isEqualTo("IROS4_OA_DV_0401_OpenAPI활용가이드_무역정보(거래업체정보서비스)_v1.7.docx");
    }

    private void configure(MotieDealingBusinessInfoConnector connector, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint", "https://apis.data.go.kr/1450000/delngBsshInfoService");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "keyParam", "ServiceKey");
        ReflectionTestUtils.setField(connector, "operation", "getDelngSttusInqire");
    }

    private PublicDataRequest request(String hsCode) {
        return new PublicDataRequest("diagnosis-1", "user-1", hsCode, "US", "페인트");
    }
}
