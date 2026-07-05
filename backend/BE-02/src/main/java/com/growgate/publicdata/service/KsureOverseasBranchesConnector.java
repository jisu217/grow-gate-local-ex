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
public class KsureOverseasBranchesConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국무역보험공사_해외지사 현황_20260625";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15044855/v1";
    private static final List<String> HEADERS = List.of("지사명", "영문 지사명", "주소");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.ksure-overseas-branches.url:https://api.odcloud.kr/api/15044855/v1/uddi:99a3f676-337f-439c-8ccd-c70ac08efcd9}")
    private String endpoint;

    @Value("${public-data.ksure-overseas-branches.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.ksure-overseas-branches.csv-resource:classpath:publicdata/ksure-overseas-branches-20260625.csv}")
    private String csvResource;

    @Value("${public-data.ksure-overseas-branches.dataset-date:2026-06-25}")
    private String datasetDate;

    @Value("${public-data.ksure-overseas-branches.next-update-date:2099-12-31}")
    private String nextUpdateDate;

    @Value("${public-data.ksure-overseas-branches.portal-total-rows:23}")
    private int portalTotalRows;

    @Value("${public-data.ksure-overseas-branches.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.ksure-overseas-branches.today-override:}")
    private String todayOverride;

    public KsureOverseasBranchesConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KSURE_OVERSEAS_BRANCHES;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + countryKey(request.getTargetCountry());
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
                "targetCountryBranchMatch", "target country token appears in address",
                "targetCountryBranchCount", "matched branch count for target country",
                "targetCountryBranchNames", "matched 지사명 values",
                "englishBranchNameCount", "rows with 영문 지사명",
                "addressCount", "rows with 주소",
                "topAddressCountry", "most frequent inferred address country",
                "datasetDate", "file data date from public data portal"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        int matched = asInt(metrics.get("targetCountryBranchCount"));
        int contribution = fallbackUsed ? -1 : matched > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.66,
                0.34 + Math.min(0.14, rows.size() / 100.0) + (matched > 0 ? 0.12 : 0.0));
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
        evidence.put("updateCycle", "occasional_one_off");
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            int matched = asInt(metrics.get("targetCountryBranchCount"));
            int contribution = matched > 0 ? 1 : 0;
            double confidence = Math.min(0.60, 0.32 + Math.min(0.12, rows.size() / 100.0) + (matched > 0 ? 0.10 : 0.0));
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
        normalized.put("filteredCount", asInt(metrics.get("targetCountryBranchCount")));
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "K-SURE overseas branch data adds a small deterministic support signal only when target country has a matching branch address.");
        normalized.put("decisionImpact", "해외지사 위치 참고자료입니다. 무역보험 이용 가능성, 지사 담당 가능성, 금융 지원 승인, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.");
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
        diagnostics.put("filteredCount", asInt(metrics.get("targetCountryBranchCount")));
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
        evidence.put("updateCycle", "occasional_one_off");
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
        List<Map<String, String>> matchedRows = rows.stream()
                .filter(row -> targetCountryMatches(request.getTargetCountry(), row))
                .toList();
        List<String> matchedNames = matchedRows.stream()
                .map(row -> safe(row.get("지사명")))
                .filter(value -> !value.isBlank())
                .toList();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("targetCountryBranchMatch", !matchedRows.isEmpty());
        metrics.put("targetCountryBranchCount", matchedRows.size());
        metrics.put("targetCountryBranchNames", matchedNames);
        metrics.put("englishBranchNameCount", rows.stream().filter(row -> !safe(row.get("영문 지사명")).isBlank()).count());
        metrics.put("addressCount", rows.stream().filter(row -> !safe(row.get("주소")).isBlank()).count());
        metrics.put("topAddressCountry", topAddressCountry(rows));
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
        if (node.isObject() && node.has("지사명")) {
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

    private boolean targetCountryMatches(String targetCountry, Map<String, String> row) {
        List<String> tokens = countryTokens(targetCountry);
        if (tokens.isEmpty()) return false;
        String text = normalize(row.get("주소") + " " + row.get("지사명") + " " + row.get("영문 지사명"));
        return tokens.stream().anyMatch(text::contains);
    }

    private String topAddressCountry(List<Map<String, String>> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String country = inferredCountry(row);
            if (!country.isBlank()) counts.put(country, counts.getOrDefault(country, 0L) + 1);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String inferredCountry(Map<String, String> row) {
        String text = normalize(row.get("주소") + " " + row.get("지사명") + " " + row.get("영문 지사명"));
        for (Map.Entry<String, List<String>> entry : countryTokenMap().entrySet()) {
            if (entry.getValue().stream().anyMatch(text::contains)) return entry.getKey();
        }
        return "";
    }

    private List<String> countryTokens(String targetCountry) {
        String key = countryKey(targetCountry);
        if (key.isBlank()) return List.of();
        List<String> mapped = countryTokenMap().get(key);
        if (mapped != null) return mapped;
        return List.of(normalize(targetCountry));
    }

    private String countryKey(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "US", "USA", "UNITED STATES", "미국" -> "US";
            case "CN", "CHN", "CHINA", "중국" -> "CN";
            case "BR", "BRA", "BRAZIL", "브라질" -> "BR";
            case "IN", "IND", "INDIA", "인도" -> "IN";
            case "FR", "FRA", "FRANCE", "프랑스" -> "FR";
            case "JP", "JPN", "JAPAN", "일본" -> "JP";
            case "VN", "VNM", "VIETNAM", "베트남" -> "VN";
            case "DE", "DEU", "GERMANY", "독일" -> "DE";
            case "AE", "ARE", "UAE", "UNITED ARAB EMIRATES", "아랍에미리트" -> "AE";
            case "MX", "MEX", "MEXICO", "멕시코" -> "MX";
            case "PL", "POL", "POLAND", "폴란드" -> "PL";
            case "ID", "IDN", "INDONESIA", "인도네시아" -> "ID";
            case "SG", "SGP", "SINGAPORE", "싱가포르" -> "SG";
            case "SA", "SAU", "SAUDI ARABIA", "사우디아라비아" -> "SA";
            default -> normalized;
        };
    }

    private Map<String, List<String>> countryTokenMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("US", List.of("usa", "unitedstates", "losangeles", "l.a"));
        map.put("CN", List.of("china", "beijing", "shanghai", "chengdu", "북경", "상해", "성도"));
        map.put("BR", List.of("brazil", "saopaulo", "상파울로"));
        map.put("IN", List.of("india", "newdelhi", "인도"));
        map.put("FR", List.of("france", "paris", "파리"));
        map.put("JP", List.of("japan", "tokyo", "일본"));
        map.put("VN", List.of("vietnam", "hanoi", "hochiminh", "베트남"));
        map.put("DE", List.of("germany", "frankfurt", "독일"));
        map.put("AE", List.of("uae", "unitedarabemirates", "dubai", "두바이"));
        map.put("MX", List.of("mexico", "mexicocity", "멕시코"));
        map.put("PL", List.of("poland", "warsaw", "폴란드"));
        map.put("ID", List.of("indonesia", "jakarta", "인도네시아"));
        map.put("SG", List.of("singapore", "싱가포르"));
        map.put("SA", List.of("saudiarabia", "riyadh", "사우디아라비아", "리야드"));
        return map;
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
    }

    private String normalize(String value) {
        return Objects.toString(value, "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace(".", "")
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
