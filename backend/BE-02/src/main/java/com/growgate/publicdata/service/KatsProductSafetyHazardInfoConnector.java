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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class KatsProductSafetyHazardInfoConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부 국가기술표준원_제품안전_위해정보_20250716";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15064255/v1";
    private static final List<String> HEADERS = List.of("품명", "제품분류", "제조국가명", "위해부위", "치료비용", "사고발생일", "날짜");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kats-product-safety-hazard-info.url:https://api.odcloud.kr/api/15064255/v1/uddi:b9de61e1-872c-4e55-9747-f4df5724aedc}")
    private String endpoint;

    @Value("${public-data.kats-product-safety-hazard-info.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kats-product-safety-hazard-info.csv-resource:classpath:publicdata/kats-product-safety-hazard-info-20250716.csv}")
    private String csvResource;

    @Value("${public-data.kats-product-safety-hazard-info.dataset-date:2025-07-16}")
    private String datasetDate;

    @Value("${public-data.kats-product-safety-hazard-info.next-update-date:2026-08-04}")
    private String nextUpdateDate;

    @Value("${public-data.kats-product-safety-hazard-info.portal-total-rows:176}")
    private int portalTotalRows;

    @Value("${public-data.kats-product-safety-hazard-info.api-per-page:500}")
    private int apiPerPage;

    @Value("${public-data.kats-product-safety-hazard-info.today-override:}")
    private String todayOverride;

    public KatsProductSafetyHazardInfoConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KATS_PRODUCT_SAFETY_HAZARD_INFO;
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
                "productKeywordMatch", "productName directly overlaps 품명",
                "nonZeroTreatmentCostCount", "rows where 치료비용 is greater than zero",
                "treatmentCostTotal", "sum of 치료비용",
                "domesticManufactureCount", "rows where 제조국가명 is 대한민국",
                "foreignManufactureCount", "rows where 제조국가명 is not blank and not 대한민국",
                "topProductName", "most frequent 품명",
                "topHazardBodyPart", "most frequent 위해부위",
                "latestAccidentDate", "max 사고발생일",
                "latestReportDate", "max 날짜"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        boolean matched = Boolean.TRUE.equals(metrics.get("productKeywordMatch"));
        int contribution = fallbackUsed ? -1 : matched && !rows.isEmpty() ? -2 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.70,
                0.36 + Math.min(0.14, rows.size() / 500.0) + (matched ? 0.12 : 0.0));
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
            boolean matched = Boolean.TRUE.equals(metrics.get("productKeywordMatch"));
            int contribution = matched && !rows.isEmpty() ? -2 : 0;
            double confidence = Math.min(0.64, 0.34 + Math.min(0.14, rows.size() / 500.0) + (matched ? 0.10 : 0.0));
            String rawPayload = toJson(Map.of("source", "csv", "fileDataName", FILE_DATA_NAME, "data", rows));
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
        normalized.put("filteredCount", Boolean.TRUE.equals(metrics.get("productKeywordMatch")) ? rows.size() : 0);
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", metrics.get("latestReportDate"));
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "KATS product safety hazard information adds a small deterministic risk signal only when product text directly overlaps 품명.");
        normalized.put("decisionImpact", "제품 위해정보 후보 참고자료입니다. 리콜 존재/부재, 안전인증 적합성, 제품 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.");
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
        diagnostics.put("filteredCount", Boolean.TRUE.equals(metrics.get("productKeywordMatch")) ? rowCount : 0);
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
        long treatmentCostTotal = rows.stream().mapToLong(row -> parseLong(row.get("치료비용"))).sum();
        long nonZeroTreatmentCostCount = rows.stream().filter(row -> parseLong(row.get("치료비용")) > 0).count();
        long domesticCount = rows.stream().filter(row -> safe(row.get("제조국가명")).equals("대한민국")).count();
        long foreignCount = rows.stream()
                .map(row -> safe(row.get("제조국가명")))
                .filter(country -> !country.isBlank() && !country.equals("대한민국"))
                .count();
        String latestAccidentDate = rows.stream().map(row -> safe(row.get("사고발생일"))).filter(value -> !value.isBlank()).max(String::compareTo).orElse("");
        String latestReportDate = rows.stream().map(row -> safe(row.get("날짜"))).filter(value -> !value.isBlank()).max(String::compareTo).orElse("");
        long distinctProductCount = rows.stream().map(row -> safe(row.get("품명"))).filter(value -> !value.isBlank()).distinct().count();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("productKeywordMatch", productKeywordMatch(request.getProductName(), rows));
        metrics.put("nonZeroTreatmentCostCount", nonZeroTreatmentCostCount);
        metrics.put("treatmentCostTotal", treatmentCostTotal);
        metrics.put("domesticManufactureCount", domesticCount);
        metrics.put("foreignManufactureCount", foreignCount);
        metrics.put("topProductName", topValue(rows, "품명"));
        metrics.put("topProductCategory", topCategory(rows));
        metrics.put("topHazardBodyPart", topValue(rows, "위해부위"));
        metrics.put("latestAccidentDate", latestAccidentDate);
        metrics.put("latestReportDate", latestReportDate);
        metrics.put("distinctProductCount", distinctProductCount);
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
        if (node.isObject() && node.has("품명")) {
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

    private boolean productKeywordMatch(String productName, List<Map<String, String>> rows) {
        String product = normalize(productName);
        if (product.isBlank()) return false;
        return rows.stream()
                .map(row -> normalize(row.get("품명")))
                .filter(value -> !value.isBlank())
                .anyMatch(name -> product.contains(name) || name.contains(product));
    }

    private String topCategory(List<Map<String, String>> rows) {
        Map<String, Long> counts = rows.stream()
                .map(this::category)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String category(Map<String, String> row) {
        String latest = safe(row.get("제품분류"));
        return latest.isBlank() ? safe(row.get("제품분류명")) : latest;
    }

    private String topValue(List<Map<String, String>> rows, String key) {
        Map<String, Long> counts = rows.stream()
                .map(row -> safe(row.get(key)))
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private long parseLong(String value) {
        String normalized = safe(value).replace(",", "").trim();
        if (normalized.isBlank()) return 0;
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String normalize(String value) {
        return Objects.toString(value, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
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
