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

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPublicDataConnector implements PublicDataConnector {

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
            log.warn("Public data connector failed: {}", provider(), e);
            return fallback(request, e.getMessage());
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
                .scoreContribution(scoreContribution(request, false))
                .fallbackUsed(false)
                .build();
    }

    protected PublicDataConnectorResult fallback(PublicDataRequest request, String reason) {
        Map<String, Object> normalized = normalize(request, null, true, reason);
        return PublicDataConnectorResult.builder()
                .provider(provider())
                .endpoint(endpointLabel())
                .requestKey(requestKey(request))
                .hsCode(request.getHsCode())
                .targetCountry(request.getTargetCountry())
                .matchedKey(matchedKey(request))
                .usedFor(usedFor())
                .rawPayload(toJson(Map.of("fallback", true, "reason", reason == null ? "unknown" : reason)))
                .normalizedPayload(normalized)
                .scoreContribution(scoreContribution(request, true))
                .fallbackUsed(true)
                .errorMessage(reason)
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
        return normalized;
    }

    protected String matchedKey(PublicDataRequest request) {
        return "hsCode=" + safe(request.getHsCode()) + ";country=" + safe(request.getTargetCountry());
    }

    protected int scoreContribution(PublicDataRequest request, boolean fallbackUsed) {
        return fallbackUsed ? -2 : 8;
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
