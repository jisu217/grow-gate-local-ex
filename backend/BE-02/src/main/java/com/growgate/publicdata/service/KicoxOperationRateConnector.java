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
import java.util.Map;

@Component
public class KicoxOperationRateConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국산업단지공단_국가산업단지 산업동향정보_가동률_20260331";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15085889/v1";
    private static final List<String> HEADERS = List.of(
            "산업단지",
            "제조업가동업체수(개사)",
            "생산(백만원)_최대생산능력",
            "생산(백만원)_당분기생산액",
            "가동률(퍼센트)_당분기",
            "가동률(퍼센트)_전분기",
            "가동률(퍼센트)_전분기대비(퍼센트포인트)"
    );
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kicox-operation-rate.url:https://api.odcloud.kr/api/15085889/v1/uddi:e32e7d2e-82a3-4cde-9d24-f3e90e2b1eb6}")
    private String endpoint;

    @Value("${public-data.kicox-operation-rate.key:${KICOX_OPERATION_RATE_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.kicox-operation-rate.csv-resource:classpath:publicdata/kicox-national-industrial-complex-operation-rate-20260331.csv}")
    private String csvResource;

    @Value("${public-data.kicox-operation-rate.dataset-date:2026-03-31}")
    private String datasetDate;

    @Value("${public-data.kicox-operation-rate.next-update-date:2026-06-30}")
    private String nextUpdateDate;

    @Value("${public-data.kicox-operation-rate.swagger-latest-dataset-date:2026-03-31}")
    private String swaggerLatestDatasetDate;

    @Value("${public-data.kicox-operation-rate.portal-total-rows:40}")
    private int portalTotalRows;

    @Value("${public-data.kicox-operation-rate.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.kicox-operation-rate.today-override:}")
    private String todayOverride;

    public KicoxOperationRateConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KICOX_OPERATION_RATE;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + datasetDate;
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
                "validRateRowCount", "rows with nonblank 가동률(퍼센트)_당분기",
                "averageOperationRatePct", "average of 가동률(퍼센트)_당분기 over valid rows",
                "minimumOperationRatePct", "min 가동률(퍼센트)_당분기 over valid rows",
                "maximumOperationRatePct", "max 가동률(퍼센트)_당분기 over valid rows",
                "below70RateComplexCount", "valid rows where current operation rate is below 70%",
                "totalActiveManufacturers", "sum of 제조업가동업체수(개사)",
                "totalQuarterProductionMillionKrw", "sum of 생산(백만원)_당분기생산액"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(rows);
        int recordCount = asInt(metrics.get("recordCount"), 0);
        double averageRate = asDouble(metrics.get("averageOperationRatePct"), 0);
        int contribution = fallbackUsed ? -1 : recordCount == 0 ? 0 : averageRate >= 75 ? 1 : averageRate < 60 ? -1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.76,
                0.38 + Math.min(0.14, recordCount / 300.0)
                        + (asInt(metrics.get("validRateRowCount"), 0) > 0 ? 0.14 : 0));
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
        evidence.put("updateCycle", "quarterly");
        evidence.put("updateDue", updateDue());
        evidence.put("swaggerLatestDatasetDate", swaggerLatestDatasetDate);
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(rows);
            double averageRate = asDouble(metrics.get("averageOperationRatePct"), 0);
            int contribution = rows.isEmpty() ? 0 : averageRate >= 75 ? 1 : averageRate < 60 ? -1 : 0;
            double confidence = Math.min(0.70,
                    0.36 + Math.min(0.12, rows.size() / 300.0)
                            + (asInt(metrics.get("validRateRowCount"), 0) > 0 ? 0.12 : 0));
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
        normalized.put("filteredCount", asInt(metrics.get("validRateRowCount"), 0));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("lastPublishedAt", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", asInt(metrics.get("validRateRowCount"), 0),
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "Industrial complex operation rate adds at most +1/-1 market reference; it does not decide export feasibility.");
        normalized.put("decisionImpact", "국가산업단지 가동률 참고 신호이며 특정 기업 생산능력, 수요 예측, 입주 가능성, 공급 안정성, 최종 Gate 판단을 확정하지 않습니다.");
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
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateCycle", "quarterly");
        evidence.put("updateDue", updateDue());
        evidence.put("apiRefreshEligible", shouldUseApi());
        evidence.put("apiRefreshSkippedReason", updateDue() && (apiKey == null || apiKey.isBlank()) ? "API_KEY_NOT_CONFIGURED" : "");
        evidence.put("swaggerLatestDatasetDate", swaggerLatestDatasetDate);
        evidence.put("portalTotalRows", portalTotalRows);
        evidence.put("localRowCount", rowCount);
        evidence.put("sameAsSwaggerLatestDataset", datasetDate.equals(swaggerLatestDatasetDate));
        evidence.put("sameAsDataGoKrMetadata", rowCount == portalTotalRows && datasetDate.equals(swaggerLatestDatasetDate));
        evidence.put("endpointConfigured", endpoint != null && !endpoint.isBlank());
        evidence.put("operationEndpoint", false);
        evidence.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        evidence.put("apiKeyStored", false);
        evidence.put("fallbackUsed", false);
        evidence.put("reason", "");
        return evidence;
    }

    private Map<String, Object> metrics(List<Map<String, String>> rows) {
        List<Double> rates = rows.stream()
                .map(row -> parseDouble(row.get("가동률(퍼센트)_당분기")))
                .filter(value -> value != null)
                .toList();
        double average = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = rates.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = rates.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        long below70 = rates.stream().filter(value -> value < 70).count();
        Map<String, String> highest = rows.stream()
                .filter(row -> parseDouble(row.get("가동률(퍼센트)_당분기")) != null)
                .max(Comparator.comparingDouble(row -> parseDouble(row.get("가동률(퍼센트)_당분기"))))
                .orElse(Map.of());
        Map<String, String> lowest = rows.stream()
                .filter(row -> parseDouble(row.get("가동률(퍼센트)_당분기")) != null)
                .min(Comparator.comparingDouble(row -> parseDouble(row.get("가동률(퍼센트)_당분기"))))
                .orElse(Map.of());

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("validRateRowCount", rates.size());
        metrics.put("averageOperationRatePct", round3(average));
        metrics.put("minimumOperationRatePct", round3(min));
        metrics.put("maximumOperationRatePct", round3(max));
        metrics.put("below70RateComplexCount", (int) below70);
        metrics.put("totalActiveManufacturers", Math.round(rows.stream()
                .map(row -> parseDouble(row.get("제조업가동업체수(개사)")))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum()));
        metrics.put("totalMaxProductionCapacityMillionKrw", Math.round(rows.stream()
                .map(row -> parseDouble(row.get("생산(백만원)_최대생산능력")))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum()));
        metrics.put("totalQuarterProductionMillionKrw", Math.round(rows.stream()
                .map(row -> parseDouble(row.get("생산(백만원)_당분기생산액")))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum()));
        metrics.put("highestRateComplex", highest.getOrDefault("산업단지", ""));
        metrics.put("lowestRateComplex", lowest.getOrDefault("산업단지", ""));
        return metrics;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        collectObjectRows(readTree(rawPayload), rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("산업단지")) {
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

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
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
