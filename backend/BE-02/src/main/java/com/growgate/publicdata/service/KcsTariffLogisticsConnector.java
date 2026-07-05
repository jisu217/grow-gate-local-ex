package com.growgate.publicdata.service;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

@Component
public class KcsTariffLogisticsConnector extends AbstractPublicDataConnector {

    private static final String FILE_DATA_NAME = "관세청_HS부호_20260101_품목번호별관세율표_20260211_운송비용_20260531";
    private static final List<String> HEADERS = List.of(
            "scenario", "prefix", "hs_code", "hs_name_ko", "hs_name_en",
            "tariff_type", "tariff_rate_pct", "mode", "route", "latest_period",
            "latest_cost_index", "avg_12m_cost_index", "yoy_pct"
    );
    private static final Map<String, List<String>> COUNTRY_ROUTES = Map.ofEntries(
            Map.entry("DE", List.of("유럽연합")),
            Map.entry("GERMANY", List.of("유럽연합")),
            Map.entry("EU", List.of("유럽연합")),
            Map.entry("US", List.of("미국", "미국서부", "미국동부")),
            Map.entry("UNITED STATES", List.of("미국", "미국서부", "미국동부")),
            Map.entry("JP", List.of("일본")),
            Map.entry("JAPAN", List.of("일본")),
            Map.entry("CN", List.of("중국")),
            Map.entry("CHINA", List.of("중국")),
            Map.entry("VN", List.of("베트남")),
            Map.entry("VIETNAM", List.of("베트남"))
    );

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Value("${public-data.kcs-tariff-logistics.csv-resource:classpath:publicdata/kcs-tariff-logistics-reference-20260531.csv}")
    private String csvResource;

    @Value("${public-data.kcs-tariff-logistics.dataset-date:2026-05-31}")
    private String datasetDate;

    @Value("${public-data.kcs-tariff-logistics.source-hs-date:2026-01-01}")
    private String sourceHsDate;

    @Value("${public-data.kcs-tariff-logistics.source-tariff-date:2026-02-11}")
    private String sourceTariffDate;

    public KcsTariffLogisticsConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.KCS_TARIFF_LOGISTICS;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":" + normalizeHsCode(request.getHsCode()) + ":" + routeCountryKey(request.getTargetCountry());
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        try {
            List<Map<String, String>> rows = readCsvRows();
            List<Map<String, String>> matchedRows = matchedRows(request, rows);
            Map<String, Object> metrics = metrics(request, rows, matchedRows);
            int contribution = contribution(metrics);
            double confidence = confidence(metrics, rows.size());
            String rawPayload = toJson(Map.of(
                    "source", "csv",
                    "fileDataName", FILE_DATA_NAME,
                    "matchedData", matchedRows
            ));
            Map<String, Object> normalized = normalizedPayload(request, rows, matchedRows, metrics, contribution, confidence, rawPayload);

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

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "normalized CSV row count",
                "hsCodeMatchCount", "hs_code starts with requested HS prefix",
                "routeMatchCount", "route mapped from requested target country",
                "mfnTariffRatePct", "tariff_type A rate for matched HS rows",
                "preferentialTariffRatePct", "lowest non-A positive tariff rate for matched HS rows",
                "latestFreightIndex", "latest route freight index from 20260531 KCS freight files",
                "freightYoyPct", "latest route freight YoY percentage"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        return signal(request, Map.of("recordCount", fallbackUsed ? 0 : estimateRecordCount(rawPayload)),
                fallbackUsed ? -1 : 1, fallbackUsed ? 0.0 : 0.6);
    }

    @Override
    protected String endpointLabel() {
        return "csv:" + csvResource;
    }

    private Map<String, Object> normalizedPayload(PublicDataRequest request, List<Map<String, String>> rows,
                                                  List<Map<String, String>> matchedRows, Map<String, Object> metrics,
                                                  int contribution, double confidence, String rawPayload) {
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
        normalized.put("signal", "file");
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
        normalized.put("filteredCount", matchedRows.size());
        normalized.put("zeroReason", matchedRows.isEmpty() ? "NO_HS_OR_ROUTE_MATCH" : "");
        normalized.put("sampleRawKeys", HEADERS);
        normalized.put("httpStatus", null);
        normalized.put("elapsedMs", 0);
        normalized.put("sourcePeriodStart", sourceHsDate);
        normalized.put("sourcePeriodEnd", datasetDate);
        normalized.put("sourcePeriod", sourceHsDate + " ~ " + datasetDate);
        normalized.put("lastPublishedAt", datasetDate);
        normalized.put("collectionEvidence", csvEvidence(rows.size()));
        normalized.put("zeroResultDiagnostics", Map.of(
                "provider", provider().name(),
                "endpointType", "csv_resource",
                "usedFor", usedFor(),
                "sourceType", "cache",
                "rawCount", rows.size(),
                "normalizedCount", rows.size(),
                "filteredCount", matchedRows.size(),
                "zeroReason", matchedRows.isEmpty() ? "NO_HS_OR_ROUTE_MATCH" : "",
                "sampleRawKeys", HEADERS,
                "fallbackUsed", false
        ));
        normalized.put("scoreFormula", "KCS tariff/logistics contribution is deterministic and capped; it adds small confidence only when HS and route reference rows exist.");
        normalized.put("decisionImpact", "참고 신호입니다. 법정 관세 확정, 견적 운송비 산정, 통관 가능 여부를 확정하지 않습니다.");
        return normalized;
    }

