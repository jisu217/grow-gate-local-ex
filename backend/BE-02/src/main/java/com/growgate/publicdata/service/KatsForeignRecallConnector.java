package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class KatsForeignRecallConnector extends AbstractPublicDataConnector {

    private static final String SAFETY_KOREA_API_NAME = "SafetyKorea Open API v2.0 국외 리콜정보";
    private static final List<String> SAMPLE_KEYS = List.of("recallId", "productName", "brand", "modelName", "publishedAt");

    @Value("${public-data.kats-foreign-recall.url:https://www.safetykorea.kr/openapi/api/recall/fRecallList.json}")
    private String endpoint;

    @Value("${public-data.kats-foreign-recall.key:${SAFETY_KOREA_AUTH_KEY:}}")
    private String apiKey;

    @Value("${public-data.kats-foreign-recall.api-per-page:1000}")
    private int apiPerPage;

    public KatsForeignRecallConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KATS_FOREIGN_RECALL;
    }

    @Override
    public String usedFor() {
        return "recall";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + normalize(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        return getJsonWithHeaderKey(request, endpoint, apiKey, "AuthKey", Map.of(
                "page", "1",
                "perPage", String.valueOf(Math.max(1, apiPerPage))
        ));
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "SafetyKorea fRecallList row count",
                "productKeywordMentionCount", "rows where productName token appears in product/brand/model/recall text",
                "brandRows", "rows with brand populated",
                "modelRows", "rows with model populated",
                "latestPublishedAt", "latest public announcement date found in response"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int mentions = asInt(metrics.get("productKeywordMentionCount"));
        int contribution = fallbackUsed ? -1 : mentions > 0 ? -3 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.72,
                0.36 + Math.min(0.20, rows.size() / 1000.0) + (mentions > 0 ? 0.14 : 0.0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "safetykorea_v2_authkey_header");
        evidence.put("apiName", SAFETY_KOREA_API_NAME);
        evidence.put("operationPath", "/openapi/api/recall/fRecallList.json");
        evidence.put("authHeader", "AuthKey");
        evidence.put("apiKeyStored", false);
        return evidence;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        List<String> dates = rows.stream().map(row -> safe(first(row,
                        "publishedAt", "공표일", "공표일자", "recallDate", "date")))
                .filter(value -> !value.isBlank()).sorted().toList();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("productKeywordMentionCount", productKeywordMentionCount(request.getProductName(), rows));
        metrics.put("brandRows", rows.stream().filter(row -> !first(row, "brand", "브랜드", "상표명").isBlank()).count());
        metrics.put("modelRows", rows.stream().filter(row -> !first(row, "modelName", "모델명", "모델").isBlank()).count());
        metrics.put("latestPublishedAt", dates.isEmpty() ? "" : dates.get(dates.size() - 1));
        metrics.put("schemaVersion", "SAFETYKOREA_FOREIGN_RECALL_v2");
        return metrics;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        JsonNode root = readTree(rawPayload);
        collectRows(root, rows);
        return rows;
    }

    private void collectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && looksLikeForeignRecallRow(node)) {
            Map<String, String> row = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> row.put(entry.getKey(), entry.getValue().asText("")));
            rows.add(row);
            return;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) collectRows(child, rows);
        }
    }

    private boolean looksLikeForeignRecallRow(JsonNode node) {
        return hasAny(node, "recallId", "rclId", "리콜ID", "제품명", "productName", "brand", "브랜드", "모델명");
    }

    private boolean hasAny(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) return true;
        }
        return false;
    }

    private int productKeywordMentionCount(String productName, List<Map<String, String>> rows) {
        List<String> tokens = productTokens(productName);
        if (tokens.isEmpty()) return 0;
        return (int) rows.stream().map(this::rowText).filter(text -> tokens.stream().anyMatch(text::contains)).count();
    }

    private List<String> productTokens(String productName) {
        String normalized = normalize(productName);
        if (normalized.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        tokens.add(normalized);
        for (String token : safe(productName).split("[\\s,/()\\-]+")) {
            String value = normalize(token);
            if (value.length() >= 3 && !tokens.contains(value)) tokens.add(value);
        }
        return tokens;
    }

    private String rowText(Map<String, String> row) {
        return normalize(String.join(" ",
                first(row, "productName", "제품명", "품목명"),
                first(row, "brand", "브랜드", "상표명"),
                first(row, "modelName", "모델명", "모델"),
                first(row, "recallReason", "리콜사유", "위해내용", "결함내용")
        ));
    }

    private String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = safe(row.get(key));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
    }

    private String normalize(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }
}
