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

@Component
public class KatsDomesticRecallReportConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_국가기술표준원_제품안전_국내리콜 보고서_20220801";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15040695/v1";
    private static final List<String> HEADERS = List.of("안전기준위반내용", "표시사항부적합내용", "사고사례내용");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kats-domestic-recall-report.url:https://api.odcloud.kr/api/15040695/v1/uddi:d4469be3-c128-4fbc-8ef2-b6e15689625f}")
    private String endpoint;

    @Value("${public-data.kats-domestic-recall-report.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kats-domestic-recall-report.csv-resource:classpath:publicdata/kats-domestic-recall-report-20220801.csv}")
    private String csvResource;

    @Value("${public-data.kats-domestic-recall-report.dataset-date:2022-08-01}")
    private String datasetDate;

    @Value("${public-data.kats-domestic-recall-report.next-update-date:2022-08-02}")
    private String nextUpdateDate;

    @Value("${public-data.kats-domestic-recall-report.portal-total-rows:848}")
    private int portalTotalRows;

    @Value("${public-data.kats-domestic-recall-report.api-per-page:1000}")
    private int apiPerPage;

    @Value("${public-data.kats-domestic-recall-report.today-override:}")
    private String todayOverride;

    public KatsDomesticRecallReportConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KATS_DOMESTIC_RECALL_REPORT;
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
                "productKeywordMentionCount", "rows where productName token appears in recall text",
                "safetyViolationRows", "rows with 안전기준위반내용",
                "labelNonComplianceRows", "rows with 표시사항부적합내용",
                "accidentCaseRows", "rows with 사고사례내용",
                "chemicalRiskRows", "rows mentioning lead/cadmium/phthalate/boron chemical risks",
                "electricalRiskRows", "rows mentioning electric strength/temperature/insulation risks",
                "childrenRiskRows", "rows mentioning child/infant risk terms",
                "topAccidentCase", "most frequent 사고사례내용"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int mentions = asInt(metrics.get("productKeywordMentionCount"));
        int contribution = fallbackUsed ? -1 : mentions > 0 ? -2 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.68,
                0.36 + Math.min(0.14, rows.size() / 1000.0) + (mentions > 0 ? 0.12 : 0.0));
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
        evidence.put("updateCycle", "file_snapshot_api_for_recent_data");
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            int mentions = asInt(metrics.get("productKeywordMentionCount"));
            int contribution = mentions > 0 ? -2 : 0;
            double confidence = Math.min(0.62, 0.34 + Math.min(0.12, rows.size() / 1000.0) + (mentions > 0 ? 0.10 : 0.0));
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
        normalized.put("filteredCount", asInt(metrics.get("productKeywordMentionCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "Domestic recall report text adds a small deterministic risk signal only when product text directly appears in recall reason text.");
        normalized.put("decisionImpact", "국내리콜 보고서 원문 텍스트 참고자료입니다. 제품별 리콜 존재/부재, 안전인증 적합성, 제품 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.");
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
        evidence.put("updateCycle", "file_snapshot_api_for_recent_data");
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
        metrics.put("safetyViolationRows", rows.stream().filter(row -> !safe(row.get("안전기준위반내용")).isBlank()).count());
        metrics.put("labelNonComplianceRows", rows.stream().filter(row -> !safe(row.get("표시사항부적합내용")).isBlank()).count());
        metrics.put("accidentCaseRows", rows.stream().filter(row -> !safe(row.get("사고사례내용")).isBlank()).count());
        metrics.put("chemicalRiskRows", rows.stream().filter(row -> containsAny(rowText(row), List.of("납", "카드뮴", "프탈레이트", "붕소", "가소제"))).count());
        metrics.put("electricalRiskRows", rows.stream().filter(row -> containsAny(rowText(row), List.of("전기", "절연", "온도상승", "감전", "발화"))).count());
        metrics.put("childrenRiskRows", rows.stream().filter(row -> containsAny(rowText(row), List.of("어린이", "유아", "아동", "완구"))).count());
        metrics.put("topAccidentCase", topValue(rows, "사고사례내용"));
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
        if (node.isObject() && node.has("안전기준위반내용")) {
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
            if (value.length() >= 3 && !tokens.contains(value)) tokens.add(value);
        }
        return tokens;
    }

    private String rowText(Map<String, String> row) {
        return normalize(row.get("안전기준위반내용") + " " + row.get("표시사항부적합내용") + " " + row.get("사고사례내용"));
    }

    private boolean containsAny(String text, List<String> tokens) {
        return tokens.stream().map(this::normalize).anyMatch(text::contains);
    }

    private String topValue(List<Map<String, String>> rows, String key) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String value = safe(row.get(key));
            if (!value.isBlank()) counts.put(value, counts.getOrDefault(value, 0L) + 1);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
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