    private Map<String, Object> csvEvidence(int rowCount) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("collectionMode", "csv_reference");
        evidence.put("fileDataName", FILE_DATA_NAME);
        evidence.put("csvResource", csvResource);
        evidence.put("sourceHsDate", sourceHsDate);
        evidence.put("sourceTariffDate", sourceTariffDate);
        evidence.put("datasetDate", datasetDate);
        evidence.put("localRowCount", rowCount);
        evidence.put("endpointConfigured", true);
        evidence.put("operationEndpoint", false);
        evidence.put("apiKeyConfigured", false);
        evidence.put("apiKeyStored", false);
        evidence.put("fallbackUsed", false);
        evidence.put("reason", "");
        return evidence;
    }

    private Map<String, Object> metrics(PublicDataRequest request, List<Map<String, String>> rows,
                                        List<Map<String, String>> matchedRows) {
        List<Map<String, String>> hsMatches = rows.stream()
                .filter(row -> hsCodeMatches(request.getHsCode(), row.get("hs_code")))
                .toList();
        OptionalDouble mfnTariff = matchedRows.stream()
                .filter(row -> "A".equalsIgnoreCase(row.get("tariff_type")))
                .map(row -> parseDouble(row.get("tariff_rate_pct")))
                .filter(value -> value >= 0)
                .mapToDouble(Double::doubleValue)
                .average();
        OptionalDouble preferentialTariff = matchedRows.stream()
                .filter(row -> !"A".equalsIgnoreCase(row.get("tariff_type")))
                .map(row -> parseDouble(row.get("tariff_rate_pct")))
                .filter(value -> value >= 0)
                .mapToDouble(Double::doubleValue)
                .min();
        OptionalDouble latestFreight = matchedRows.stream()
                .map(row -> parseDouble(row.get("latest_cost_index")))
                .filter(value -> value >= 0)
                .mapToDouble(Double::doubleValue)
                .average();
        OptionalDouble freightYoy = matchedRows.stream()
                .map(row -> parseDouble(row.get("yoy_pct")))
                .filter(value -> value > -9999)
                .mapToDouble(Double::doubleValue)
                .average();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", rows.size());
        metrics.put("hsCodeMatchCount", hsMatches.size());
        metrics.put("routeMatchCount", matchedRows.size());
        metrics.put("mfnTariffRatePct", round(mfnTariff.orElse(0)));
        metrics.put("preferentialTariffRatePct", round(preferentialTariff.orElse(0)));
        metrics.put("latestFreightIndex", round(latestFreight.orElse(0)));
        metrics.put("freightYoyPct", round(freightYoy.orElse(0)));
        metrics.put("matchedRoutes", matchedRows.stream().map(row -> row.get("route")).distinct().sorted().toList());
        metrics.put("matchedModes", matchedRows.stream().map(row -> row.get("mode")).distinct().sorted().toList());
        return metrics;
    }

    private int contribution(Map<String, Object> metrics) {
        int hsMatches = asInt(metrics.get("hsCodeMatchCount"), 0);
        int routeMatches = asInt(metrics.get("routeMatchCount"), 0);
        double mfn = asDouble(metrics.get("mfnTariffRatePct"), 0);
        double yoy = asDouble(metrics.get("freightYoyPct"), 0);
        if (hsMatches == 0) return 0;
        int contribution = 1 + Math.min(2, routeMatches / 20);
        if (mfn <= 8) contribution += 1;
        if (yoy > 20) contribution -= 1;
        if (yoy < -10) contribution += 1;
        return Math.max(-2, Math.min(5, contribution));
    }

    private double confidence(Map<String, Object> metrics, int rowCount) {
        int hsMatches = asInt(metrics.get("hsCodeMatchCount"), 0);
        int routeMatches = asInt(metrics.get("routeMatchCount"), 0);
        return Math.min(0.82,
                0.35 + Math.min(0.12, rowCount / 2000.0) + (hsMatches > 0 ? 0.18 : 0) + (routeMatches > 0 ? 0.12 : 0));
    }

    private List<Map<String, String>> matchedRows(PublicDataRequest request, List<Map<String, String>> rows) {
        List<String> routes = routeNames(request.getTargetCountry());
        return rows.stream()
                .filter(row -> hsCodeMatches(request.getHsCode(), row.get("hs_code")))
                .filter(row -> routes.isEmpty() || routes.contains(safe(row.get("route"))))
                .sorted(Comparator.comparing(row -> safe(row.get("hs_code")) + safe(row.get("mode")) + safe(row.get("route"))))
                .toList();
    }

    private boolean hsCodeMatches(String requested, String candidate) {
        String request = normalizeHsCode(requested);
        String value = normalizeHsCode(candidate);
        return !request.isBlank() && !value.isBlank() && (value.startsWith(request) || request.startsWith(value));
    }

    private List<String> routeNames(String country) {
        String key = routeCountryKey(country);
        return COUNTRY_ROUTES.getOrDefault(key, List.of());
    }

    private String routeCountryKey(String country) {
        String value = safe(country).trim().toUpperCase(Locale.ROOT);
        if (value.contains("GERMANY") || value.contains("EU") || value.contains("유럽")) return "EU";
        if (value.contains("UNITED STATES") || value.equals("USA") || value.equals("US") || value.contains("미국")) return "US";
        if (value.contains("JAPAN") || value.contains("JP") || value.contains("일본")) return "JP";
        if (value.contains("CHINA") || value.contains("CN") || value.contains("중국")) return "CN";
        if (value.contains("VIETNAM") || value.contains("VN") || value.contains("베트남")) return "VN";
        return value;
    }

    private String normalizeHsCode(String value) {
        return safe(value).replaceAll("[^0-9]", "");
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
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
