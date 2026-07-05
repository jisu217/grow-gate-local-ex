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
import java.util.Map;

@Component
public class KicoxFactoryRegistrationConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국산업단지공단_공장등록현황통계_시도별 업종별 현황_20260331";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15085508/v1";
    private static final List<String> REGION_HEADERS = List.of(
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기",
            "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
    );
    private static final List<String> HEADERS = csvHeaders();

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kicox-factory-registration.url:https://api.odcloud.kr/api/15085508/v1/uddi:b0d2581a-2a88-47a6-af2d-87b575db32d2}")
    private String endpoint;

    @Value("${public-data.kicox-factory-registration.key:${KICOX_FACTORY_REGISTRATION_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.kicox-factory-registration.csv-resource:classpath:publicdata/kicox-factory-registration-by-region-industry-20260331.csv}")
    private String csvResource;

    @Value("${public-data.kicox-factory-registration.dataset-date:2026-03-31}")
    private String datasetDate;

    @Value("${public-data.kicox-factory-registration.next-update-date:2026-04-30}")
    private String nextUpdateDate;

    @Value("${public-data.kicox-factory-registration.swagger-latest-dataset-date:2026-03-31}")
    private String swaggerLatestDatasetDate;

    @Value("${public-data.kicox-factory-registration.portal-total-rows:26}")
    private int portalTotalRows;

    @Value("${public-data.kicox-factory-registration.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.kicox-factory-registration.today-override:}")
    private String todayOverride;

    public KicoxFactoryRegistrationConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY;
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
                "regionCount", "시도별 numeric region column count",
                "industryCount", "등록현황 industry row count",
                "totalRegisteredFactories", "sum of all 시도별 공장등록 수",
                "topIndustry", "industry row with highest regional sum",
                "topIndustryFactoryCount", "sum of registered factories for topIndustry",
                "topRegion", "region column with highest registered factory count",
                "topRegionFactoryCount", "registered factory count for topRegion"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(rows);
        int recordCount = asInt(metrics.get("recordCount"), 0);
        long totalFactories = asLong(metrics.get("totalRegisteredFactories"), 0);
        int contribution = fallbackUsed ? -1 : recordCount > 0 && totalFactories > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.72,
                0.36 + Math.min(0.12, recordCount / 100.0)
                        + (totalFactories > 0 ? 0.12 : 0));
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
        evidence.put("updateCycle", "monthly");
        evidence.put("updateDue", updateDue());
        evidence.put("swaggerLatestDatasetDate", swaggerLatestDatasetDate);
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(rows);
            long totalFactories = asLong(metrics.get("totalRegisteredFactories"), 0);
            int contribution = rows.isEmpty() || totalFactories <= 0 ? 0 : 1;
            double confidence = Math.min(0.70,
                    0.36 + Math.min(0.12, rows.size() / 100.0)
                            + (totalFactories > 0 ? 0.12 : 0));
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
        normalized.put("filteredCount", rows.size());
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
                "filteredCount", rows.size(),
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "Factory registration adds at most +1 market reference; it does not decide export feasibility.");
        normalized.put("decisionImpact", "시도별 업종별 공장등록 현황 참고 신호이며 특정 기업 생산능력, 입주 가능성, 지역 공급 안정성, 수요 예측, 최종 Gate 판단을 확정하지 않습니다.");
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
        evidence.put("updateCycle", "monthly");
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
        Map<String, Long> byIndustry = new LinkedHashMap<>();
        Map<String, Long> byRegion = new LinkedHashMap<>();
        REGION_HEADERS.forEach(region -> byRegion.put(region, 0L));

        for (Map<String, String> row : rows) {
            String industry = row.getOrDefault("등록현황", "");
            long industryTotal = 0;
            for (String region : REGION_HEADERS) {
                long value = parseLong(row.get(region));
                industryTotal += value;
                byRegion.put(region, byRegion.getOrDefault(region, 0L) + value);
            }
            byIndustry.put(industry, industryTotal);
        }

        Map.Entry<String, Long> topIndustry = byIndustry.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("", 0L));
        Map.Entry<String, Long> topRegion = byRegion.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(Map.entry("", 0L));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("regionCount", REGION_HEADERS.size());
        metrics.put("industryCount", byIndustry.size());
        metrics.put("totalRegisteredFactories", byRegion.values().stream().mapToLong(Long::longValue).sum());
        metrics.put("topIndustry", topIndustry.getKey());
        metrics.put("topIndustryFactoryCount", topIndustry.getValue());
        metrics.put("topRegion", topRegion.getKey());
        metrics.put("topRegionFactoryCount", topRegion.getValue());
        metrics.put("factoryDenseRegionCount", (int) byRegion.values().stream().filter(value -> value >= 10_000).count());
        metrics.put("regionTotals", byRegion);
        return metrics;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        collectObjectRows(readTree(rawPayload), rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("등록현황")) {
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

    private long parseLong(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.round(Double.parseDouble(value.replace(",", "").trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return fallback;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return fallback;
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

    private static List<String> csvHeaders() {
        List<String> headers = new ArrayList<>(List.of("년월", "등록현황"));
        headers.addAll(REGION_HEADERS);
        return List.copyOf(headers);
    }
}
