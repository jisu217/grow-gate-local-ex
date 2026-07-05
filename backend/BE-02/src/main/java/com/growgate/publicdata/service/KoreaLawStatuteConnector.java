package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class KoreaLawStatuteConnector extends AbstractPublicDataConnector {

    @Value("${public-data.korea-law-statutes.url:https://www.law.go.kr/DRF/lawSearch.do}")
    private String endpoint;

    @Value("${public-data.korea-law-statutes.key:${LAW_OPENAPI_OC:}}")
    private String apiKey;

    @Value("${public-data.korea-law-statutes.display:20}")
    private int display;

    public KoreaLawStatuteConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KOREA_LAW_STATUTES;
    }

    @Override
    public String usedFor() {
        return "regulation";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + queryFor(request);
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJsonWithKeyParam(request, endpoint, apiKey, "OC", Map.of(
                "target", "law",
                "type", "JSON",
                "display", String.valueOf(Math.max(1, display)),
                "page", "1",
                "query", queryFor(request)
        ));
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "lawSearch law item count",
                "certificationLawHits", "rows mentioning 인증/안전/전기용품",
                "productNameHits", "rows mentioning productName tokens",
                "query", "domestic law search query"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        int rowCount = lawCount(root);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", fallbackUsed ? 0 : rowCount);
        metrics.put("certificationLawHits", textHitCount(root, "인증|안전|전기용품|생활용품|제품안전|시험성적"));
        metrics.put("productNameHits", productHitCount(root, request.getProductName()));
        metrics.put("query", queryFor(request));
        metrics.put("decisionImpact", "국내 법령·인증 설명 보강용입니다. EU 규제, CE/REACH/RoHS 적합성 또는 최종 Gate를 대체하지 않습니다.");
        int contribution = fallbackUsed ? -1 : rowCount > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : rowCount > 0 ? 0.62 : 0.30;
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private int lawCount(JsonNode root) {
        JsonNode law = root.path("LawSearch").path("law");
        if (law.isArray()) return law.size();
        if (law.isObject()) return 1;
        return maxArraySize(root);
    }

    private int productHitCount(JsonNode root, String productName) {
        String normalized = safe(productName).trim();
        if (normalized.isBlank()) return 0;
        int hits = 0;
        for (String token : normalized.split("[\\s,/()\\-]+")) {
            if (token.length() >= 2) hits += textHitCount(root, token);
        }
        return hits;
    }

    private String queryFor(PublicDataRequest request) {
        String product = safe(request.getProductName()).toLowerCase(Locale.ROOT);
        if (product.contains("battery") || product.contains("배터리") || product.contains("전지")) {
            return "전기용품 안전관리";
        }
        if (product.contains("chemical") || product.contains("화학")) {
            return "생활화학제품 안전관리";
        }
        if (product.contains("gas") || product.contains("수소")) {
            return "고압가스 안전관리";
        }
        return "제품안전 인증";
    }
}
