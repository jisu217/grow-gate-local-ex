package com.growgate.publicdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.growgate.publicdata.dto.PublicDataSignal;
import com.growgate.publicdata.enums.PublicDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class BokKeyStatisticsConnector extends AbstractPublicDataConnector {

    private final RestTemplate directRestTemplate;

    @Value("${public-data.bok-key-statistics.url:https://ecos.bok.or.kr/api/KeyStatisticList}")
    private String endpoint;

    @Value("${public-data.bok-key-statistics.key:${BOK_ECOS_API_KEY:}}")
    private String apiKey;

    @Value("${public-data.bok-key-statistics.language:kr}")
    private String language;

    @Value("${public-data.bok-key-statistics.max-rows:100}")
    private int maxRows;

    public BokKeyStatisticsConnector(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(restTemplate, objectMapper);
        this.directRestTemplate = restTemplate;
    }

    @Override
    public PublicDataProvider provider() {
        return PublicDataProvider.BOK_KEY_STATISTICS;
    }

    @Override
    public String usedFor() {
        return "market";
    }

    @Override
    public String requestKey(PublicDataRequest request) {
        return provider() + ":KEY_STATISTIC_LIST:" + safe(request.getTargetCountry()).toUpperCase(Locale.ROOT);
    }

    @Override
    public PublicDataConnectorResult collect(PublicDataRequest request) {
        if (endpoint == null || endpoint.isBlank()) {
            return fallback(request, endpoint, apiKey, "OPERATION_ENDPOINT_NOT_CONFIGURED");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallback(request, endpoint, apiKey, "API_KEY_NOT_CONFIGURED");
        }

        long started = System.nanoTime();
        try {
            String url = endpoint.replaceAll("/+$", "") + "/" + apiKey + "/json/" + language + "/1/" + Math.max(1, maxRows);
            ResponseEntity<String> response = directRestTemplate.getForEntity(url, String.class);
            return success(request, endpoint, apiKey, "apiKeyInPath", Map.of(
                    "format", "json",
                    "language", language,
                    "start", "1",
                    "end", String.valueOf(Math.max(1, maxRows))
            ), response.getBody(), response.getStatusCode().value(), elapsedMs(started));
        } catch (Exception e) {
            return fallback(request, endpoint, apiKey, "apiKeyInPath",
                    "HTTP_ERROR", null, elapsedMs(started));
        }
    }

    @Override
    protected Map<String, String> fieldMapping() {
        return Map.of(
                "recordCount", "KeyStatisticList.row count",
                "exportIndicatorHits", "rows mentioning export/import/trade",
                "fxIndicatorHits", "rows mentioning exchange rate/current account",
                "productionIndicatorHits", "rows mentioning production/manufacturing",
                "latestStatisticName", "first statistic name in ECOS response"
        );
    }

    @Override
    protected PublicDataSignal extractSignal(PublicDataRequest request, String rawPayload, boolean fallbackUsed) {
        JsonNode root = readTree(rawPayload);
        JsonNode rows = root.path("KeyStatisticList").path("row");
        int rowCount = rows.isArray() ? rows.size() : maxArraySize(root);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("recordCount", fallbackUsed ? 0 : rowCount);
        metrics.put("exportIndicatorHits", textHitCount(root, "수출|수입|무역|경상수지"));
        metrics.put("fxIndicatorHits", textHitCount(root, "환율|외환|달러|원/달러"));
        metrics.put("productionIndicatorHits", textHitCount(root, "생산|제조|산업"));
        metrics.put("latestStatisticName", firstText(rows, "CLASS_NAME", "KEYSTAT_NAME", "STAT_NAME"));
        metrics.put("collectedAt", Instant.now().toString());
        int contribution = fallbackUsed ? -1 : rowCount > 0 ? 1 : 0;
        double confidence = fallbackUsed ? 0.0 : rowCount > 0 ? 0.68 : 0.35;
        return signal(request, metrics, contribution, confidence);
    }

    @Override
    protected String endpointLabel() {
        return endpoint;
    }

    private long elapsedMs(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private String firstText(JsonNode rows, String... names) {
        if (rows == null || !rows.isArray() || rows.isEmpty()) return "";
        JsonNode first = rows.get(0);
        for (String name : names) {
            String value = first.path(name).asText("");
            if (!value.isBlank()) return value;
        }
        return "";
    }
}
