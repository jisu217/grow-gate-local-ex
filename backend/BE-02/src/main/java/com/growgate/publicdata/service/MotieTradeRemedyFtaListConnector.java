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
public class MotieTradeRemedyFtaListConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_무역구제_자유무역협정 목록자료_20250825";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15117950/v1";
    private static final List<String> HEADERS = List.of("자유무역협정아이디", "자유무역협정명", "사용여부");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.motie-trade-remedy-fta-list.url:https://api.odcloud.kr/api/15117950/v1/uddi:dd7abd3f-f7fc-4064-9377-ffd563de8d12}")
    private String endpoint;

    @Value("${public-data.motie-trade-remedy-fta-list.key:${MOTIE_TRADE_REMEDY_FTA_LIST_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.motie-trade-remedy-fta-list.csv-resource:classpath:publicdata/motie-trade-remedy-fta-list-20250825.csv}")
    private String csvResource;

    @Value("${public-data.motie-trade-remedy-fta-list.dataset-date:2025-08-25}")
    private String datasetDate;

    @Value("${public-data.motie-trade-remedy-fta-list.next-update-date:2026-08-25}")
    private String nextUpdateDate;

    @Value("${public-data.motie-trade-remedy-fta-list.swagger-latest-dataset-date:2025-08-25}")
    private String swaggerLatestDatasetDate;

    @Value("${public-data.motie-trade-remedy-fta-list.portal-total-rows:16}")
    private int portalTotalRows;

    @Value("${public-data.motie-trade-remedy-fta-list.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.motie-trade-remedy-fta-list.today-override:}")
    private String todayOverride;

    public MotieTradeRemedyFtaListConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_TRADE_REMEDY_FTA_LIST;
    }

    @Override
    public String usedFor() {
        return "market";
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
                "activeAgreementCount", "사용여부 Y count",
                "targetCountryAgreementMatch", "target country appears in 자유무역협정명",
                "targetCountryAgreementNames", "matching 자유무역협정명 values",
                "datasetDate", "file data date from public data portal"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        boolean matched = Boolean.TRUE.equals(metrics.get("targetCountryAgreementMatch"));
        int contribution = fallbackUsed ? -1 : matched ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.62, 0.34 + Math.min(0.10, rows.size() / 100.0) + (matched ? 0.10 : 0));
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
            boolean matched = Boolean.TRUE.equals(metrics.get("targetCountryAgreementMatch"));
            int contribution = matched ? 1 : 0;
            double confidence = Math.min(0.58, 0.32 + Math.min(0.10, rows.size() / 100.0) + (matched ? 0.08 : 0));
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
        normalized.put("filteredCount", ((List<?>) metrics.getOrDefault("targetCountryAgreementNames", List.of())).size());
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("lastPublishedAt", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "FTA list adds at most +1 market reference when target country/region appears in active agreement names.");
        normalized.put("decisionImpact", "자유무역협정 목록 참고자료입니다. 관세율 확정, 원산지 판정, 협정 적용 가능성, 법률 판단, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        List<String> tokens = countryTokens(request.getTargetCountry());
        List<String> names = rows.stream()
                .filter(row -> "Y".equalsIgnoreCase(safe(row.get("사용여부"))))
                .map(row -> safe(row.get("자유무역협정명")))
                .filter(name -> tokens.stream().anyMatch(token -> normalize(name).contains(token)))
                .toList();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("activeAgreementCount", rows.stream().filter(row -> "Y".equalsIgnoreCase(safe(row.get("사용여부")))).count());
        metrics.put("targetCountryAgreementMatch", !names.isEmpty());
        metrics.put("targetCountryAgreementNames", names);
        metrics.put("datasetDate", datasetDate);
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
        evidence.put("updateCycle", "annual");
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
                "filteredCount", ((List<?>) metrics.getOrDefault("targetCountryAgreementNames", List.of())).size(),
                "zeroReason", rowCount == 0 ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        );
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        collectObjectRows(readTree(rawPayload), rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("자유무역협정명")) {
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

    private List<String> countryTokens(String targetCountry) {
        String key = countryKey(targetCountry);
        return switch (key) {
            case "US" -> List.of("미국", "us", "usa", "unitedstates");
            case "CL" -> List.of("칠레", "chile");
            case "SG" -> List.of("싱가포르", "singapore");
            case "IN" -> List.of("인도", "india");
            case "EU", "DE", "FR", "IT", "ES", "NL", "PL" -> List.of("eu", "유럽연합");
            case "PE" -> List.of("페루", "peru");
            case "TR" -> List.of("터키", "turkey");
            case "AU" -> List.of("호주", "australia");
            case "CA" -> List.of("캐나다", "canada");
            case "CN" -> List.of("중국", "china");
            case "NZ" -> List.of("뉴질랜드", "newzealand");
            case "VN" -> List.of("베트남", "vietnam");
            case "CO" -> List.of("콜롬비아", "colombia");
            case "CR", "SV", "HN", "NI", "PA" -> List.of("중미", "centralamerica");
            default -> List.of(normalize(targetCountry));
        };
    }

    private String countryKey(String value) {
        String normalized = safe(value).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "US", "USA", "UNITED STATES", "미국" -> "US";
            case "CL", "CHL", "CHILE", "칠레" -> "CL";
            case "SG", "SGP", "SINGAPORE", "싱가포르" -> "SG";
            case "IN", "IND", "INDIA", "인도" -> "IN";
            case "EU", "DE", "DEU", "GERMANY", "독일", "FR", "FRA", "FRANCE", "프랑스" -> "EU";
            case "PE", "PER", "PERU", "페루" -> "PE";
            case "TR", "TUR", "TURKEY", "터키" -> "TR";
            case "AU", "AUS", "AUSTRALIA", "호주" -> "AU";
            case "CA", "CAN", "CANADA", "캐나다" -> "CA";
            case "CN", "CHN", "CHINA", "중국" -> "CN";
            case "NZ", "NZL", "NEW ZEALAND", "뉴질랜드" -> "NZ";
            case "VN", "VNM", "VIETNAM", "베트남" -> "VN";
            case "CO", "COL", "COLOMBIA", "콜롬비아" -> "CO";
            default -> normalized;
        };
    }

    private String normalize(String value) {
        return Objects.toString(value, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("-", "").trim();
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
