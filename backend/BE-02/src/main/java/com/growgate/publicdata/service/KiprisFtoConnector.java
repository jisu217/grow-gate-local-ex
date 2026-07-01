package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
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

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int patentCount = Math.max(maxArraySize(root), Math.max(0, objectCount(root) - 1));
        int claimsCount = (int) sumNumericFields(root, "claimCount", "claims", "청구항수");
        int productMentions = textHitCount(root, safe(request.getProductName()));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("patentCount", patentCount);
        metrics.put("claimsCount", claimsCount);
        metrics.put("productMentions", productMentions);
        metrics.put("recordCount", patentCount);

        int contribution = fallbackUsed ? -3 : patentCount == 0 ? 3 : -Math.min(9, Math.max(1, patentCount / 2) + Math.min(3, productMentions));
        double confidence = fallbackUsed ? 0.0 : Math.min(0.9,
                0.35 + Math.min(0.30, patentCount * 0.03)
                        + Math.min(0.15, productMentions * 0.05)
                        + (claimsCount > 0 ? 0.10 : 0));
        return signal(request, metrics, contribution, confidence);
    }
}
