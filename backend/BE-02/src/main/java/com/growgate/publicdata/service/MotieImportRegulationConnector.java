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
import java.util.Set;

@Component
public class MotieImportRegulationConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_수입규제현황_20260508";
    private static final List<String> HEADERS = List.of("관리번호", "규제국", "품목명", "규제유형", "조사개시일", "규제상태", "최근갱신일");
    private static final Set<String> ACTIVE_STATUS = Set.of("규제중", "조사중");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.motie-import-regulation.url:https://api.odcloud.kr/api/3039436/v1/uddi:e460a8d2-9074-45b1-b5eb-c3cd169cbe2a_201703291030}")
    private String endpoint;

    @Value("${public-data.motie-import-regulation.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.motie-import-regulation.csv-resource:classpath:publicdata/motie-import-regulations-20260508.csv}")
    private String csvResource;

    @Value("${public-data.motie-import-regulation.dataset-date:2026-05-08}")
    private String datasetDate;

    @Value("${public-data.motie-import-regulation.next-update-date:2027-05-14}")
    private String nextUpdateDate;

    @Value("${public-data.motie-import-regulation.portal-total-rows:235}")
    private int portalTotalRows;

    @Value("${public-data.motie-import-regulation.api-per-page:1000}")
    private int apiPerPage;

    @Value("${public-data.motie-import-regulation.today-override:}")
    private String todayOverride;

    public MotieImportRegulationConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_IMPORT_REGULATION;
    }

    @Override
    public String usedFor() {
        return "regulation";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + countryLabel(request.getTargetCountry()) + ":" + safe(request.getProductName());
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
                "countryMatchCount", "규제국 equals normalized target country label",
                "productMentionCount", "품목명 contains product keyword token",
                "activeRegulationCount", "규제상태 in 규제중/조사중",
                "investigationCount", "규제상태 equals 조사중",
                "antiDumpingCount", "규제유형 contains 반덤핑"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int active = asInt(metrics.get("activeRegulationCount"), 0);
        int countryMatches = asInt(metrics.get("countryMatchCount"), 0);
        int productMentions = asInt(metrics.get("productMentionCount"), 0);
        int contribution = fallbackUsed ? -1 : riskContribution(active, countryMatches, productMentions);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.88,
                0.45 + Math.min(0.25, rows.size() / 1000.0) + (countryMatches > 0 ? 0.10 : 0) + (productMentions > 0 ? 0.08 : 0));
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
            int active = asInt(metrics.get("activeRegulationCount"), 0);
            int countryMatches = asInt(metrics.get("countryMatchCount"), 0);
            int productMentions = asInt(metrics.get("productMentionCount"), 0);
            int contribution = riskContribution(active, countryMatches, productMentions);
            double confidence = Math.min(0.78,
                    0.40 + Math.min(0.20, rows.size() / 1000.0) + (updateDue() ? 0.0 : 0.10) + (countryMatches > 0 ? 0.08 : 0));
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
        addPeriod(normalized, rows);
        normalized.put("rawCount", rows.size());
        normalized.put("normalizedCount", rows.size());
        normalized.put("filteredCount", asInt(metrics.get("countryMatchCount"), 0));
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
                "filteredCount", asInt(metrics.get("countryMatchCount"), 0),
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "CSV/API regulation pressure is deterministic: active country/product matches reduce regulation readiness score");
        return normalized;
    }

    private Map<String, Object> csvEvidence(int rowCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("collectionMode", "csv_first_until_scheduled_update");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
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
        String targetCountry = countryLabel(request.getTargetCountry());
        List<Map<String, String>> countryRows = rows.stream()
                .filter(row -> targetCountry.isBlank() || targetCountry.equals(row.get("규제국")))
                .toList();
        int productMentions = (int) countryRows.stream()
                .filter(row -> productMatches(row.get("품목명"), request.getProductName()))
                .count();
        int active = (int) countryRows.stream()
                .filter(row -> ACTIVE_STATUS.contains(safe(row.get("규제상태"))))
                .count();
        int investigation = (int) countryRows.stream()
                .filter(row -> "조사중".equals(safe(row.get("규제상태"))))
                .count();
        int antiDumping = (int) countryRows.stream()
                .filter(row -> safe(row.get("규제유형")).contains("반덤핑"))
                .count();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("countryMatchCount", countryRows.size());
        metrics.put("productMentionCount", productMentions);
        metrics.put("activeRegulationCount", active);
        metrics.put("investigationCount", investigation);
        metrics.put("antiDumpingCount", antiDumping);
        return metrics;
    }

    private int riskContribution(int active, int countryMatches, int productMentions) {
        if (countryMatches == 0) return 0;
        return Math.max(-8, -1 - Math.min(4, active) - Math.min(2, productMentions));
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

    private void addPeriod(Map<String, Object> normalized, List<Map<String, String>> rows) {
        List<String> dates = rows.stream()
                .map(row -> safe(row.get("최근갱신일")))
                .filter(value -> !value.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
        if (dates.isEmpty()) return;
        normalized.put("sourcePeriodStart", dates.get(0));
        normalized.put("sourcePeriodEnd", dates.get(dates.size() - 1));
        normalized.put("sourcePeriod", dates.get(0) + " ~ " + dates.get(dates.size() - 1));
        normalized.put("lastPublishedAt", dates.get(dates.size() - 1));
    }

    private boolean productMatches(String itemName, String productName) {
        String item = safe(itemName).toLowerCase(Locale.ROOT);
        String product = safe(productName).toLowerCase(Locale.ROOT);
        if (product.isBlank()) return false;
        for (String token : product.split("[\\s,()/._-]+")) {
            if (token.length() >= 2 && item.contains(token)) return true;
        }
        return item.contains(product);
    }

    private String countryLabel(String value) {
        String normalized = safe(value).trim().toLowerCase(Locale.ROOT).replace(" ", "");
        return switch (normalized) {
            case "us", "usa", "unitedstates", "미국" -> "미국";
            case "cn", "china", "중국" -> "중국";
            case "jp", "japan", "일본" -> "일본";
            case "eu", "europeanunion", "유럽", "유럽연합" -> "유럽연합";
            case "de", "germany", "독일" -> "독일";
            case "in", "india", "인도" -> "인도";
            default -> safe(value).trim();
        };
    }
}
