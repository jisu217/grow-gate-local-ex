package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import com.growgate.publicdata.dto.PublicDataSignal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPublicDataConnector implements PublicDataConnector {

    private static final Pattern SECRET_QUERY_PARAM = Pattern.compile(
            "(?i)([?&](?:serviceKey|servicekey|apiKey|apikey|key|token|access_token|client_secret)=)([^&\\s]+)");
    private static final Pattern ISO_DATE = Pattern.compile("\\b20\\d{2}[-./]\\d{1,2}[-./]\\d{1,2}\\b");
    private static final String REDACTED = "[REDACTED]";
    private static final Map<PublicDataProvider, CircuitState> CIRCUITS = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${public-data.http.retry.max-attempts:2}")
    private int retryMaxAttempts = 2;

    @Value("${public-data.circuit.failure-threshold:3}")
    private int circuitFailureThreshold = 3;

    @Value("${public-data.circuit.open-seconds:60}")
    private long circuitOpenSeconds = 60;

    protected PublicDataConnectorResult getJson(PublicDataRequest request, String endpoint, String apiKey,
                                                Map<String, String> params) {
        return getJsonWithKeyParam(request, endpoint, apiKey, "serviceKey", params);
    }

    protected PublicDataConnectorResult getJsonWithKeyParam(PublicDataRequest request, String endpoint, String apiKey,
                                                            String keyParam, Map<String, String> params) {
        if (endpoint == null || endpoint.isBlank()) {
            return fallback(request, endpoint, apiKey, keyParam, "OPERATION_ENDPOINT_NOT_CONFIGURED", null, 0);
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(request, endpoint, apiKey, keyParam, "API_KEY_NOT_CONFIGURED", null, 0);
        }
        if (isCatalogOrApplicationPage(endpoint)) {
            return fallback(request, endpoint, apiKey, keyParam,
                    "CATALOG_PAGE_ENDPOINT", null, 0);
        }
        if (isCircuitOpen()) {
            return fallback(request, endpoint, apiKey, keyParam, "CIRCUIT_OPEN", null, 0);
        }

        long started = System.nanoTime();
        int attempts = Math.max(1, retryMaxAttempts);
        RestClientException lastException = null;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
            params.forEach((key, value) -> {
                if (value != null && !value.isBlank()) builder.queryParam(key, value);
            });
            builder.queryParam(keyParam == null || keyParam.isBlank() ? "serviceKey" : keyParam, apiKey);
            URI uri = builder.build().encode().toUri();
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
                    markSuccess();
                    return success(request, endpoint, apiKey, keyParam, params, response.getBody(),
                            response.getStatusCode().value(), elapsedMs(started));
                } catch (RestClientException e) {
                    lastException = e;
                    if (attempt >= attempts || !isRetryable(e)) break;
                }
            }
            markFailure();
            String reason = failureReason(lastException);
            log.warn("Public data connector failed: provider={}, reason={}", provider(), reason);
            return fallback(request, endpoint, apiKey, keyParam, reason, httpStatus(lastException), elapsedMs(started));
        } catch (IllegalArgumentException e) {
            markFailure();
            String reason = sanitizeErrorMessage(e.getMessage());
            log.warn("Public data connector failed: provider={}, reason={}", provider(), reason);
            return fallback(request, endpoint, apiKey, keyParam, "HTTP_ERROR", null, elapsedMs(started));
        }
    }

    protected PublicDataConnectorResult success(PublicDataRequest request, String endpoint, String apiKey,
                                                Map<String, String> requestParams, String rawPayload,
                                                Integer httpStatus, long elapsedMs) {
        return success(request, endpoint, apiKey, "serviceKey", requestParams, rawPayload, httpStatus, elapsedMs);
    }

    protected PublicDataConnectorResult success(PublicDataRequest request, String endpoint, String apiKey,
                                                String keyParam, Map<String, String> requestParams, String rawPayload,
                                                Integer httpStatus, long elapsedMs) {
        Map<String, Object> normalized = normalize(request, rawPayload, false, null);
        normalized.put("collectionEvidence", collectionEvidence(endpoint, apiKey, false, null));
        normalized.put("requestParamsMasked", maskedParams(requestParams, apiKey, keyParam));
        addZeroDiagnostics(normalized, rawPayload, false, null, httpStatus, elapsedMs);
        String sourceType = sourceTypeFromDiagnostics(normalized, false);
        logPublicDataEvent(normalized, sourceType);
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(endpoint)
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(rawPayload)
                .normalizedPayload(normalized)
                .scoreContribution(scoreContribution(request, false, normalized))
                .confidence(asDouble(normalized.get("confidence"), 0.0))
                .sourceType(sourceType)
                .fallbackUsed(false)
                .build();
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String reason) {
        return fallback(request, endpointLabel(), "", reason, null, 0);
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String endpoint, String apiKey, String reason) {
        return fallback(request, endpoint, apiKey, reason, null, 0);
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String endpoint, String apiKey,
                                                 String reason, Integer httpStatus, long elapsedMs) {
        return fallback(request, endpoint, apiKey, "serviceKey", reason, httpStatus, elapsedMs);
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String endpoint, String apiKey,
                                                 String keyParam, String reason, Integer httpStatus, long elapsedMs) {
        String safeReason = sanitizeErrorMessage(reason);
        Map<String, Object> normalized = normalize(request, null, true, safeReason);
        normalized.put("collectionEvidence", collectionEvidence(endpoint, apiKey, true, safeReason));
        normalized.put("requestParamsMasked", maskedParams(Map.of(), apiKey, keyParam));
        addZeroDiagnostics(normalized, null, true, safeReason, httpStatus, elapsedMs);
        String sourceType = sourceTypeFromDiagnostics(normalized, true);
        boolean fallbackUsed = "fallback".equals(sourceType);
        normalized.put("fallbackUsed", fallbackUsed);
        Object evidenceValue = normalized.get("collectionEvidence");
        if (evidenceValue instanceof Map<?, ?> evidence) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mutable = (Map<String, Object>) evidence;
            mutable.put("fallbackUsed", fallbackUsed);
        }
        Object diagnosticsValue = normalized.get("zeroResultDiagnostics");
        if (diagnosticsValue instanceof Map<?, ?> diagnostics) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mutable = (Map<String, Object>) diagnostics;
            mutable.put("sourceType", sourceType);
            mutable.put("fallbackUsed", fallbackUsed);
        }
        logPublicDataEvent(normalized, sourceType);
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(endpoint)
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(Map.of("fallback", fallbackUsed, "sourceType", sourceType, "reason", safeReason)))
                .normalizedPayload(normalized)
                .scoreContribution(scoreContribution(request, fallbackUsed, normalized))
                .confidence(asDouble(normalized.get("confidence"), 0.0))
                .sourceType(sourceType)
                .fallbackUsed(fallbackUsed)
                .errorMessage(safeReason)
                .build();
    }

    protected Map<String, Object> normalize(PublicDataRequest request, String rawPayload,
                                            boolean fallbackUsed, String reason) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("provider", provider().name());
        normalized.put("usedFor", usedFor());
        normalized.put("hsCode", request.getHsCode());
        normalized.put("targetCountry", request.getTargetCountry());
        normalized.put("productName", request.getProductName());
        normalized.put("matchedKey", matchedKey(request));
        normalized.put("fallbackUsed", fallbackUsed);
        normalized.put("reason", reason);
        normalized.put("rawLength", rawPayload == null ? 0 : rawPayload.length());
        normalized.put("signal", fallbackUsed ? "fallback" : "live");
        normalized.put("agency", provider().agency());
        normalized.put("ministry", provider().ministry());
        normalized.put("dataFamily", provider().dataFamily());
        normalized.put("motieRelated", provider().motieRelated());
        normalized.put("refreshCycleHours", provider().refreshCycleHours());
        normalized.put("sourceName", provider().sourceName());
        normalized.put("officialUrl", provider().officialUrl());
        normalized.put("dataUsagePolicy", dataUsagePolicy());
        normalized.put("domainSignals", extractDomainSignals(request, rawPayload, fallbackUsed));
        PublicDataSignal signal = extractSignal(request, rawPayload, fallbackUsed);
        normalized.put("metrics", signal.getMetrics());
        normalized.put("metricSchema", signal.getMetrics().keySet());
        normalized.put("fieldMapping", fieldMapping());
        normalized.put("scoreContribution", signal.getScoreContribution());
        normalized.put("confidence", signal.getConfidence());
        addSourcePeriod(normalized, rawPayload);
        normalized.put("scoreFormula", "scoreContribution is calculated from provider-specific metrics; diagnosis uses confidence-adjusted contribution");
        return normalized;
    }

    protected Map<String, Object> dataUsagePolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("provider", provider().agency());
        policy.put("sourceName", provider().sourceName());
        policy.put("officialUrl", provider().officialUrl());
        policy.put("operationEndpoint", provider().operationEndpoint());
        policy.put("apiKeyRequired", provider().apiKeyRequired());
        policy.put("licenseName", provider().licenseName());
        policy.put("termsUrl", provider().termsUrl());
        policy.put("redistributionAllowed", provider().redistributionAllowed());
        policy.put("storageAllowed", provider().storageAllowed());
        policy.put("reprocessingAllowed", provider().reprocessingAllowed());
        policy.put("attributionRequired", provider().attributionRequired());
        policy.put("usageWarning", provider().usageWarning());
        policy.put("lastVerifiedAt", provider().lastVerifiedAt());
        return policy;
    }

    @SuppressWarnings("unchecked")
    private void addZeroDiagnostics(Map<String, Object> normalized, String rawPayload,
                                    boolean fallbackUsed, String reason, Integer httpStatus, long elapsedMs) {
        int rawCount = fallbackUsed ? 0 : estimateRecordCount(rawPayload);
        int normalizedCount = 0;
        Object metricsValue = normalized.get("metrics");
        if (metricsValue instanceof Map<?, ?> metrics) {
            Object recordCount = metrics.get("recordCount");
            if (recordCount instanceof Number number) {
                normalizedCount = Math.max(0, number.intValue());
            }
        }
        int filteredCount = normalizedCount;
        String zeroReason = zeroReason(rawCount, normalizedCount, fallbackUsed, reason);

        normalized.put("rawCount", rawCount);
        normalized.put("normalizedCount", normalizedCount);
        normalized.put("filteredCount", filteredCount);
        normalized.put("zeroReason", zeroReason);
        normalized.put("sampleRawKeys", sampleRawKeys(rawPayload));
        normalized.put("httpStatus", httpStatus);
        normalized.put("elapsedMs", elapsedMs);

        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("provider", provider().name());
        diagnostics.put("endpointType", endpointType(normalized));
        diagnostics.put("usedFor", usedFor());
        diagnostics.put("sourceType", fallbackUsed ? "fallback" : normalizedCount == 0 ? "empty" : "live");
        diagnostics.put("httpStatus", httpStatus);
        diagnostics.put("elapsedMs", elapsedMs);
        diagnostics.put("rawCount", rawCount);
        diagnostics.put("normalizedCount", normalizedCount);
        diagnostics.put("filteredCount", filteredCount);
        diagnostics.put("zeroReason", zeroReason);
        diagnostics.put("sampleRawKeys", normalized.get("sampleRawKeys"));
        diagnostics.put("fallbackUsed", fallbackUsed);
        normalized.put("zeroResultDiagnostics", diagnostics);
    }

    private String zeroReason(int rawCount, int normalizedCount, boolean fallbackUsed, String reason) {
        if (fallbackUsed) {
            String safeReason = safe(reason).toLowerCase(Locale.ROOT);
            if (safeReason.contains("public_data_enabled=false")) return "PUBLIC_DATA_DISABLED";
            if (safeReason.contains("operation_endpoint_not_configured")
                    || safeReason.contains("api endpoint is not configured")) return "OPERATION_ENDPOINT_NOT_CONFIGURED";
            if (safeReason.contains("api_key_not_configured")
                    || safeReason.contains("api key is not configured")) return "API_KEY_NOT_CONFIGURED";
            if (safeReason.contains("catalog_page_endpoint")
                    || safeReason.contains("catalog/application url")) return "CATALOG_PAGE_ENDPOINT";
            if (safeReason.contains("timed out") || safeReason.contains("timeout")) return "TIMEOUT";
            if (safeReason.contains("429")) return "RATE_LIMITED";
            if (safeReason.contains("circuit_open")) return "CIRCUIT_OPEN";
            if (safeReason.contains("parser")) return "PARSER_ERROR";
            return "HTTP_ERROR";
        }
        if (normalizedCount > 0) return "";
        if (rawCount > 0) return "NORMALIZER_SCHEMA_MISMATCH";
        return "EMPTY_LIVE_RESPONSE";
    }

    private Map<String, String> maskedParams(Map<String, String> params, String apiKey) {
        return maskedParams(params, apiKey, "serviceKey");
    }

    private Map<String, String> maskedParams(Map<String, String> params, String apiKey, String keyParam) {
        Map<String, String> masked = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((key, value) -> masked.put(key, value == null ? "" : value));
        }
        masked.put(keyParam == null || keyParam.isBlank() ? "serviceKey" : keyParam,
                apiKey == null || apiKey.isBlank() ? "" : REDACTED);
        return masked;
    }

    private String sourceTypeFromDiagnostics(Map<String, Object> normalized, boolean fallbackPath) {
        Object zeroReasonValue = normalized.get("zeroReason");
        String zeroReason = zeroReasonValue == null ? "" : zeroReasonValue.toString();
        if (!fallbackPath) {
            return zeroReason.isBlank() ? "live" : "empty";
        }
        if (zeroReason.equals("API_KEY_NOT_CONFIGURED")
                || zeroReason.equals("OPERATION_ENDPOINT_NOT_CONFIGURED")
                || zeroReason.equals("CATALOG_PAGE_ENDPOINT")
                || zeroReason.equals("PUBLIC_DATA_DISABLED")) {
            return "empty";
        }
        return "fallback";
    }

    private String endpointType(Map<String, Object> normalized) {
        Object value = normalized.get("collectionEvidence");
        if (!(value instanceof Map<?, ?> evidence)) return "";
        Object endpointType = evidence.get("endpointType");
        return endpointType == null ? "" : endpointType.toString();
    }

    private void logPublicDataEvent(Map<String, Object> normalized, String sourceType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", "public_data_provider_call");
        event.put("correlationId", MDC.get("correlationId"));
        event.put("provider", provider().name());
        event.put("endpointType", endpointType(normalized));
        event.put("sourceType", sourceType);
        event.put("httpStatus", normalized.get("httpStatus"));
        event.put("elapsedMs", normalized.get("elapsedMs"));
        event.put("rawCount", normalized.get("rawCount"));
        event.put("normalizedCount", normalized.get("normalizedCount"));
        event.put("fallbackUsed", normalized.get("fallbackUsed"));
        event.put("zeroReason", normalized.get("zeroReason"));
        log.info("{}", toJson(event));
    }

    private boolean isRetryable(RestClientException e) {
        if (e instanceof ResourceAccessException) return true;
        Integer status = httpStatus(e);
        return status != null && (status == 408 || status == 429 || status >= 500);
    }

    private Integer httpStatus(RestClientException e) {
        if (e instanceof HttpStatusCodeException statusException) {
            return statusException.getStatusCode().value();
        }
        return null;
    }

    private String failureReason(RestClientException e) {
        if (e == null) return "HTTP_ERROR";
        Integer status = httpStatus(e);
        if (status != null) {
            if (status == 429) return "RATE_LIMITED";
            return "HTTP_ERROR status=" + status;
        }
        if (e instanceof ResourceAccessException) return "TIMEOUT";
        return sanitizeErrorMessage(e.getMessage());
    }

    private long elapsedMs(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private boolean isCircuitOpen() {
        CircuitState state = CIRCUITS.get(provider());
        if (state == null || state.openedAt == null) return false;
        long openUntil = state.openedAt.plusSeconds(Math.max(1, circuitOpenSeconds)).toEpochMilli();
        if (Instant.now().toEpochMilli() < openUntil) return true;
        CIRCUITS.remove(provider());
        return false;
    }

    private void markSuccess() {
        CIRCUITS.remove(provider());
    }

    private void markFailure() {
        CIRCUITS.compute(provider(), (provider, previous) -> {
            int failures = previous == null ? 1 : previous.failures + 1;
            Instant openedAt = failures >= Math.max(1, circuitFailureThreshold) ? Instant.now() : null;
            return new CircuitState(failures, openedAt);
        });
    }

    private List<String> sampleRawKeys(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return List.of();
        JsonNode root = readTree(rawPayload);
        List<String> keys = new ArrayList<>();
        if (root.isObject()) {
            Iterator<String> names = root.fieldNames();
            while (names.hasNext() && keys.size() < 12) keys.add(names.next());
        }
        if (!keys.isEmpty()) return keys;
        java.util.regex.Matcher matcher = Pattern.compile("<([A-Za-z][A-Za-z0-9_:-]*)\\b").matcher(rawPayload);
        while (matcher.find() && keys.size() < 12) {
            String key = matcher.group(1);
            if (!keys.contains(key)) keys.add(key);
        }
        return keys;
    }

    private static class CircuitState {
        private final int failures;
        private final Instant openedAt;

        private CircuitState(int failures, Instant openedAt) {
            this.failures = failures;
            this.openedAt = openedAt;
        }
    }

    protected Map<String, String> fieldMapping() {
        return Map.of("recordCount", "provider response item/row/object count");
    }

    protected Map<String, Object> collectionEvidence(String endpoint, String apiKey, boolean fallbackUsed, String reason) {
        String safeEndpoint = endpoint == null ? "" : endpoint;
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("endpointConfigured", !safeEndpoint.isBlank() && !"disabled".equalsIgnoreCase(safeEndpoint));
        evidence.put("operationEndpoint", !safeEndpoint.isBlank() && !isCatalogOrApplicationPage(safeEndpoint)
                && !"disabled".equalsIgnoreCase(safeEndpoint));
        evidence.put("endpointType", safeEndpoint.isBlank() ? "missing"
                : "disabled".equalsIgnoreCase(safeEndpoint) ? "disabled"
                : isCatalogOrApplicationPage(safeEndpoint) ? "catalog_or_application_page" : "operation");
        evidence.put("apiKeyConfigured", apiKey != null && !apiKey.isBlank());
        evidence.put("apiKeyStored", false);
        evidence.put("fallbackUsed", fallbackUsed);
        evidence.put("reason", reason == null || reason.isBlank() ? "" : sanitizeErrorMessage(reason));
        return evidence;
    }

    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", fallbackUsed ? 0 : estimateRecordCount(rawPayload));
        return signal(request, metrics, fallbackUsed ? scoreContribution(request, true) : scoreContribution(request, false),
                fallbackUsed ? 0.0 : Math.min(0.75, 0.35 + asInt(metrics.get("recordCount"), 0) * 0.05));
    }

    protected Map<String, Object> extractDomainSignals(PublicDataRequest request, String rawPayload,
                                                       boolean fallbackUsed) {
        String body = rawPayload == null ? "" : rawPayload;
        String lower = body.toLowerCase(Locale.ROOT);
        String hsCode = safe(request.getHsCode());
        String country = safe(request.getTargetCountry());
        String productName = safe(request.getProductName());
        int keywordHits = 0;
        if (!hsCode.isBlank() && body.contains(hsCode)) keywordHits++;
        if (!country.isBlank() && lower.contains(country.toLowerCase(Locale.ROOT))) keywordHits++;
        if (!productName.isBlank() && lower.contains(productName.toLowerCase(Locale.ROOT))) keywordHits++;

        int recordCountEstimate = estimateRecordCount(body);
        int numericTokenCount = countMatches(body, "\\b\\d+(?:\\.\\d+)?\\b", 50);
        int evidenceStrength = fallbackUsed ? 0 : Math.min(5,
                (body.isBlank() ? 0 : 1)
                        + Math.min(2, keywordHits)
                        + (recordCountEstimate > 0 ? 1 : 0)
                        + (numericTokenCount >= 3 ? 1 : 0));

        Map<String, Object> signals = new LinkedHashMap<>();
        signals.put("containsHsCode", !hsCode.isBlank() && body.contains(hsCode));
        signals.put("containsTargetCountry", !country.isBlank()
                && lower.contains(country.toLowerCase(Locale.ROOT)));
        signals.put("containsProductName", !productName.isBlank()
                && lower.contains(productName.toLowerCase(Locale.ROOT)));
        signals.put("keywordHits", keywordHits);
        signals.put("numericTokenCount", numericTokenCount);
        signals.put("recordCountEstimate", recordCountEstimate);
        signals.put("evidenceStrength", evidenceStrength);
        return signals;
    }

    protected String matchedKey(PublicDataRequest request) {
        return "hsCode=" + safe(request.getHsCode()) + ";country=" + safe(request.getTargetCountry());
    }

    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -2 : 8;
    }

    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed,
                                    Map<String, Object> normalizedPayload) {
        Object parsedContribution = normalizedPayload.get("scoreContribution");
        if (parsedContribution instanceof Number number) {
            return Math.max(-10, Math.min(15, number.intValue()));
        }
        int base = scoreContribution(request, fallbackUsed);
        if (fallbackUsed) return base;
        Object domainSignals = normalizedPayload.get("domainSignals");
        if (!(domainSignals instanceof Map<?, ?> signals)) return base;
        Object strengthValue = signals.get("evidenceStrength");
        int strength = strengthValue instanceof Number number ? number.intValue() : 0;
        return Math.max(-10, Math.min(15, base + Math.min(3, Math.max(0, strength - 2))));
    }

    protected PublicDataSignal signal(PublicDataRequest request, Map<String, Object> metrics,
                                      int scoreContribution, double confidence) {
        return PublicDataSignal.builder()
                .provider(provider().name())
                .usedFor(usedFor())
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .metrics(metrics)
                .scoreContribution(Math.max(-10, Math.min(15, scoreContribution)))
                .confidence(Math.max(0.0, Math.min(1.0, confidence)))
                .build();
    }

    protected JsonNode readTree(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return objectMapper.createObjectNode();
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    protected double sumNumericFields(JsonNode root, String... fieldNames) {
        Map<String, Boolean> targets = new LinkedHashMap<>();
        for (String fieldName : fieldNames) targets.put(canonical(fieldName), true);
        return sumNumericFields(root, targets);
    }

    protected int maxArraySize(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) return 0;
        int max = root.isArray() ? root.size() : 0;
        if (root.isContainerNode()) {
            for (JsonNode child : root) max = Math.max(max, maxArraySize(child));
        }
        return max;
    }

    protected int objectCount(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) return 0;
        int count = root.isObject() ? 1 : 0;
        if (root.isContainerNode()) {
            for (JsonNode child : root) count += objectCount(child);
        }
        return count;
    }

    protected int textHitCount(JsonNode root, String regex) {
        if (regex == null || regex.isBlank()) return 0;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return textHitCount(root, pattern);
    }

    protected OptionalDouble firstNumericField(JsonNode root, String... fieldNames) {
        Map<String, Boolean> targets = new LinkedHashMap<>();
        for (String fieldName : fieldNames) targets.put(canonical(fieldName), true);
        return firstNumericField(root, targets);
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }

    protected String sanitizeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown";
        }
        return SECRET_QUERY_PARAM.matcher(message).replaceAll("$1" + REDACTED);
    }

    protected int estimateRecordCount(String body) {
        if (body == null || body.isBlank()) return 0;
        int jsonObjectHints = countMatches(body, "\\{", 200);
        int xmlItemHints = countMatches(body, "<(?:item|row|record)\\b", 200);
        if (xmlItemHints > 0) return xmlItemHints;
        return Math.max(0, jsonObjectHints - 1);
    }

    private void addSourcePeriod(Map<String, Object> normalized, String rawPayload) {
        List<String> dates = extractDateTokens(rawPayload);
        if (dates.isEmpty()) return;
        dates.sort(Comparator.naturalOrder());
        String start = dates.get(0);
        String end = dates.get(dates.size() - 1);
        normalized.put("sourcePeriodStart", start);
        normalized.put("sourcePeriodEnd", end);
        normalized.put("sourcePeriod", start.equals(end) ? start : start + " ~ " + end);
        normalized.put("lastPublishedAt", end);
    }

    private List<String> extractDateTokens(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) return List.of();
        java.util.regex.Matcher matcher = ISO_DATE.matcher(rawPayload);
        List<String> dates = new ArrayList<>();
        while (matcher.find() && dates.size() < 100) {
            dates.add(matcher.group().replace('.', '-').replace('/', '-'));
        }
        return dates;
    }

    private int countMatches(String body, String regex, int limit) {
        if (body == null || body.isBlank()) return 0;
        java.util.regex.Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(body);
        int count = 0;
        while (matcher.find() && count < limit) count++;
        return count;
    }

    protected int asInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    protected double asDouble(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private double sumNumericFields(JsonNode node, Map<String, Boolean> targets) {
        if (node == null || node.isMissingNode() || node.isNull()) return 0;
        double sum = 0;
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                if (targets.containsKey(canonical(field.getKey())) && value.isNumber()) {
                    sum += value.asDouble();
                }
                sum += sumNumericFields(value, targets);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) sum += sumNumericFields(child, targets);
        }
        return sum;
    }

    private OptionalDouble firstNumericField(JsonNode node, Map<String, Boolean> targets) {
        if (node == null || node.isMissingNode() || node.isNull()) return OptionalDouble.empty();
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                if (targets.containsKey(canonical(field.getKey())) && value.isNumber()) {
                    return OptionalDouble.of(value.asDouble());
                }
                OptionalDouble nested = firstNumericField(value, targets);
                if (nested.isPresent()) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                OptionalDouble nested = firstNumericField(child, targets);
                if (nested.isPresent()) return nested;
            }
        }
        return OptionalDouble.empty();
    }

    private int textHitCount(JsonNode node, Pattern pattern) {
        if (node == null || node.isMissingNode() || node.isNull()) return 0;
        if (node.isTextual()) return pattern.matcher(node.asText()).find() ? 1 : 0;
        int count = 0;
        if (node.isContainerNode()) {
            for (JsonNode child : node) count += textHitCount(child, pattern);
        }
        return count;
    }

    private String canonical(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
    }

    private boolean isCatalogOrApplicationPage(String endpoint) {
        String normalized = endpoint.toLowerCase();
        return normalized.contains("data.go.kr") && normalized.endsWith("/openapi.do")
                || normalized.contains("plus.kipris.or.kr") && normalized.contains("/contents.do")
                || normalized.equals("https://www.exportvoucher.com/")
                || normalized.equals("https://www.safetykorea.kr/release/openapi");
    }

    protected abstract String endpointLabel();

    public abstract PublicDataProvider provider();
}
