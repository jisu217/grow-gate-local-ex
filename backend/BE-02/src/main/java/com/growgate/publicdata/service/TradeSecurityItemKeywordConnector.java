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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class TradeSecurityItemKeywordConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "무역안보관리원_전략물자 품목키워드 및 개정정보_20260522";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15042113/v1";
    private static final List<String> HEADERS = List.of("통제번호", "키워드1", "키워드2", "키워드3");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.trade-security-item-keyword.url:https://api.odcloud.kr/api/15042113/v1/uddi:40a03829-dfb8-4e40-b6c5-e10345ee462d}")
    private String endpoint;

    @Value("${public-data.trade-security-item-keyword.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.trade-security-item-keyword.csv-resource:classpath:publicdata/trade-security-item-keywords-20260522.csv}")
    private String csvResource;

    @Value("${public-data.trade-security-item-keyword.dataset-date:2026-05-22}")
    private String datasetDate;

    @Value("${public-data.trade-security-item-keyword.next-update-date:2027-05-22}")
    private String nextUpdateDate;

    @Value("${public-data.trade-security-item-keyword.portal-total-rows:1006}")
    private int portalTotalRows;

    @Value("${public-data.trade-security-item-keyword.api-per-page:1500}")
    private int apiPerPage;

    @Value("${public-data.trade-security-item-keyword.today-override:}")
    private String todayOverride;

    public TradeSecurityItemKeywordConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.TRADE_SECURITY_ITEM_KEYWORD;
    }

    @Override
    public String usedFor() {
        return "regulation";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + canonicalProduct(request.getProductName()) + ":" + safe(request.getHsCode());
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
                "keywordMatchRowCount", "rows where product name overlaps 키워드1/2/3 token or phrase",
                "matchedControlNumberCount", "distinct non-empty 통제번호 values in matched rows",
                "matchedKeywordTokenCount", "distinct product token or keyword phrase overlaps in matched rows",
                "keywordColumnsScanned", "키워드1/키워드2/키워드3 columns scanned"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int matchedRows = asInt(metrics.get("keywordMatchRowCount"), 0);
        int matchedControls = asInt(metrics.get("matchedControlNumberCount"), 0);
        int contribution = fallbackUsed ? -1 : riskContribution(matchedRows, matchedControls);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.84,
                0.38 + Math.min(0.20, rows.size() / 2000.0)
                        + (matchedRows > 0 ? 0.12 : 0)
                        + (matchedControls > 0 ? 0.06 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            int contribution = riskContribution(asInt(metrics.get("keywordMatchRowCount"), 0),
                    asInt(metrics.get("matchedControlNumberCount"), 0));
            double confidence = Math.min(0.78,
                    0.38 + Math.min(0.20, rows.size() / 2000.0) + (updateDue() ? 0.0 : 0.10)
                            + (asInt(metrics.get("keywordMatchRowCount"), 0) > 0 ? 0.08 : 0));
            String rawPayload = toJson(Map.of("source", "csv", "fileDataName", FILE_DATA_NAME, "data", rows));
            Map<String, Object> normalized = csvNormalizedPayload(request, rows, metrics, contribution, confidence, rawPayload);

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

    private Map<String, Object> csvNormalizedPayload(PublicDataRequest request, List<Map<String, String>> rows,
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
        normalized.put("filteredCount", asInt(metrics.get("keywordMatchRowCount"), 0));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", asInt(metrics.get("keywordMatchRowCount"), 0),
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "Strategic-item keyword contribution is deterministic and only flags review candidates");
        normalized.put("decisionImpact", "키워드 hit는 전략물자 사전 검토 신호이며 전략물자 해당 여부나 허가 필요성을 확정하지 않습니다.");
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

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        Set<String> matchedControlNumbers = new LinkedHashSet<>();
        Set<String> matchedKeywordTokens = new LinkedHashSet<>();
        int matchedRows = 0;

        for (Map<String, String> row : rows) {
            Set<String> rowHits = keywordHits(row, request.getProductName());
            if (rowHits.isEmpty()) continue;
            matchedRows++;
            matchedKeywordTokens.addAll(rowHits);
            String controlNumber = safe(row.get("통제번호")).trim();
            if (!controlNumber.isBlank()) matchedControlNumbers.add(controlNumber);
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("keywordMatchRowCount", matchedRows);
        metrics.put("matchedControlNumberCount", matchedControlNumbers.size());
        metrics.put("matchedKeywordTokenCount", matchedKeywordTokens.size());
        metrics.put("keywordColumnsScanned", 3);
        return metrics;
    }

    private Set<String> keywordHits(Map<String, String> row, String productName) {
        Set<String> hits = new LinkedHashSet<>();
        String product = canonicalProduct(productName);
        if (product.isBlank()) return hits;
        String keywordText = keywordText(row);
        for (String token : productTokens(product)) {
            if (keywordText.contains(token)) hits.add(token);
        }
        for (String phrase : keywordPhrases(row)) {
            if (phrase.length() >= 2 && product.contains(phrase)) hits.add(phrase);
        }
        return hits;
    }

    private String keywordText(Map<String, String> row) {
        return canonicalProduct(safe(row.get("키워드1")) + " " + safe(row.get("키워드2")) + " " + safe(row.get("키워드3")));
    }

    private List<String> keywordPhrases(Map<String, String> row) {
        List<String> phrases = new ArrayList<>();
        for (String header : List.of("키워드1", "키워드2", "키워드3")) {
            for (String phrase : safe(row.get(header)).split("[;,\n]+")) {
                String normalized = canonicalProduct(phrase);
                if (!normalized.isBlank()) phrases.add(normalized);
            }
        }
        return phrases;
    }

    private List<String> productTokens(String product) {
        List<String> tokens = new ArrayList<>();
        for (String token : product.split("[\\s,()/._\\-;]+")) {
            if (token.length() >= 2) tokens.add(token);
        }
        return tokens;
    }

    private int riskContribution(int matchedRows, int matchedControlNumbers) {
        if (matchedRows == 0 || matchedControlNumbers == 0) return 0;
        return Math.max(-7, -1 - Math.min(3, matchedRows) - Math.min(3, matchedControlNumbers));
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

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        JsonNode root = readTree(rawPayload);
        JsonNode data = root.path("data");
        if (!data.isArray()) return List.of();
        List<Map<String, String>> rows = new ArrayList<>();
        for (JsonNode item : data) {
            Map<String, String> row = new LinkedHashMap<>();
            for (String header : HEADERS) {
                row.put(header, item.path(header).asText(""));
            }
            rows.add(row);
        }
        return rows;
    }

    private String canonicalProduct(String value) {
        return safe(value).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
