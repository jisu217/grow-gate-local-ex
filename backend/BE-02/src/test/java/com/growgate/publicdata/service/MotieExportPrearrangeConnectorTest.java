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

class MotieExportPrearrangeConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectReturnsEmptyWhenApiKeyIsMissing() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MotieExportPrearrangeConnector connector = new MotieExportPrearrangeConnector(restTemplate, objectMapper);
        configure(connector, "");

        PublicDataConnectorResult result = connector.collect(request());

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");
        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.MOTIE_EXPORT_PREARRANGE);
        assertThat(result.getSourceType()).isEqualTo("empty");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(normalized.get("zeroReason")).isEqualTo("API_KEY_NOT_CONFIGURED");
        assertThat(evidence.get("operationEndpoint")).isEqualTo(true);
        assertThat(evidence.get("apiKeyConfigured")).isEqualTo(false);
        assertThat(evidence.get("operation")).isEqualTo("getExportPrearngeStat");
        verify(restTemplate, never()).getForEntity(any(URI.class), eq(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesExportPrearrangeOperationAndParsesXmlMetrics() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        <?xml version="1.0" encoding="UTF-8" standalone="true"?>
                        <response>
                          <header>
                            <resultCode>00</resultCode>
                            <resultMsg>NORMAL SERVICE.</resultMsg>
                          </header>
                          <body>
                            <items>
                              <item><amt>12460517219346</amt><byrorgnm>서울특별시</byrorgnm><cnt>38355</cnt><cur>KRW</cur></item>
                              <item><amt>272717192</amt><byrorgnm>서울특별시</byrorgnm><cnt>491</cnt><cur>EUR</cur></item>
                              <item><amt>112051073</amt><byrorgnm>울산광역시</byrorgnm><cnt>245</cnt><cur>USD</cur></item>
                            </items>
                            <numOfRows>3</numOfRows>
                            <pageNo>1</pageNo>
                            <totalCount>34</totalCount>
                          </body>
                        </response>"""));
        MotieExportPrearrangeConnector connector = new MotieExportPrearrangeConnector(restTemplate, objectMapper);
        configure(connector, "test-key");

        PublicDataConnectorResult result = connector.collect(request());

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String uri = uriCaptor.getValue().toString();

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, String> maskedParams = (Map<String, String>) normalized.get("requestParamsMasked");
        Map<String, Object> evidence = (Map<String, Object>) normalized.get("collectionEvidence");

        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(uri).contains("https://apis.data.go.kr/1450000/exportPrearngeInfoService/getExportPrearngeStat");
        assertThat(uri).contains("splOrgCd=11000");
        assertThat(uri).contains("numOfRows=10");
        assertThat(uri).contains("pageNo=1");
        assertThat(uri).contains("ServiceKey=test-key");
        assertThat(maskedParams.get("ServiceKey")).isEqualTo("[REDACTED]");
        assertThat(metrics.get("prearrangeRecordCount")).isEqualTo(3);
        assertThat(metrics.get("totalCount")).isEqualTo(34);
        assertThat(metrics.get("exportPrearrangedAmount")).isEqualTo(12460901987611L);
        assertThat(metrics.get("exportPrearrangedCount")).isEqualTo(39091);
        assertThat(metrics.get("currencyCount")).isEqualTo(3);
        assertThat(metrics.get("regionCount")).isEqualTo(2);
        assertThat(metrics.get("recordCount")).isEqualTo(3);
        assertThat(result.getScoreContribution()).isEqualTo(6);
        assertThat(evidence.get("collectionMode")).isEqualTo("live_operation_api");
        assertThat(evidence.get("documentName")).isEqualTo("IROS4_OA_DV_0401_OpenAPI활용가이드_무역정보(수출+예정정보서비스)_v1.6.docx");
    }

    private void configure(MotieExportPrearrangeConnector connector, String apiKey) {
        ReflectionTestUtils.setField(connector, "endpoint", "https://apis.data.go.kr/1450000/exportPrearngeInfoService");
        ReflectionTestUtils.setField(connector, "apiKey", apiKey);
        ReflectionTestUtils.setField(connector, "keyParam", "ServiceKey");
        ReflectionTestUtils.setField(connector, "operation", "getExportPrearngeStat");
        ReflectionTestUtils.setField(connector, "regionCode", "11000");
        ReflectionTestUtils.setField(connector, "numOfRows", 10);
        ReflectionTestUtils.setField(connector, "pageNo", 1);
        ReflectionTestUtils.setField(connector, "lookbackMonths", 2);
    }

    private PublicDataRequest request() {
        return new PublicDataRequest("diagnosis-1", "user-1", "850760", "US", "배터리팩");
    }
}
