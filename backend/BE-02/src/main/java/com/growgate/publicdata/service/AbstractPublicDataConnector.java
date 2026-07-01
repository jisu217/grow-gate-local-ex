package com.growgate.publicdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.enums.PublicDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPublicDataConnector implements PublicDataConnector {

    private static final Pattern SECRET_QUERY_PARAM = Pattern.compile(
            "(?i)([?&](?:serviceKey|servicekey|apiKey|apikey|key|token|access_token|client_secret)=)([^&\\s]+)");
    private static final String REDACTED = "[REDACTED]";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    protected PublicDataConnectorResult getJson(PublicDataRequest request, String endpoint, String apiKey,
                                                Map<String, String> params) {
        if (endpoint == null || endpoint.isBlank()) {
            return fallback(request, "API endpoint is not configured");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(request, provider().name() + " API key is not configured");
        }
        if (isCatalogOrApplicationPage(endpoint)) {
            return fallback(request, "Official catalog/application URL is configured; set the provider operation endpoint after API approval");
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
            params.forEach((key, value) -> {
                if (value != null && !value.isBlank()) builder.queryParam(key, value);
            });
            builder.queryParam("serviceKey", apiKey);
            URI uri = builder.build(true).toUri();
            String body = restTemplate.getForObject(uri, String.class);
            return success(request, endpoint, body);
        } catch (IllegalArgumentException | RestClientException e) {
            String reason = sanitizeErrorMessage(e.getMessage());
            log.warn("Public data connector failed: provider={}, reason={}", provider(), reason);
            return fallback(request, reason);
        }
    }

    protected PublicDataConnectorResult success(PublicDataRequest request, String endpoint, String rawPayload) {
        Map<String, Object> normalized = normalize(request, rawPayload, false, null);
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
                .fallbackUsed(false)
                .build();
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String reason) {
        String safeReason = sanitizeErrorMessage(reason);
        Map<String, Object> normalized = normalize(request, null, true, safeReason);
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(endpointLabel())
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(Map.of("fallback", true, "reason", safeReason)))
                .normalizedPayload(normalized)
                .scoreContribution(scoreContribution(request, true, normalized))
                .fallbackUsed(true)
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
        normalized.put("domainSignals", extractDomainSignals(request, rawPayload, fallbackUsed));
        return normalized;
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
        int base = scoreContribution(request, fallbackUsed);
        if (fallbackUsed) return base;
        Object domainSignals = normalizedPayload.get("domainSignals");
        if (!(domainSignals instanceof Map<?, ?> signals)) return base;
        Object strengthValue = signals.get("evidenceStrength");
        int strength = strengthValue instanceof Number number ? number.intValue() : 0;
        return Math.max(-10, Math.min(15, base + Math.min(3, Math.max(0, strength - 2))));
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

    private int estimateRecordCount(String body) {
        if (body == null || body.isBlank()) return 0;
        int jsonObjectHints = countMatches(body, "\\{", 200);
        int xmlItemHints = countMatches(body, "<(?:item|row|record)\\b", 200);
        if (xmlItemHints > 0) return xmlItemHints;
        return Math.max(0, jsonObjectHints - 1);
    }

    private int countMatches(String body, String regex, int limit) {
        if (body == null || body.isBlank()) return 0;
        java.util.regex.Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(body);
        int count = 0;
        while (matcher.find() && count < limit) count++;
        return count;
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
