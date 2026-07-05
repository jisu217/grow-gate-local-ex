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
public class MotieTradeRemedyOccurrenceConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "산업통상부_무역구제제도 발생현황_20251231";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=3039967/v1";
    private static final List<String> HEADERS = List.of(
            "연도", "반덤핑_국내", "반덤핑_전세계", "상계관세_국내", "상계관세_전세계", "세이프가드_국내", "세이프가드_전세계"
    );
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.motie-trade-remedy-occurrence.url:https://api.odcloud.kr/api/3039967/v1/uddi:aaf4da5b-3469-4e43-9e45-ef834a9a2de0_202002211020}")
    private String endpoint;

    @Value("${public-data.motie-trade-remedy-occurrence.key:${MOTIE_TRADE_REMEDY_OCCURRENCE_API_KEY:${DATAGO_SERVICE_KEY:}}}")
    private String apiKey;

    @Value("${public-data.motie-trade-remedy-occurrence.csv-resource:classpath:publicdata/motie-trade-remedy-occurrences-20251231.csv}")
    private String csvResource;

    @Value("${public-data.motie-trade-remedy-occurrence.dataset-date:2025-12-31}")
    private String datasetDate;

    @Value("${public-data.motie-trade-remedy-occurrence.next-update-date:2026-12-31}")
    private String nextUpdateDate;

    @Value("${public-data.motie-trade-remedy-occurrence.swagger-latest-dataset-date:2025-12-31}")
    private String swaggerLatestDatasetDate;

    @Value("${public-data.motie-trade-remedy-occurrence.portal-total-rows:22}")
    private int portalTotalRows;

    @Value("${public-data.motie-trade-remedy-occurrence.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.motie-trade-remedy-occurrence.today-override:}")
    private String todayOverride;

    public MotieTradeRemedyOccurrenceConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.MOTIE_TRADE_REMEDY_OCCURRENCE;
    }

    @Override
    public String usedFor() {
        return "regulation";
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
                "latestYear", "max 연도",
                "latestDomesticRemedyCount", "latest 반덤핑_국내 + 상계관세_국내 + 세이프가드_국내",
                "latestWorldRemedyCount", "latest 반덤핑_전세계 + 상계관세_전세계 + 세이프가드_전세계",
                "latestAntiDumpingDomestic", "latest 반덤핑_국내",
                "latestAntiDumpingWorld", "latest 반덤핑_전세계",
                "totalDomesticRemedyCount", "sum of domestic remedy counts across all years"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(rows);
        int latestDomestic = asInt(metrics.get("latestDomesticRemedyCount"));
        int contribution = fallbackUsed ? -1 : latestDomestic > 0 ? -1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.64, 0.34 + Math.min(0.12, rows.size() / 100.0) + (latestDomestic > 0 ? 0.10 : 0));
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
            Map<String, Object> metrics = metrics(rows);
            int latestDomestic = asInt(metrics.get("latestDomesticRemedyCount"));
            int contribution = latestDomestic > 0 ? -1 : 0;
            double confidence = Math.min(0.60, 0.32 + Math.min(0.12, rows.size() / 100.0) + (latestDomestic > 0 ? 0.08 : 0));
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
        normalized.put("filteredCount", rows.size());
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("lastPublishedAt", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size()));
        normalized.put("scoreFormula", "Trade remedy occurrence aggregate adds at most -1 regulation reference when latest domestic remedy count is nonzero.");
        normalized.put("decisionImpact", "연도별 무역구제 발생 aggregate 참고자료입니다. 특정 품목 제소 가능성, 법률 판단, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
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

    private Map<String, Object> zeroDiagnostics(int rowCount) {
        return Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rowCount,
                "normalizedCount", rowCount,
                "filteredCount", rowCount,
                "zeroReason", rowCount == 0 ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        );
    }

    private Map<String, Object> metrics(List<Map<String, String>> rows) {
        Map<String, String> latest = rows.stream()
                .max(Comparator.comparingInt(row -> asInt(row.get("연도"))))
                .orElse(Map.of());
        int latestDomestic = asInt(latest.get("반덤핑_국내")) + asInt(latest.get("상계관세_국내")) + asInt(latest.get("세이프가드_국내"));
        int latestWorld = asInt(latest.get("반덤핑_전세계")) + asInt(latest.get("상계관세_전세계")) + asInt(latest.get("세이프가드_전세계"));
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("latestYear", latest.getOrDefault("연도", ""));
        metrics.put("latestDomesticRemedyCount", latestDomestic);
        metrics.put("latestWorldRemedyCount", latestWorld);
        metrics.put("latestAntiDumpingDomestic", asInt(latest.get("반덤핑_국내")));
        metrics.put("latestAntiDumpingWorld", asInt(latest.get("반덤핑_전세계")));
        metrics.put("latestCountervailingDomestic", asInt(latest.get("상계관세_국내")));
        metrics.put("latestSafeguardDomestic", asInt(latest.get("세이프가드_국내")));
        metrics.put("totalDomesticRemedyCount", rows.stream().mapToInt(row ->
                asInt(row.get("반덤핑_국내")) + asInt(row.get("상계관세_국내")) + asInt(row.get("세이프가드_국내"))).sum());
        return metrics;
    }

    private List<Map<String, String>> rowsFromJson(String rawPayload) {
        List<Map<String, String>> rows = new ArrayList<>();
        collectObjectRows(readTree(rawPayload), rows);
        return rows;
    }

    private void collectObjectRows(JsonNode node, List<Map<String, String>> rows) {
        if (node == null || node.isNull() || node.isMissingNode()) return;
        if (node.isObject() && node.has("연도")) {
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

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null || value.toString().isBlank()) return 0;
        try {
            return (int) Math.round(Double.parseDouble(value.toString().replace(",", "").trim()));
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
