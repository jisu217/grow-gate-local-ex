package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class SafetyRecallConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_국가기술표준원_제품안전_국내리콜정보";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15040696/v1";
    private static final List<String> NORMALIZED_HEADERS = List.of(
            "recallId", "productName", "category", "modelName", "manufacturer",
            "recallReason", "recallDate", "hazardType", "country", "sourceUrl",
            "rawProvider", "collectedAt"
    );

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.safety-recall.url:}")
    private String endpoint;

    @Value("${public-data.safety-recall.key:}")
    private String apiKey;

    @Value("${public-data.safety-recall.csv-resource:classpath:publicdata/kats-domestic-recall-20230809.csv}")
    private String csvResource;

    @Value("${public-data.safety-recall.cache-root:data/cache/safety_recall}")
    private String cacheRoot;

    @Value("${public-data.safety-recall.dataset-date:2023-08-09}")
    private String datasetDate;

    @Value("${public-data.safety-recall.next-update-date:2025-09-05}")
    private String nextUpdateDate;

    @Value("${public-data.safety-recall.portal-total-rows:881}")
    private int portalTotalRows;

    @Value("${public-data.safety-recall.api-per-page:1000}")
    private int apiPerPage;

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
        return provider() + ":" + normalize(request.getProductName());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        boolean endpointReady = endpoint != null && !endpoint.isBlank();
        boolean keyReady = apiKey != null && !apiKey.isBlank();
        String apiAttemptReason = "";

        if (endpointReady && keyReady) {
            PublicDataConnectorResult live = getJson(request, endpoint, apiKey, Map.of(
                    "page", "1",
                    "perPage", String.valueOf(Math.max(1, apiPerPage)),
                    "returnType", "JSON"
            ));
            if (!live.isFallbackUsed() && "live".equals(live.getSourceType())) {
                writeApiDerivedCache(live);
                return live;
            }
            apiAttemptReason = live.getErrorMessage() == null || live.getErrorMessage().isBlank()
                    ? "API_FAILED"
                    : live.getErrorMessage();
        } else if (!endpointReady) {
            apiAttemptReason = "OPERATION_ENDPOINT_NOT_CONFIGURED";
        } else {
            apiAttemptReason = "API_KEY_NOT_CONFIGURED";
        }

        PublicDataConnectorResult derivedCache = collectLatestApiDerivedCache(request, apiAttemptReason);
        if (derivedCache != null) return derivedCache;
        return collectCsvSnapshot(request, apiAttemptReason);
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "KATS domestic recall row count",
                "productKeywordMentionCount", "rows where productName token appears in product/model/category/manufacturer/hazard text",
                "hazardMentionCount", "rows with 위해유형/recall reason populated",
                "electricalRiskRows", "rows mentioning battery/electric/fire/overheat terms",
                "childrenRiskRows", "rows in 어린이 product categories",
                "recallDateRange", "first and last 등록일/recallDate in source"
        );
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        evidence.put("collectionMode", "kats_domestic_recall_api_with_csv_cache_aside");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("datasetDate", datasetDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("portalTotalRows", portalTotalRows);
        evidence.put("cacheRoot", cacheRoot);
        evidence.put("cacheLineage", fallbackUsed ? "" : "live-api");
        return evidence;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int mentions = asInt(metrics.get("productKeywordMentionCount"));
        int electricalRiskRows = asInt(metrics.get("electricalRiskRows"));
        int contribution = fallbackUsed ? -2 : mentions > 0 ? -Math.min(6, 2 + electricalRiskRows) : 0;
        double confidence = fallbackUsed ? 0.0 : confidence(rows.size(), mentions);
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private PublicDataConnectorResult collectLatestApiDerivedCache(PublicDataRequest request, String apiAttemptReason) {
        try {
            Path latest = latestApiDerivedCacheDir();
            if (latest == null) return null;
            Path raw = latest.resolve("raw_response.json");
            Path normalized = latest.resolve("safety_recall_normalized.csv");
            Path metadata = latest.resolve("metadata.json");
            if (!Files.exists(raw) || !Files.exists(normalized) || !Files.exists(metadata)) return null;

            String rawPayload = Files.readString(raw, StandardCharsets.UTF_8);
            List<Map<String, String>> rows = readNormalizedCsv(normalized);
            Map<String, Object> metrics = metrics(request, rows);
            int mentions = asInt(metrics.get("productKeywordMentionCount"));
            int contribution = mentions > 0 ? -2 : 0;
            double confidence = Math.min(0.78, confidence(rows.size(), mentions) + 0.08);
            String reason = reasonWithCache(apiAttemptReason, "API-derived cache available from " + latest.getFileName());
            Map<String, Object> payload = normalizedPayload(request, rows, metrics, contribution, confidence,
                    rawPayload, "cache", "api-derived", reason);
            return result(request, rawPayload, payload, contribution, confidence, "cache", false, null,
                    "cache:" + normalized);
        } catch (Exception e) {
            return null;
        }
    }

    private PublicDataConnectorResult collectCsvSnapshot(PublicDataRequest request, String apiAttemptReason) {
        try {
            List<Map<String, String>> rows = readCsvRows(csvResource);
            List<Map<String, String>> normalizedRows = rows.stream().map(this::normalizeRow).toList();
            Map<String, Object> metrics = metrics(request, normalizedRows);
            int mentions = asInt(metrics.get("productKeywordMentionCount"));
            int contribution = mentions > 0 ? -2 : 0;
            double confidence = Math.min(0.66, confidence(normalizedRows.size(), mentions));
            String rawPayload = toJson(Map.of(
                    "source", "csv",
                    "fileDataName", FILE_DATA_NAME,
                    "data", normalizedRows
            ));
            String reason = reasonWithCache(apiAttemptReason, "CSV cache available; cacheLineage=file-snapshot");
            Map<String, Object> payload = normalizedPayload(request, normalizedRows, metrics, contribution, confidence,
                    rawPayload, "cache", "file-snapshot", reason);
            return result(request, rawPayload, payload, contribution, confidence, "cache", false, null,
                    "csv:" + csvResource);
        } catch (IOException e) {
            return fallback(request, endpoint, apiKey, "CSV_CACHE_UNAVAILABLE: " + e.getMessage());
        }
    }

    private PublicDataConnectorResult result(PublicDataRequest request, String rawPayload, Map<String, Object> payload,
                                             int contribution, double confidence, String sourceType,
                                             boolean fallbackUsed, String errorMessage, String resultEndpoint) {
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(resultEndpoint)
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(rawPayload)
                .normalizedPayload(payload)
                .scoreContribution(contribution)
                .confidence(confidence)
                .sourceType(sourceType)
                .fallbackUsed(fallbackUsed)
                .errorMessage(errorMessage)
                .build();
    }

    private Map<String, Object> normalizedPayload(PublicDataRequest request, List<Map<String, String>> rows,
                                                  Map<String, Object> metrics, int contribution,
                                                  double confidence, String rawPayload, String sourceType,
                                                  String cacheLineage, String reason) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", false);
        normalized.put("reason", reason);
        normalized.put("rawLength", rawPayload.length());
        normalized.put("signal", sourceType);
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
        normalized.put("filteredCount", asInt(metrics.get("productKeywordMentionCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", NORMALIZED_HEADERS);
        normalized.put("httpStatus", "api-derived".equals(cacheLineage) ? 200 : null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodStart", metrics.get("firstRecallDate"));
        normalized.put("sourcePeriodEnd", metrics.get("lastRecallDate"));
        normalized.put("lastPublishedAt", metrics.get("lastRecallDate"));
        normalized.put("collectionEvidence", cacheEvidence(rows.size(), cacheLineage, reason));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics, cacheLineage));
        normalized.put("scoreFormula", "KATS domestic recall information adds a small deterministic recall-risk signal only when product/model/category text directly overlaps.");
        normalized.put("decisionImpact", "국내리콜정보 참고자료입니다. 제품별 최신 리콜 없음, 안전인증 적합성, 제품 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> cacheEvidence(int rowCount, String cacheLineage, String reason) {
        boolean apiDerived = "api-derived".equals(cacheLineage);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("endpointConfigured", endpoint != null && !endpoint.isBlank());
        evidence.put("operationEndpoint", apiDerived);
        evidence.put("endpointType", apiDerived ? "operation" : "csv_resource");
        evidence.put("apiKeyConfigured", apiDerived);
        evidence.put("apiKeyConfiguredAtCollection", apiDerived);
        evidence.put("apiKeyStored", false);
        evidence.put("fallbackUsed", false);
        evidence.put("reason", reason == null ? "" : reason);
        evidence.put("collectionMode", apiDerived ? "cache_from_live_api" : "csv_snapshot_cache_aside");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("csvResource", csvResource);
        evidence.put("cacheRoot", cacheRoot);
        evidence.put("cacheLineage", cacheLineage);
        evidence.put("cacheDerivedFromApi", apiDerived);
        evidence.put("csvCacheAvailable", rowCount > 0);
        evidence.put("datasetDate", datasetDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("portalTotalRows", portalTotalRows);
        evidence.put("localRowCount", rowCount);
        return evidence;
    }

    private Map<String, Object> zeroDiagnostics(int rowCount, Map<String, Object> metrics, String cacheLineage) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("provider", provider().name());
        diagnostics.put("endpointType", "api-derived".equals(cacheLineage) ? "operation-cache" : "csv_resource");
        diagnostics.put("usedFor", usedFor());
        diagnostics.put("sourceType", "cache");
        diagnostics.put("rawCount", rowCount);
        diagnostics.put("normalizedCount", rowCount);
        diagnostics.put("filteredCount", asInt(metrics.get("productKeywordMentionCount")));
        diagnostics.put("zeroReason", rowCount == 0 ? "EMPTY_CSV_RESOURCE" : "");
        diagnostics.put("sampleRawKeys", NORMALIZED_HEADERS);
        diagnostics.put("fallbackUsed", false);
        diagnostics.put("cacheLineage", cacheLineage);
        return diagnostics;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        int mentions = productKeywordMentionCount(request.getProductName(), rows);
        List<String> dates = rows.stream().map(row -> safe(row.get("recallDate")))
                .filter(value -> !value.isBlank()).sorted().toList();
        metrics.put("recordCount", rows.size());
        metrics.put("productKeywordMentionCount", mentions);
        metrics.put("hazardMentionCount", rows.stream().filter(row -> !safe(row.get("hazardType")).isBlank()).count());
        metrics.put("electricalRiskRows", rows.stream().filter(row -> containsAny(rowText(row),
                List.of("전기", "배터리", "충전", "감전", "발화", "화재", "과열", "온도"))).count());
        metrics.put("childrenRiskRows", rows.stream().filter(row -> containsAny(rowText(row),
                List.of("어린이", "유아", "아동", "완구"))).count());
        metrics.put("recallCount", rows.size());
        metrics.put("highRiskCount", rows.stream().filter(row -> containsAny(rowText(row),
                List.of("전기", "배터리", "충전", "감전", "발화", "화재", "과열", "온도", "유해물질"))).count());
        metrics.put("firstRecallDate", dates.isEmpty() ? datasetDate : dates.get(0));
        metrics.put("lastRecallDate", dates.isEmpty() ? datasetDate : dates.get(dates.size() - 1));
        metrics.put("datasetDate", datasetDate);
        metrics.put("schemaVersion", "SAFETY_RECALL_DOMESTIC_v1");
        return metrics;
    }

    private void writeApiDerivedCache(PublicDataConnectorResult live) {
        try {
            List<Map<String, String>> rows = rowsFromJson(live.getRawPayload());
            Path dir = Path.of(cacheRoot, LocalDate.now().toString());
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("raw_response.json"), live.getRawPayload(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("safety_recall_normalized.csv"), toCsv(rows), StandardCharsets.UTF_8);
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("collectedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metadata.put("provider", provider().name());
            metadata.put("sourceApiUrl", endpoint);
            metadata.put("operationEndpoint", true);
            metadata.put("apiKeyConfiguredAtCollection", true);
            metadata.put("rawResponseSha256", "sha256:" + sha256(live.getRawPayload()));
            metadata.put("recordCount", rows.size());
            metadata.put("schemaVersion", "SAFETY_RECALL_DOMESTIC_v1");
            metadata.put("cacheLineage", "api-derived");
            Files.writeString(dir.resolve("metadata.json"), toJson(metadata), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("SAFETY_RECALL API-derived cache write failed: {}", e.getMessage());
        }
    }

    private Path latestApiDerivedCacheDir() throws IOException {
        Path root = Path.of(cacheRoot);
        if (!Files.isDirectory(root)) return null;
        try (var stream = Files.list(root)) {
            return stream.filter(Files::isDirectory)
                    .filter(path -> Files.exists(path.resolve("metadata.json")))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElse(null);
        }
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        JsonNode root = readTree(rawPayload);
        collectRows(root, rows);
        return rows;
    }

    private void collectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && looksLikeRecallRow(node)) {
            Map<String, String> row = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry -> row.put(entry.getKey(), entry.getValue().asText("")));
            rows.add(normalizeRow(row));
            return;
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) collectRows(child, rows);
        }
    }

    private boolean looksLikeRecallRow(JsonNode node) {
        return node.has("인증번호") || node.has("제품명") || node.has("모델명")
                || node.has("recallId") || node.has("productName") || node.has("modelName");
    }

    private Map<String, String> normalizeRow(Map<String, String> row) {
        Map<String, String> normalized = new LinkedHashMap<>();
        String collectedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        normalized.put("recallId", first(row, "recallId", "인증번호", "인증 번호", "certNo"));
        normalized.put("productName", first(row, "productName", "제품명", "품목명", "itemNm"));
        normalized.put("category", first(row, "category", "법정제품분류", "제품분류", "분류"));
        normalized.put("modelName", first(row, "modelName", "모델명", "모델"));
        normalized.put("manufacturer", first(row, "manufacturer", "제조사명", "리콜사업자명", "사업자명"));
        normalized.put("recallReason", first(row, "recallReason", "리콜종류", "리콜사유", "리콜방법"));
        normalized.put("recallDate", first(row, "recallDate", "등록일", "리콜일자", "공표일자"));
        normalized.put("hazardType", first(row, "hazardType", "위해유형", "위해내용", "결함내용"));
        normalized.put("country", first(row, "country", "제조국가명", "제조사국가명"));
        normalized.put("sourceUrl", provider().officialUrl());
        normalized.put("rawProvider", FILE_DATA_NAME);
        normalized.put("collectedAt", collectedAt);
        return normalized;
    }

    private String first(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = safe(row.get(key));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private int productKeywordMentionCount(String productName, List<Map<String, String>> rows) {
        List<String> tokens = productTokens(productName);
        if (tokens.isEmpty()) return 0;
        return (int) rows.stream()
                .map(this::rowText)
                .filter(text -> tokens.stream().anyMatch(text::contains))
                .count();
    }

    private List<String> productTokens(String productName) {
        String normalized = normalize(productName);
        if (normalized.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        tokens.add(normalized);
        for (String token : safe(productName).split("[\\s,/()\\-]+")) {
            String value = normalize(token);
            if (value.length() >= 2 && !tokens.contains(value)) tokens.add(value);
        }
        return tokens;
    }

    private String rowText(Map<String, String> row) {
        return normalize(row.get("productName") + " " + row.get("category") + " "
                + row.get("modelName") + " " + row.get("manufacturer") + " "
                + row.get("recallReason") + " " + row.get("hazardType"));
    }

    private double confidence(int rowCount, int mentions) {
        return Math.min(0.72, 0.38 + Math.min(0.18, rowCount / 1200.0) + (mentions > 0 ? 0.12 : 0.0));
    }

    private String reasonWithCache(String apiAttemptReason, String cacheReason) {
        String reason = safe(apiAttemptReason);
        if (reason.isBlank()) return cacheReason;
        return reason + "; " + cacheReason;
    }

    private List<Map<String, String>> readCsvRows(String resourcePath) throws IOException {
        Resource resource = resourceLoader.getResource(resourcePath);
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

    private List<Map<String, String>> readNormalizedCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank()).toList();
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

    private String toCsv(List<Map<String, String>> rows) {
        StringBuilder csv = new StringBuilder(String.join(",", NORMALIZED_HEADERS)).append("\n");
        for (Map<String, String> row : rows) {
            for (int i = 0; i < NORMALIZED_HEADERS.size(); i++) {
                if (i > 0) csv.append(",");
                csv.append(escapeCsv(row.get(NORMALIZED_HEADERS.get(i))));
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        String safe = safe(value);
        if (!safe.contains(",") && !safe.contains("\"") && !safe.contains("\n")) return safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
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

    private boolean containsAny(String text, List<String> tokens) {
        return tokens.stream().map(this::normalize).anyMatch(text::contains);
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
    }

    private String normalize(String value) {
        return Objects.toString(value, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }

    private String sha256(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
