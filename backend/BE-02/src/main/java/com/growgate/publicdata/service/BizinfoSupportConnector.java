package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class BizinfoSupportConnector extends AbstractPublicDataConnector {

    @Value("${public-data.bizinfo-support.url:}")
    private String endpoint;

    @Value("${public-data.bizinfo-support.key:}")
    private String apiKey;

    public BizinfoSupportConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.BIZINFO_SUPPORT;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getTargetCountry()) + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJson(request, endpoint, apiKey, Map.of(
                "keyword", safe(request.getProductName()),
                "country", safe(request.getTargetCountry())
        ));
    }

    @Override
    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -1 : 6;
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }
}
