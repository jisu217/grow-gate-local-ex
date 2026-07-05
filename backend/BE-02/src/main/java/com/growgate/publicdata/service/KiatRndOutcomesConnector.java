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
public class KiatRndOutcomesConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "한국산업기술진흥원_연구개발 성과_20260410";
    private static final String SWAGGER_URL = "https://infuser.odcloud.kr/oas/docs?namespace=15144951/v1";
    private static final List<String> HEADERS = List.of(
            "성과발생년도", "접수번호", "시점구분", "발생여부_논문", "발생여부_특허",
            "등록기탁번호_특허", "국가과학기술지식정보서비스 순번", "등록일자 및 수정일자", "데이터 기준일자"
    );
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kiat-rnd-outcomes.url:https://api.odcloud.kr/api/15144951/v1/uddi:552c6b5b-5d8b-4aa5-8859-d0441cf28de0}")
    private String endpoint;

    @Value("${public-data.kiat-rnd-outcomes.key:${DATAGO_SERVICE_KEY:}}")
    private String apiKey;

    @Value("${public-data.kiat-rnd-outcomes.csv-resource:classpath:publicdata/kiat-rnd-outcomes-20260410.csv}")
    private String csvResource;

    @Value("${public-data.kiat-rnd-outcomes.dataset-date:2026-04-10}")
    private String datasetDate;

    @Value("${public-data.kiat-rnd-outcomes.next-update-date:2027-05-14}")
    private String nextUpdateDate;

    @Value("${public-data.kiat-rnd-outcomes.portal-total-rows:8665}")
    private int portalTotalRows;

    @Value("${public-data.kiat-rnd-outcomes.api-per-page:10000}")
    private int apiPerPage;

    @Value("${public-data.kiat-rnd-outcomes.today-override:}")
    private String todayOverride;

    public KiatRndOutcomesConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KIAT_RND_OUTCOMES;
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
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("recordCount", "CSV/API data[] row count including portal blank rows");
        fields.put("effectiveRecordCount", "rows with 성과발생년도 or 접수번호");
        fields.put("supportKeywordMatch", "productName contains R&D/technology/patent/paper keywords");
        fields.put("paperOutcomeCount", "rows where 발생여부_논문 is 발생");
        fields.put("patentOutcomeCount", "rows where 발생여부_특허 is 발생");
        fields.put("patentDepositNumberCount", "nonblank 등록기탁번호_특허 rows");
        fields.put("earliestOutcomeYear", "min 성과발생년도");
        fields.put("latestOutcomeYear", "max 성과발생년도");
        fields.put("latestRegistrationDate", "max 등록일자 및 수정일자");
        fields.put("latestBaseDate", "max 데이터 기준일자");
        return fields;
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        List<Map<String, String>> rows = rowsFromJson(rawPayload);
        Map<String, Object> metrics = metrics(request, rows);
        boolean matched = Boolean.TRUE.equals(metrics.get("supportKeywordMatch"));
        int contribution = fallbackUsed ? -1 : matched && asInt(metrics.get("effectiveRecordCount")) > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : Math.min(0.68,
                0.34 + Math.min(0.14, rows.size() / 10000.0) + (matched ? 0.12 : 0.0));
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
            int contribution = matched && asInt(metrics.get("effectiveRecordCount")) > 0 ? 1 : 0;
            double confidence = Math.min(0.62, 0.32 + Math.min(0.12, rows.size() / 10000.0) + (matched ? 0.10 : 0.0));
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
        normalized.put("normalizedCount", asInt(metrics.get("effectiveRecordCount")));
        normalized.put("filteredCount", Boolean.TRUE.equals(metrics.get("supportKeywordMatch")) ? asInt(metrics.get("effectiveRecordCount")) : 0);
        normalized.put("zeroReason", rows.isEmpty() ? "EMPTY_CSV_RESOURCE" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodEnd", metrics.get("latestBaseDate"));
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", zeroDiagnostics(rows.size(), metrics));
        normalized.put("scoreFormula", "KIAT R&D outcome statistics add a small deterministic support signal only when product text is R&D, technology, patent, or paper-related.");
        normalized.put("decisionImpact", "연구개발 성과 발생 여부 통계 참고자료입니다. 특정 제품의 기술성, 특허권리성, 논문 성과, 사업화 가능성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> zeroDiagnostics(int rowCount, Map<String, Object> metrics) {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("provider", provider().name());
        diagnostics.put("endpointType", "csv_resource");
        diagnostics.put("usedFor", usedFor());
        diagnostics.put("sourceType", "cache");
        diagnostics.put("rawCount", rowCount);
        diagnostics.put("normalizedCount", asInt(metrics.get("effectiveRecordCount")));
        diagnostics.put("filteredCount", Boolean.TRUE.equals(metrics.get("supportKeywordMatch")) ? asInt(metrics.get("effectiveRecordCount")) : 0);
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
        int effective = 0;
        int paper = 0;
        int patent = 0;
        int patentNumbers = 0;
        String earliestYear = "";
        String latestYear = "";
        String latestRegistrationDate = "";
        String latestBaseDate = "";
        for (Map<String, String> row : rows) {
            String year = safe(row.get("성과발생년도"));
            String receiptNo = safe(row.get("접수번호"));
            if (!year.isBlank() || !receiptNo.isBlank()) effective++;
            if ("발생".equals(safe(row.get("발생여부_논문")))) paper++;
            if ("발생".equals(safe(row.get("발생여부_특허")))) patent++;
            if (!safe(row.get("등록기탁번호_특허")).isBlank()) patentNumbers++;
            if (!year.isBlank()) {
                if (earliestYear.isBlank() || year.compareTo(earliestYear) < 0) earliestYear = year;
                if (latestYear.isBlank() || year.compareTo(latestYear) > 0) latestYear = year;
            }
            String registrationDate = safe(row.get("등록일자 및 수정일자"));
            if (!registrationDate.isBlank() && (latestRegistrationDate.isBlank()
                    || registrationDate.compareTo(latestRegistrationDate) > 0)) latestRegistrationDate = registrationDate;
            String baseDate = safe(row.get("데이터 기준일자"));
            if (!baseDate.isBlank() && (latestBaseDate.isBlank() || baseDate.compareTo(latestBaseDate) > 0)) latestBaseDate = baseDate;
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("effectiveRecordCount", effective);
        metrics.put("supportKeywordMatch", supportKeyword(request.getProductName()));
        metrics.put("paperOutcomeCount", paper);
        metrics.put("patentOutcomeCount", patent);
        metrics.put("patentDepositNumberCount", patentNumbers);
        metrics.put("earliestOutcomeYear", earliestYear);
        metrics.put("latestOutcomeYear", latestYear);
        metrics.put("latestRegistrationDate", latestRegistrationDate);
        metrics.put("latestBaseDate", latestBaseDate);
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
        if (node.isObject() && node.has("성과발생년도")) {
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
        return value.contains("연구") || value.contains("개발") || value.contains("기술")
                || value.contains("특허") || value.contains("논문") || value.contains("성과")
                || value.contains("r&d") || value.contains("rnd") || value.contains("patent")
                || value.contains("paper") || value.contains("technology");
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
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
