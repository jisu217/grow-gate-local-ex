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
public class KotraMarketConnector extends AbstractPublicDataConnector {

    @Value("${public-data.kotra-market.url:}")
    private String endpoint;

    @Value("${public-data.kotra-market.key:}")
    private String apiKey;

    public KotraMarketConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KOTRA_MARKET;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + safe(request.getTargetCountry()) + ":" + safe(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJson(request, endpoint, apiKey, Map.of(
                "country", safe(request.getTargetCountry()),
                "keyword", safe(request.getProductName())
        ));
    }

    @Override
    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -1 : 4;
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int articleCount = Math.max(maxArraySize(root), Math.max(0, objectCount(root) - 1));
        int countryMentions = textHitCount(root, safe(request.getTargetCountry()));
        int productMentions = textHitCount(root, safe(request.getProductName()));
        double growthPct = firstNumericField(root, "growthPct", "growth", "marketGrowth", "증가율").orElse(0);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("marketArticleCount", articleCount);
        metrics.put("countryMentions", countryMentions);
        metrics.put("productMentions", productMentions);
        metrics.put("yoyGrowthPct", Math.round(growthPct * 10.0) / 10.0);
        metrics.put("recordCount", articleCount);

        int contribution = fallbackUsed ? -1 : 1
                + Math.min(4, articleCount / 2)
                + Math.min(2, countryMentions)
                + Math.min(2, productMentions)
                + (growthPct > 0 ? 1 : 0);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.9,
                0.30 + Math.min(0.25, articleCount * 0.04)
                        + Math.min(0.20, (countryMentions + productMentions) * 0.05)
                        + (growthPct != 0 ? 0.10 : 0));
        return signal(request, metrics, contribution, confidence);
    }
}
