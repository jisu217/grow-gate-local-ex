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

@Component
public class KhnpSafetyHealthCertificationConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국수력원자력(주)_안전보건 인증현황 정보_20260623";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15101214/v1";
    private static final List<String> HEADERS = List.of("사업소", "인증규격", "인증기준", "유효기간 시작일", "유효기간 종료일", "비  고");
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.khnp-safety-health-certification.url:https://api.odcloud.kr/api/15101214/v1/uddi:81fdbdb3-dea1-46e7-8286-c64e83ae0743}")
    private String endpoint;

    @Value("${public-data.khnp-safety-health-certification.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.khnp-safety-health-certification.csv-resource:classpath:publicdata/khnp-safety-health-certifications-20260623.csv}")
    private String csvResource;

    @Value("${public-data.khnp-safety-health-certification.dataset-date:2026-06-23}")
    private String datasetDate;

    @Value("${public-data.khnp-safety-health-certification.next-update-date:2027-06-30}")
    private String nextUpdateDate;

    @Value("${public-data.khnp-safety-health-certification.portal-total-rows:16}")
    private int portalTotalRows;

    @Value("${public-data.khnp-safety-health-certification.api-per-page:100}")
    private int apiPerPage;

    @Value("${public-data.khnp-safety-health-certification.today-override:}")
    private String todayOverride;

    public KhnpSafetyHealthCertificationConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KHNP_SAFETY_HEALTH_CERTIFICATION;
    }

    @Override
    public String usedFor() {
        return "support";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + supportKeyword(request.getProductName());
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
                "supportKeywordMatch", "productName contains 원전/원자력/발전/에너지/안전/safety/nuclear",
                "isoCertificationCount", "rows where 인증규격 is ISO",
                "koshaCertificationCount", "rows where 인증규격 is KOSHA",
                "activeCertificationCount", "rows whose 유효기간 종료일 is on/after current date and note does not contain 인증 반납",
                "surrenderedCertificationCount", "rows whose 비  고 contains 인증 반납",
                "earliestExpiryDate", "min 유효기간 종료일",
                "latestExpiryDate", "max 유효기간 종료일"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        boolean matched = Boolean.TRUE.equals(metrics.get("supportKeywordMatch"));
        int contribution = fallbackUsed ? -1 : matched && !rows.isEmpty() ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.68,
                0.34 + Math.min(0.10, rows.size() / 100.0) + (matched ? 0.12 : 0.0));
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
        evidence.put("updateCycle", "annual");
        evidence.put("updateDue", updateDue());
        evidence.put("portalTotalRows", portalTotalRows);
        return evidence;
    }

    private PublicDataConnectorResult collectCsv(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            Map<String, Object> metrics = metrics(request, rows);
            boolean matched = Boolean.TRUE.equals(metrics.get("supportKeywordMatch"));
            int contribution = matched && !rows.isEmpty() ? 1 : 0;
            double confidence = Math.min(0.62, 0.32 + Math.min(0.10, rows.size() / 100.0) + (matched ? 0.10 : 0.0));
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
        normalized.put("filteredCount", Boolean.TRUE.equals(metrics.get("supportKeywordMatch")) ? rows.size() : 0);
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", Boolean.TRUE.equals(metrics.get("supportKeywordMatch")) ? rows.size() : 0,
                "zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "KHNP safety-health certification statistics add a small support signal only when product text is energy/nuclear/safety-related.");
        normalized.put("decisionImpact", "한국수력원자력 사업소 안전보건경영시스템 인증 참고 신호이며 제품 안전인증, 수출 적합성, 거래 가능성, 규제 준수 판정, 최종 Gate 판단을 확정하지 않습니다.");
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

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows) {
        LocalDate today = currentDate();
        int isoCount = 0;
        int koshaCount = 0;
        int activeCount = 0;
        int surrenderedCount = 0;
        String earliestExpiry = "";
        String latestExpiry = "";
        for (Map<String, String> row : rows) {
            String standard = safe(row.get("인증규격")).toUpperCase(Locale.ROOT);
            String note = safe(row.get("비  고"));
            String expiry = safe(row.get("유효기간 종료일"));
            if (standard.contains("ISO")) isoCount++;
            if (standard.contains("KOSHA")) koshaCount++;
            if (note.contains("인증 반납")) surrenderedCount++;
            LocalDate expiryDate = parseDate(expiry);
            if (expiryDate != null && !expiryDate.isBefore(today) && !note.contains("인증 반납")) activeCount++;
            if (!expiry.isBlank()) {
                if (earliestExpiry.isBlank() || expiry.compareTo(earliestExpiry) < 0) earliestExpiry = expiry;
                if (latestExpiry.isBlank() || expiry.compareTo(latestExpiry) > 0) latestExpiry = expiry;
            }
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("supportKeywordMatch", supportKeyword(request.getProductName()));
        metrics.put("isoCertificationCount", isoCount);
        metrics.put("koshaCertificationCount", koshaCount);
        metrics.put("activeCertificationCount", activeCount);
        metrics.put("surrenderedCertificationCount", surrenderedCount);
        metrics.put("earliestExpiryDate", earliestExpiry);
        metrics.put("latestExpiryDate", latestExpiry);
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
        if (node.isObject() && node.has("사업소")) {
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

    private boolean supportKeyword(String productName) {
        String value = safe(productName).toLowerCase(Locale.ROOT);
        return value.contains("원전") || value.contains("원자력") || value.contains("발전")
                || value.contains("에너지") || value.contains("안전")
                || value.contains("nuclear") || value.contains("energy") || value.contains("safety");
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
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
