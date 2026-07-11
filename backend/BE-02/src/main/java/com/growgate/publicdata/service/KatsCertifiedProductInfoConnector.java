package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class KatsCertifiedProductInfoConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_국가기술표준원_제품안전_인증제품 정보_20220801";
    private static final String SWAGGER_URL = "https://www.safetykorea.kr/release/openapi";
    private static final List<String> HEADERS = List.of("인증번호", "인증구분명", "인증상태", "제품명", "모델명", "제조사국가명");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kats-certified-product-info.url:}")
    private String endpoint;

    @Value("${public-data.kats-certified-product-info.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kats-certified-product-info.csv-resource:classpath:publicdata/kats-certified-product-info-20220801.csv}")
    private String csvResource;

    @Value("${public-data.kats-certified-product-info.dataset-date:2022-08-01}")
    private String datasetDate;

    @Value("${public-data.kats-certified-product-info.next-update-date:2026-08-01}")
    private String nextUpdateDate;

    @Value("${public-data.kats-certified-product-info.portal-total-rows:152433}")
    private int portalTotalRows;

    @Value("${public-data.kats-certified-product-info.api-per-page:5000}")
    private int apiPerPage;

    @Value("${public-data.kats-certified-product-info.today-override:}")
    private String todayOverride;

    public KatsCertifiedProductInfoConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KATS_CERTIFIED_PRODUCT_INFO;
    }

    @Override
    public String usedFor() {
        return "regulation";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + normalize(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        if (shouldUseApi()) {
            return getJsonWithHeaderKey(request, endpoint, apiKey, "AuthKey", Map.of(
                    "page", "1",
                    "perPage", String.valueOf(Math.max(1, apiPerPage)),
                    "returnType", "JSON"
            ));
        }
        return collectCsv(request);
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "CSV/API data[] row count",
                "productKeywordMentionCount", "rows where productName token appears in product/model/category/manufacturer fields",
                "activeCertificationRows", "rows whose 인증상태 looks valid/active",
                "electricSafetyRows", "rows mentioning 전기용품",
                "importRows", "rows marked 수입",
                "targetCountryManufacturerRows", "rows whose 제조사국가명 matches target country"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int mentions = asInt(metrics.get("productKeywordMentionCount"));
        int contribution = fallbackUsed ? -1 : mentions > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.70, 0.38 + Math.min(0.20, rows.size() / 10000.0) + (mentions > 0 ? 0.10 : 0.0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "csv_first_until_scheduled_update");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("datasetDate", datasetDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = PublicDataCsvSupport.readRows(resourceLoader, csvResource);
            Map<String, Object> metrics = metrics(request, rows);
            int mentions = asInt(metrics.get("productKeywordMentionCount"));
            int contribution = mentions > 0 ? 1 : 0;
            double confidence = Math.min(0.66, 0.36 + Math.min(0.20, rows.size() / 10000.0) + (mentions > 0 ? 0.10 : 0.0));
            String rawPayload = toJson(Map.of(
                    "source", "csv",
                    "fileDataName", FILE_DATA_NAME,
                    "recordCount", rows.size(),
                    "sampleRows", rows.stream().limit(5).toList()
            ));
            Map<String, Object> normalized = normalizedPayload(request, rows, metrics, contribution, confidence, rawPayload);
            return PublicDataConnectorResult.builder()
                    .provider(provider())
                    .endpoint("csv:" + csvResource)
                    .requestKey(requestKey(request))
                    .hsCode(request.getHsCode())
                    .targetCountry(request.getTargetCountry())
                    .matchedKey(matchedKey(request))
                    .usedFor(usedFor())
                    .rawPayload(rawPayload)
                    .normalizedPayload(normalized)
                    .scoreContribution(contribution)
                    .confidence(confidence)
                    .sourceType("cache")
                    .fallbackUsed(false)
                    .build();
        } catch (IOException e) {
            return fallback(request, "CSV_READ_ERROR: " + e.getMessage());
        }
    }

    private Map<String, Object> normalizedPayload(PublicDataRequest request, List<Map<String, String>> rows,
                                                  Map<String, Object> metrics, int contribution,
                                                  double confidence, String rawPayload) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", false);
        normalized.put("reason", "");
        normalized.put("rawLength", rawPayload.length());
        normalized.put("signal", updateDue() ? "file_stale" : "file");
        normalized.put("agency", provider().agency());
        normalized.put("ministry", provider().ministry());
        normalized.put("dataFamily", provider().dataFamily());
        normalized.put("motieRelated", provider().motieRelated());
        normalized.put("refreshCycleHours", provider().refreshCycleHours());
        normalized.put("sourceName", provider().sourceName());
        normalized.put("officialUrl", provider().officialUrl());
        normalized.put("dataUsagePolicy", dataUsagePolicy());
        normalized.put("metrics", metrics);
        normalized.put("metricSchema", metrics.keySet());
        normalized.put("fieldMapping", fieldMapping());
        normalized.put("scoreContribution", contribution);
        normalized.put("confidence", confidence);
        normalized.put("rawCount", rows.size());
        normalized.put("normalizedCount", rows.size());
        normalized.put("filteredCount", asInt(metrics.get("productKeywordMentionCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "KATS certified product cache adds a small deterministic context signal only when product/model/category text directly matches.");
        normalized.put("decisionImpact", "국내 제품안전 인증제품 정보 참고자료입니다. EU 규제 적합성, 해외 인증 보유, 인증 유효성, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> zeroDiagnostics(int rowCount, Map<String, Object> metrics) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("provider", provider().name());
        diagnostics.put("endpointType", "csv_resource");
        diagnostics.put("usedFor", usedFor());
        diagnostics.put("sourceType", "cache");
        diagnostics.put("rawCount", rowCount);
        diagnostics.put("normalizedCount", rowCount);
        diagnostics.put("filteredCount", asInt(metrics.get("productKeywordMentionCount")));
        diagnostics.put("zeroReason", rowCount == 0 ? "EMPTY_CSV_RESOURCE" : "");
        diagnostics.put("sampleRawKeys", HEADERS);
        diagnostics.put("fallbackUsed", false);
        return diagnostics;
    }

    private Map<String, Object> csvEvidence(int rowCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("collectionMode", "csv_first_until_scheduled_update");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("csvResource", csvResource);
        evidence.put("datasetDate", datasetDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        evidence.put("localRowCount", rowCount);
        evidence.put("sameAsDataGoKrMetadata", rowCount == portalTotalRows);
        evidence.put("endpointConfigured", endpoint != null && !endpoint.isBlank());
        evidence.put("operationEndpoint", false);
        evidence.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        evidence.put("apiKeyStored", false);
        evidence.put("fallbackUsed", false);
        evidence.put("reason", "");
        return evidence;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("productKeywordMentionCount", productKeywordMentionCount(request.getProductName(), rows));
        metrics.put("activeCertificationRows", rows.stream().filter(row -> containsAny(row.get("인증상태"), List.of("적합", "유효", "인증"))).count());
        metrics.put("electricSafetyRows", rows.stream().filter(row -> containsAny(rowText(row), List.of("전기용품", "전기", "배터리", "충전"))).count());
        metrics.put("importRows", rows.stream().filter(row -> containsAny(row.get("수입제조"), List.of("수입"))).count());
        metrics.put("targetCountryManufacturerRows", targetCountryRows(request.getTargetCountry(), rows));
        metrics.put("datasetDate", datasetDate);
        return metrics;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        JsonNode root = readTree(rawPayload);
        collectObjectRows(root, rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && (node.has("인증번호") || node.has("제품명"))) {
            Map<String, String> row = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> row.put(entry.getKey(), entry.getValue().asText("")));
            rows.add(row);
            return;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) collectObjectRows(child, rows);
        }
    }

    private boolean shouldUseApi() {
        return updateDue() && endpoint != null && !endpoint.isBlank() && apiKey != null && !apiKey.isBlank();
    }

    private boolean updateDue() {
        return !currentDate().isBefore(LocalDate.parse(nextUpdateDate));
    }

    private LocalDate currentDate() {
        if (todayOverride != null && !todayOverride.isBlank()) return LocalDate.parse(todayOverride);
        return LocalDate.now();
    }

    private int productKeywordMentionCount(String productName, List<Map<String, String>> rows) {
        List<String> tokens = productTokens(productName);
        if (tokens.isEmpty()) return 0;
        return (int) rows.stream().map(this::rowText).filter(text -> tokens.stream().anyMatch(text::contains)).count();
    }

    private long targetCountryRows(String country, List<Map<String, String>> rows) {
        List<String> tokens = countryTokens(country);
        if (tokens.isEmpty()) return 0;
        return rows.stream().filter(row -> {
            String text = normalize(row.get("제조사국가명"));
            return tokens.stream().anyMatch(text::contains);
        }).count();
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

    private List<String> countryTokens(String country) {
        String normalized = normalize(country);
        if (normalized.isBlank()) return List.of();
        if (normalized.equals("us") || normalized.contains("미국")) return List.of("미국", "usa", "unitedstates");
        if (normalized.equals("de") || normalized.contains("독일")) return List.of("독일", "germany");
        if (normalized.equals("eu")) return List.of("독일", "프랑스", "이탈리아", "스페인", "네덜란드");
        return List.of(normalized);
    }

    private String rowText(Map<String, String> row) {
        return normalize(row.get("제품명") + " " + row.get("모델명") + " " + row.get("제품분류코드명") + " " + row.get("제조사명"));
    }

    private boolean containsAny(String text, List<String> tokens) {
        String value = normalize(text);
        return tokens.stream().map(this::normalize).anyMatch(value::contains);
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
    }

    private String normalize(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }
}
