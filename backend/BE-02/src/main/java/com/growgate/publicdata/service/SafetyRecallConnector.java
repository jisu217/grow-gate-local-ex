package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class SafetyRecallConnector extends AbstractPublicDataConnector {

    @Value("${public-data.safety-recall.url:}")
    private String endpoint;

    @Value("${public-data.safety-recall.key:}")
    private String apiKey;

    public SafetyRecallConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.SAFETY_RECALL;
    }

    @Override
    public String usedFor() {
        return "recall";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getHsCode()) + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJson(request, endpoint, apiKey, Map.of(
                "hsCode", safe(request.getHsCode()),
                "productName", safe(request.getProductName())
        ));
    }

    @Override
    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -2 : 5;
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }
}
