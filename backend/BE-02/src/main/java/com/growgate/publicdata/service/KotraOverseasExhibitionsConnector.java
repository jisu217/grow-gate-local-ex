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
public class KotraOverseasExhibitionsConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "대한무역투자진흥공사 해외전시회 정보_20250630";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15003367/v1";
    private static final List<String> HEADERS = List.of("전시회", "전시회 영문명", "개최기간", "품목 대분류", "개최국가", "개최도시");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kotra-overseas-exhibitions.url:}")
    private String endpoint;

    @Value("${public-data.kotra-overseas-exhibitions.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kotra-overseas-exhibitions.csv-resource:classpath:publicdata/kotra-overseas-exhibitions-20250630.csv}")
    private String csvResource;

    @Value("${public-data.kotra-overseas-exhibitions.dataset-date:2025-06-30}")
    private String datasetDate;

    @Value("${public-data.kotra-overseas-exhibitions.next-update-date:2026-06-30}")
    private String nextUpdateDate;

    @Value("${public-data.kotra-overseas-exhibitions.portal-total-rows:105}")
    private int portalTotalRows;

    @Value("${public-data.kotra-overseas-exhibitions.api-per-page:200}")
    private int apiPerPage;

    @Value("${public-data.kotra-overseas-exhibitions.today-override:}")
    private String todayOverride;

    public KotraOverseasExhibitionsConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KOTRA_OVERSEAS_EXHIBITIONS;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + normalize(request.getTargetCountry()) + ":" + normalize(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        if (shouldUseApi()) {
            return getJson(request, endpoint, apiKey, Map.of(
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
                "targetCountryExhibitionCount", "rows matching targetCountry",
                "categoryMatchCount", "rows whose 품목 대분류 matches product category tokens",
                "electricalElectronicsExhibitionCount", "rows for 전기전자",
                "sampleExhibitions", "up to three matched exhibition names"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int matches = asInt(metrics.get("targetCountryExhibitionCount")) + asInt(metrics.get("categoryMatchCount"));
        int contribution = fallbackUsed ? -1 : matches > 0 ? 2 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.68, 0.38 + Math.min(0.12, rows.size() / 200.0) + (matches > 0 ? 0.14 : 0.0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = PublicDataCsvSupport.readRows(resourceLoader, csvResource);
            Map<String, Object> metrics = metrics(request, rows);
            int matches = asInt(metrics.get("targetCountryExhibitionCount")) + asInt(metrics.get("categoryMatchCount"));
            int contribution = matches > 0 ? 2 : 0;
            double confidence = Math.min(0.64, 0.36 + Math.min(0.12, rows.size() / 200.0) + (matches > 0 ? 0.14 : 0.0));
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
        normalized.put("filteredCount", asInt(metrics.get("targetCountryExhibitionCount")) + asInt(metrics.get("categoryMatchCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "KOTRA overseas exhibition cache adds deterministic support signal only when country/category rows match.");
        normalized.put("decisionImpact", "해외전시회 참가·시장 탐색 지원 참고자료입니다. 전시회 참가 가능성, 바이어 확보, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.");
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
        diagnostics.put("filteredCount", asInt(metrics.get("targetCountryExhibitionCount")) + asInt(metrics.get("categoryMatchCount")));
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
        List<String> matchedExhibitions = rows.stream()
                .filter(row -> countryMatches(request.getTargetCountry(), row) || categoryMatches(request.getProductName(), row))
                .map(row -> safe(row.get("전시회")))
                .filter(value -> !value.isBlank())
                .limit(3)
                .toList();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("targetCountryExhibitionCount", rows.stream().filter(row -> countryMatches(request.getTargetCountry(), row)).count());
        metrics.put("categoryMatchCount", rows.stream().filter(row -> categoryMatches(request.getProductName(), row)).count());
        metrics.put("electricalElectronicsExhibitionCount", rows.stream().filter(row -> containsAny(row.get("품목 대분류"), List.of("전기전자"))).count());
        metrics.put("sampleExhibitions", matchedExhibitions);
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
        if (node.isObject() && (node.has("전시회") || node.has("개최국가"))) {
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

    private boolean countryMatches(String country, Map<String, String> row) {
        List<String> tokens = countryTokens(country);
        String text = normalize(row.get("개최국가") + " " + row.get("개최도시"));
        return !tokens.isEmpty() && tokens.stream().anyMatch(text::contains);
    }

    private boolean categoryMatches(String productName, Map<String, String> row) {
        String product = normalize(productName);
        String category = normalize(row.get("품목 대분류"));
        if (product.contains("battery") || product.contains("배터리") || product.contains("전동") || product.contains("전기")) {
            return category.contains("전기전자");
        }
        if (product.contains("medical") || product.contains("의료")) return category.contains("의료");
        if (product.contains("food") || product.contains("식품")) return category.contains("식품");
        return !product.isBlank() && category.contains(product);
    }

    private List<String> countryTokens(String country) {
        String normalized = normalize(country);
        if (normalized.isBlank()) return List.of();
        if (normalized.equals("us") || normalized.contains("미국")) return List.of("미국", "라스베가스", "뉴욕", "시카고");
        if (normalized.equals("de") || normalized.contains("독일")) return List.of("독일", "프랑크푸르트", "하노버", "뮌헨");
        if (normalized.equals("eu")) return List.of("독일", "프랑스", "이탈리아", "스페인", "네덜란드");
        return List.of(normalized);
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
