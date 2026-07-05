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

@Component
public class KgsHydrogenSandboxRegionConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국가스안전공사_수소분야 규제샌드박스 실증지역별 통계_20251130";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15139606/v1";
    private static final List<String> HEADERS = List.of("행정구역", "규제자유특구", "규제샌드박스", "기준일자");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kgs-hydrogen-sandbox-regions.url:https://api.odcloud.kr/api/15139606/v1/uddi:1757878e-98ac-41b6-bcc6-98fda8d01d03}")
    private String endpoint;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.csv-resource:classpath:publicdata/kgs-hydrogen-sandbox-regions-20251130.csv}")
    private String csvResource;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.dataset-date:2025-11-30}")
    private String datasetDate;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.next-update-date:2026-12-01}")
    private String nextUpdateDate;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.portal-total-rows:17}")
    private int portalTotalRows;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.kgs-hydrogen-sandbox-regions.today-override:}")
    private String todayOverride;

    public KgsHydrogenSandboxRegionConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KGS_HYDROGEN_SANDBOX_REGIONS;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + hydrogenKeyword(request.getProductName());
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
                "hydrogenKeywordMatch", "productName contains 수소/hydrogen/H2",
                "totalFreeZoneCount", "sum of 규제자유특구",
                "totalSandboxCount", "sum of 규제샌드박스",
                "leadingRegion", "row with highest 규제샌드박스 count",
                "latestBaseDate", "max 기준일자"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        boolean hydrogen = Boolean.TRUE.equals(metrics.get("hydrogenKeywordMatch"));
        int contribution = fallbackUsed ? -1 : hydrogen && !rows.isEmpty() ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.72, 0.38 + Math.min(0.12, rows.size() / 100.0) + (hydrogen ? 0.12 : 0.0));
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
        evidence.put("updateCycle", "annual/ad-hoc");
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            boolean hydrogen = Boolean.TRUE.equals(metrics.get("hydrogenKeywordMatch"));
            int contribution = hydrogen && !rows.isEmpty() ? 1 : 0;
            double confidence = Math.min(0.66, 0.36 + Math.min(0.12, rows.size() / 100.0) + (hydrogen ? 0.10 : 0.0));
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
        normalized.put("filteredCount", Boolean.TRUE.equals(metrics.get("hydrogenKeywordMatch")) ? rows.size() : 0);
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", metrics.get("latestBaseDate"));
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", Boolean.TRUE.equals(metrics.get("hydrogenKeywordMatch")) ? rows.size() : 0,
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "Hydrogen regulatory sandbox region statistics add a small deterministic support reference only when product text is hydrogen-related.");
        normalized.put("decisionImpact", "수소 분야 실증지역 분포 참고 신호이며 규제 승인, 특구 참여 가능성, 사업 실행, 안전 적합성, 최종 Gate 판단을 확정하지 않습니다.");
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
        evidence.put("updateCycle", "annual/ad-hoc");
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
        int freeZoneTotal = rows.stream().mapToInt(row -> parseInt(row.get("규제자유특구"))).sum();
        int sandboxTotal = rows.stream().mapToInt(row -> parseInt(row.get("규제샌드박스"))).sum();
        Map<String, String> leading = rows.stream()
                .max(Comparator.comparingInt(row -> parseInt(row.get("규제샌드박스"))))
                .orElse(Map.of());
        String latestBaseDate = rows.stream()
                .map(row -> safe(row.get("기준일자")))
                .max(String::compareTo)
                .orElse("");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("hydrogenKeywordMatch", hydrogenKeyword(request.getProductName()));
        metrics.put("totalFreeZoneCount", freeZoneTotal);
        metrics.put("totalSandboxCount", sandboxTotal);
        metrics.put("leadingRegion", leading.getOrDefault("행정구역", ""));
        metrics.put("leadingRegionSandboxCount", parseInt(leading.get("규제샌드박스")));
        metrics.put("latestBaseDate", latestBaseDate);
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
        if (node.isObject() && node.has("행정구역")) {
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

    private boolean hydrogenKeyword(String productName) {
        String value = safe(productName).toLowerCase(Locale.ROOT);
        return value.contains("수소") || value.contains("hydrogen") || value.contains("h2");
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
