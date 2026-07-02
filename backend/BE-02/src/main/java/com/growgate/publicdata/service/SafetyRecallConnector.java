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

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recallCount", "recall/리콜/위해/부적합/결함 records or keywords",
                "highRiskCount", "fire/burn/overheat/explosion/화재/과열/폭발/감전 risk text",
                "recordCount", "provider response recall item count"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int recordCount = Math.max(maxArraySize(root), Math.max(0, objectCount(root) - 1));
        int recallKeywordCount = textHitCount(root, "recall|리콜|위해|부적합|결함");
        int highRiskCount = textHitCount(root, "fire|burn|overheat|explosion|화재|과열|폭발|감전");
        int recallCount = Math.max(recallKeywordCount, recordCount);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recallCount", recallCount);
        metrics.put("highRiskCount", highRiskCount);
        metrics.put("recordCount", recordCount);

        int contribution = fallbackUsed ? -2 : recallCount == 0 ? 4 : -Math.min(8, recallCount + highRiskCount);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.9,
                0.35 + Math.min(0.30, recordCount * 0.04)
                        + Math.min(0.20, recallKeywordCount * 0.04));
        return signal(request, metrics, contribution, confidence);
    }
}
