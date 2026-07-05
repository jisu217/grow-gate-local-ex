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
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class KsureCountryIndustryOverdueRateConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국무역보험공사_77개국 업종별 연체율_20211231";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15104444/v1";
    private static final List<String> HEADERS = List.of("국가", "업종명", "2017", "2018", "2019", "2020", "2021");
    private static final List<String> YEAR_COLUMNS = List.of("2017", "2018", "2019", "2020", "2021");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.ksure-country-industry-overdue-rate.url:https://api.odcloud.kr/api/15104444/v1/uddi:8deb1668-3932-4323-93bf-fad271cfdb0c}")
    private String endpoint;

    @Value("${public-data.ksure-country-industry-overdue-rate.key:${KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.ksure-country-industry-overdue-rate.csv-resource:classpath:publicdata/ksure-country-industry-overdue-rates-20211231.csv}")
    private String csvResource;

    @Value("${public-data.ksure-country-industry-overdue-rate.dataset-date:2021-12-31}")
    private String datasetDate;

    @Value("${public-data.ksure-country-industry-overdue-rate.next-update-date:2022-12-31}")
    private String nextUpdateDate;

    @Value("${public-data.ksure-country-industry-overdue-rate.swagger-latest-dataset-date:2021-12-31}")
    private String swaggerLatestDatasetDate;

    @Value("${public-data.ksure-country-industry-overdue-rate.portal-total-rows:1217}")
    private int portalTotalRows;

    @Value("${public-data.ksure-country-industry-overdue-rate.api-per-page:1500}")
    private int apiPerPage;

    @Value("${public-data.ksure-country-industry-overdue-rate.today-override:}")
    private String todayOverride;

    public KsureCountryIndustryOverdueRateConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE;
    }

    @Override
    public String usedFor() {
        return "market";
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
                "countryCount", "distinct 국가 count",
                "industryCount", "distinct 업종명 count",
                "targetCountryRowCount", "rows matching target country",
                "targetCountryAverageLatestOverduePct", "average 2021 overdue rate for target country",
                "targetCountryHighOverdueRowCount", "target country rows where 2021 overdue rate is >= 20%",
                "globalAverageLatestOverduePct", "average 2021 overdue rate across rows"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        double targetAverage = asDouble(metrics.get("targetCountryAverageLatestOverduePct"));
        int targetRows = asInt(metrics.get("targetCountryRowCount"));
        int contribution = fallbackUsed ? -1 : overdueContribution(targetRows, targetAverage);
        double confidence = fallbackUsed ? 0.0 : Math.min(0.68,
                0.34 + Math.min(0.18, rows.size() / 2000.0) + (targetRows > 0 ? 0.10 : 0));
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    @Override
    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(super.collectionEvidence(endpoint, apiKey, fallbackUsed, reason));
        addEvidenceBase(evidence, "api_after_scheduled_update");
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            double targetAverage = asDouble(metrics.get("targetCountryAverageLatestOverduePct"));
            int targetRows = asInt(metrics.get("targetCountryRowCount"));
            int contribution = overdueContribution(targetRows, targetAverage);
            double confidence = Math.min(0.62, 0.32 + Math.min(0.16, rows.size() / 2000.0) + (targetRows > 0 ? 0.08 : 0));
            String rawPayload = toJson(Map.of("source", "csv", "fileDataName", FILE_DATA_NAME, "data", rows));
            return PublicDataConnectorResult.builder()
                    .provider(provider())
                    .endpoint("csv:" + csvResource)
                    .requestKey(requestKey(request))
                    .hsCode(request.getHsCode())
                    .targetCountry(request.getTargetCountry())
                    .matchedKey(matchedKey(request))
                    .usedFor(usedFor())
                    .rawPayload(rawPayload)
                    .normalizedPayload(normalizedPayload(request, rows, metrics, contribution, confidence, rawPayload))
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
        normalized.put("filteredCount", asInt(metrics.get("targetCountryRowCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("lastPublishedAt", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "K-SURE overdue-rate data adds at most -2 market risk reference based on target-country average latest overdue rate.");
        normalized.put("decisionImpact", "국가·업종별 과거 연체율 참고자료입니다. 바이어 신용도, 개별 거래 리스크, 보험 인수 가능성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        String targetCountry = countryLabel(request.getTargetCountry());
        List<Map<String, String>> targetRows = rows.stream()
                .filter(row -> targetCountry.isBlank() || targetCountry.equals(safe(row.get("국가"))))
                .toList();
        DoubleSummaryStatistics targetLatest = targetRows.stream()
                .map(row -> parsePercent(row.get("2021")))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        DoubleSummaryStatistics globalLatest = rows.stream()
                .map(row -> parsePercent(row.get("2021")))
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("countryCount", rows.stream().map(row -> safe(row.get("국가"))).filter(value -> !value.isBlank()).distinct().count());
        metrics.put("industryCount", rows.stream().map(row -> safe(row.get("업종명"))).filter(value -> !value.isBlank()).distinct().count());
        metrics.put("targetCountryRowCount", targetRows.size());
        metrics.put("targetCountryAverageLatestOverduePct", round1(targetLatest.getCount() == 0 ? 0 : targetLatest.getAverage()));
        metrics.put("targetCountryHighOverdueRowCount", targetRows.stream()
                .map(row -> parsePercent(row.get("2021")))
                .filter(value -> value != null && value >= 20.0)
                .count());
        metrics.put("globalAverageLatestOverduePct", round1(globalLatest.getCount() == 0 ? 0 : globalLatest.getAverage()));
        metrics.put("latestYear", "2021");
        metrics.put("yearColumns", YEAR_COLUMNS);
        return metrics;
    }

    private Map<String, Object> csvEvidence(int rowCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        addEvidenceBase(evidence, "csv_first_until_scheduled_update");
        evidence.put("csvResource", csvResource);
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

    private void addEvidenceBase(Map<String, Object> evidence, String collectionMode) {
        evidence.put("collectionMode", collectionMode);
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("officialUrl", provider().officialUrl());
        evidence.put("swaggerUrl", SWAGGER_URL);
        evidence.put("datasetDate", datasetDate);
        evidence.put("nextUpdateDate", nextUpdateDate);
        evidence.put("updateCycle", "annual_or_one_off");
        evidence.put("updateDue", updateDue());
        evidence.put("apiRefreshEligible", shouldUseApi());
        evidence.put("apiRefreshSkippedReason", updateDue() && (apiKey == null || apiKey.isBlank()) ? "API_KEY_NOT_CONFIGURED" : "");
        evidence.put("swaggerLatestDatasetDate", swaggerLatestDatasetDate);
        evidence.put("portalTotalRows", portalTotalRows);
    }

    private Map<String, Object> zeroDiagnostics(int rowCount, Map<String, Object> metrics) {
        return Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rowCount,
                "normalizedCount", rowCount,
                "filteredCount", asInt(metrics.get("targetCountryRowCount")),
                "zeroReason", rowCount == 0 ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        );
    }

    private int overdueContribution(int targetRows, double targetAverage) {
        if (targetRows == 0) return 0;
        if (targetAverage >= 20.0) return -2;
        if (targetAverage >= 10.0) return -1;
        return 0;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        collectObjectRows(readTree(rawPayload), rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("국가") && node.has("업종명")) {
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

    private String countryLabel(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CN", "CHN", "CHINA" -> "중국";
            case "US", "USA", "UNITED STATES" -> "미국";
            case "JP", "JPN", "JAPAN" -> "일본";
            case "VN", "VNM", "VIETNAM" -> "베트남";
            case "DE", "DEU", "GERMANY" -> "독일";
            case "IN", "IND", "INDIA" -> "인도";
            case "NG", "NGA", "NIGERIA" -> "나이지리아";
            default -> safe(value);
        };
    }

    private Double parsePercent(String value) {
        String clean = safe(value).replace("%", "").replace(",", "").trim();
        if (clean.isBlank()) return null;
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return 0;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    @Override
    protected String safe(String value) {
        return value == null ? "" : value.trim();
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
            for (int j = 0; j < header.size(); j++) row.put(header.get(j), j < values.size() ? values.get(j) : "");
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
