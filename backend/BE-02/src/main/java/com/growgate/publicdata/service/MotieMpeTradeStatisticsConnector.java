package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MotieMpeTradeStatisticsConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_소재부품장비 무역통계 수출입 총괄_20251231";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15099627/v1";
    private static final List<String> HEADERS = List.of("지역", "소재부품장비품목코드", "소재부품장비품목명");
    private static final Pattern YEAR_EXPORT = Pattern.compile("^(20\\d{2})수출\\(달러\\)$");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.motie-mpe-trade-statistics.url:https://api.odcloud.kr/api/15099627/v1/uddi:64f277d4-70bb-49a7-bb50-a4d56466ea3e}")
    private String endpoint;

    @Value("${public-data.motie-mpe-trade-statistics.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.motie-mpe-trade-statistics.csv-resource:classpath:publicdata/motie-mpe-trade-statistics-20251231.csv}")
    private String csvResource;

    @Value("${public-data.motie-mpe-trade-statistics.dataset-date:2025-12-31}")
    private String datasetDate;

    @Value("${public-data.motie-mpe-trade-statistics.registered-date:2026-06-17}")
    private String registeredDate;

    @Value("${public-data.motie-mpe-trade-statistics.modified-date:2026-06-18}")
    private String modifiedDate;

    @Value("${public-data.motie-mpe-trade-statistics.next-update-date:2027-06-17}")
    private String nextUpdateDate;

    @Value("${public-data.motie-mpe-trade-statistics.portal-total-rows:20352}")
    private int portalTotalRows;

    @Value("${public-data.motie-mpe-trade-statistics.api-per-page:25000}")
    private int apiPerPage;

    @Value("${public-data.motie-mpe-trade-statistics.today-override:}")
    private String todayOverride;

    public MotieMpeTradeStatisticsConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_MPE_TRADE_STATISTICS;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + countryLabel(request.getTargetCountry()) + ":" + productKeyword(request.getProductName());
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
                "regionMatchRowCount", "지역 equals normalized target country label",
                "productCategoryMatchRowCount", "소재부품장비품목명 contains product keyword",
                "matchedRowCount", "region and product category matched row count",
                "latestExportUsd", "latest YYYY수출(달러) in matched rows",
                "latestImportUsd", "latest YYYY수입(달러) in matched rows"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int matched = asInt(metrics.get("matchedRowCount"), 0);
        long balance = asLong(metrics.get("latestTradeBalanceUsd"), 0);
        int contribution = fallbackUsed ? -1 : matched == 0 ? 0 : 1 + (balance > 0 ? 1 : balance < 0 ? -1 : 0);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.82,
                0.38 + Math.min(0.18, rows.size() / 6000.0) + (matched > 0 ? 0.18 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "api_after_scheduled_update");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("datasetDate", datasetDate);
        evidence.put("registeredDate", registeredDate);
        evidence.put("modifiedDate", modifiedDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateCycle", "annual");
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            int contribution = contribution(metrics);
            double confidence = confidence(metrics, rows.size());
            String rawPayload = toJson(Map.of("source", "csv", "fileDataName", FILE_DATA_NAME, "matchedData", matchedRows(request, rows)));
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
        normalized.put("domainSignals", extractDomainSignals(request, rawPayload, false));
        normalized.put("metrics", metrics);
        normalized.put("metricSchema", metrics.keySet());
        normalized.put("fieldMapping", fieldMapping());
        normalized.put("scoreContribution", contribution);
        normalized.put("confidence", confidence);
        normalized.put("rawCount", rows.size());
        normalized.put("normalizedCount", rows.size());
        normalized.put("filteredCount", asInt(metrics.get("matchedRowCount"), 0));
        normalized.put("zeroReason", asInt(metrics.get("matchedRowCount"), 0) == 0 ? "NO_REGION_PRODUCT_MATCH" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("lastPublishedAt", modifiedDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", asInt(metrics.get("matchedRowCount"), 0),
                "zeroReason", asInt(metrics.get("matchedRowCount"), 0) == 0 ? "NO_REGION_PRODUCT_MATCH" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "MPE trade statistics contribution is deterministic and capped; matched positive trade balance adds a small market reference signal.");
        normalized.put("decisionImpact", "소재부품장비 지역/품목 수출입 통계 참고 신호이며 수요 예측, 가격 전망, 최종 사업 실행 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> csvEvidence(int rowCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("collectionMode", "csv_first_until_scheduled_update");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("csvResource", csvResource);
        evidence.put("datasetDate", datasetDate);
        evidence.put("registeredDate", registeredDate);
        evidence.put("modifiedDate", modifiedDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateCycle", "annual");
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
        List<Map<String, String>> regionRows = rows.stream()
                .filter(row -> countryLabel(request.getTargetCountry()).equals(row.get("지역")))
                .toList();
        List<Map<String, String>> productRows = rows.stream()
                .filter(row -> productMatches(row.get("소재부품장비품목명"), request.getProductName()))
                .toList();
        List<Map<String, String>> matched = matchedRows(request, rows);
        int latestYear = latestYear(rows);
        long latestExport = matched.stream().mapToLong(row -> parseLong(row.get(latestYear + "수출(달러)"))).sum();
        long latestImport = matched.stream().mapToLong(row -> parseLong(row.get(latestYear + "수입(달러)"))).sum();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("regionMatchRowCount", regionRows.size());
        metrics.put("productCategoryMatchRowCount", productRows.size());
        metrics.put("matchedRowCount", matched.size());
        metrics.put("latestYear", latestYear);
        metrics.put("latestExportUsd", latestExport);
        metrics.put("latestImportUsd", latestImport);
        metrics.put("latestTradeBalanceUsd", latestExport - latestImport);
        return metrics;
    }

    private List<Map<String, String>> matchedRows(PublicDataRequest request, List<Map<String, String>> rows) {
        String country = countryLabel(request.getTargetCountry());
        return rows.stream()
                .filter(row -> country.equals(row.get("지역")))
                .filter(row -> productMatches(row.get("소재부품장비품목명"), request.getProductName()))
                .sorted(Comparator.comparing(row -> safe(row.get("소재부품장비품목코드"))))
                .toList();
    }

    private int contribution(Map<String, Object> metrics) {
        int matched = asInt(metrics.get("matchedRowCount"), 0);
        long balance = asLong(metrics.get("latestTradeBalanceUsd"), 0);
        if (matched == 0) return 0;
        return Math.max(-2, Math.min(4, 1 + (balance > 0 ? 1 : balance < 0 ? -1 : 0)));
    }

    private double confidence(Map<String, Object> metrics, int rowCount) {
        int matched = asInt(metrics.get("matchedRowCount"), 0);
        return Math.min(0.80, 0.36 + Math.min(0.18, rowCount / 6000.0) + (matched > 0 ? 0.18 : 0));
    }

    private int latestYear(List<Map<String, String>> rows) {
        return rows.stream()
                .flatMap(row -> row.keySet().stream())
                .map(YEAR_EXPORT::matcher)
                .filter(Matcher::matches)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max()
                .orElse(0);
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        JsonNode root = readTree(rawPayload);
        collectObjectRows(root, rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("지역")) {
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

    private String countryLabel(String country) {
        String value = safe(country).trim().toUpperCase(Locale.ROOT);
        if (value.equals("DE") || value.contains("GERMANY") || value.contains("독일")) return "독일";
        if (value.equals("US") || value.equals("USA") || value.contains("UNITED STATES") || value.contains("미국")) return "미국";
        if (value.equals("JP") || value.contains("JAPAN") || value.contains("일본")) return "일본";
        if (value.equals("CN") || value.contains("CHINA") || value.contains("중국")) return "중국";
        if (value.equals("VN") || value.contains("VIETNAM") || value.contains("베트남")) return "베트남";
        return safe(country).trim();
    }

    private String productKeyword(String productName) {
        String value = safe(productName);
        if (value.contains("축전지") || value.toLowerCase(Locale.ROOT).contains("battery")) return "축전지";
        return value;
    }

    private boolean productMatches(String category, String productName) {
        String target = productKeyword(productName);
        return !target.isBlank() && safe(category).contains(target);
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.round(Double.parseDouble(value.replace(",", "").trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long asLong(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private List<Map<String, String>> readCsvRows() throws IOException {
        Resource resource = resourceLoader.getResource(csvResource);
        byte[] bytes;
        try (var input = resource.getInputStream()) {
            bytes = input.readAllBytes();
        }
        String text = decode(bytes);
        List<String> lines = text.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) return List.of();
        List<String> header = parseCsvLine(lines.get(0));
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> values = parseCsvLine(lines.get(i));
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < header.size(); j++) {
                row.put(header.get(j), j < values.size() ? values.get(j) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (Exception ignored) {
            return new String(bytes, Charset.forName("MS949"));
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
