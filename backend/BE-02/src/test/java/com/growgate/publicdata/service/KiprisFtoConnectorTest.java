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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KiprisFtoConnectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SuppressWarnings("unchecked")
    void collectUsesConfirmedKiprisPlusWordSearchOperation() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(any(URI.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        <response>
                          <body>
                            <items>
                              <item>
                                <inventionTitle>센서 배터리팩 보호장치</inventionTitle>
                                <applicationNumber>1020260000000</applicationNumber>
                                <applicantName>공개자료 예시</applicantName>
                              </item>
                            </items>
                          </body>
                        </response>"""));
        KiprisFtoConnector connector = new KiprisFtoConnector(restTemplate, objectMapper);
        ReflectionTestUtils.setField(connector, "endpoint",
                "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getWordSearch");
        ReflectionTestUtils.setField(connector, "apiKey", "test-key");
        ReflectionTestUtils.setField(connector, "keyParam", "ServiceKey");
        ReflectionTestUtils.setField(connector, "pageSize", 10);

        PublicDataConnectorResult result = connector.collect(
                new PublicDataRequest("diagnosis-1", "user-1", "850760", "DE", "센서"));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), eq(String.class));
        String uri = uriCaptor.getValue().toString();

        Map<String, Object> normalized = result.getNormalizedPayload();
        Map<String, Object> metrics = (Map<String, Object>) normalized.get("metrics");
        Map<String, String> maskedParams = (Map<String, String>) normalized.get("requestParamsMasked");

        assertThat(result.getProvider()).isEqualTo(PublicDataProvider.KIPRIS_FTO);
        assertThat(result.getSourceType()).isEqualTo("live");
        assertThat(uri).contains("getWordSearch");
        assertThat(uri).contains("word=");
        assertThat(uri).contains("year=0");
        assertThat(uri).contains("pageNo=1");
        assertThat(uri).contains("numOfRows=10");
        assertThat(uri).contains("ServiceKey=test-key");
        assertThat(maskedParams.get("ServiceKey")).isEqualTo("[REDACTED]");
        assertThat(maskedParams).doesNotContainKey("serviceKey");
        assertThat(metrics.get("patentCount")).isEqualTo(1);
        assertThat(metrics.get("productMentions")).isEqualTo(1);
    }
}
