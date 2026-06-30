package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KiprisFtoConnector extends AbstractPublicDataConnector {

    @Value("${public-data.kipris-fto.url:}")
    private String endpoint;

    @Value("${public-data.kipris-fto.key:}")
    private String apiKey;

    public KiprisFtoConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KIPRIS_FTO;
    }

    @Override
    public String usedFor() {
        return "fto";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getProductName()) + ":" + safe(request.getTargetCountry());
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
        return fallbackUsed ? -3 : 7;
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }
}
